package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.SubscriptionStatusDTO;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private final UserRepository userRepository;
    private final Optional<RedisCacheService> redisCacheService;
    private final Optional<TelegramBotService> telegramBotService;

    // Premium lesson patterns
    private static final List<String> PREMIUM_PATTERNS = Arrays.asList(
            "Средний уровень (Подписка)",
            "Продвинутый уровень (Подписка)",
            "Эксперт уровень (Подписка)",
            "🎓"
    );

    // Subscription cache duration in minutes
    private static final long SUBSCRIPTION_CACHE_DURATION = 5;

    /**
     * Check if a lesson requires subscription
     */
    public boolean isPremiumLesson(String lessonPath) {
        if (lessonPath == null || lessonPath.isEmpty()) {
            return false;
        }

        return PREMIUM_PATTERNS.stream()
                .anyMatch(pattern -> lessonPath.contains(pattern));
    }

    /**
     * Check user subscription status
     */
    public SubscriptionStatusDTO getSubscriptionStatus(Long telegramId) {
        if (telegramId == null) {
            return SubscriptionStatusDTO.builder()
                    .subscribed(false)
                    .verified(false)
                    .build();
        }

        // Check cache first
        Optional<SubscriptionStatusDTO> cached = redisCacheService
                .flatMap(cache -> Optional.ofNullable(cache.getCachedSubscriptionStatus(telegramId)))
                .map(SubscriptionStatusDTO.class::cast);

        if (cached.isPresent()) {
            SubscriptionStatusDTO status = cached.get();
            // Check if cache is still valid
            if (status.verifiedAt() != null) {
                long minutesSinceVerification = ChronoUnit.MINUTES.between(
                        status.verifiedAt(), LocalDateTime.now()
                );
                if (minutesSinceVerification < SUBSCRIPTION_CACHE_DURATION) {
                    return status;
                }
            }
        }

        // Verify with Telegram bot service
        boolean isSubscribed = verifySubscriptionWithTelegram(telegramId);

        SubscriptionStatusDTO status = SubscriptionStatusDTO.builder()
                .telegramId(telegramId)
                .subscribed(isSubscribed)
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(SUBSCRIPTION_CACHE_DURATION))
                .build();

        // Update user record
        updateUserSubscriptionStatus(telegramId, isSubscribed);

        // Cache the result
        redisCacheService.ifPresent(cache ->
            cache.cacheSubscriptionStatus(telegramId, status, SUBSCRIPTION_CACHE_DURATION)
        );

        return status;
    }

    /**
     * Verify subscription through Telegram bot
     */
    private boolean verifySubscriptionWithTelegram(Long telegramId) {
        if (telegramBotService.isEmpty()) {
            log.warn("Telegram bot service not available, defaulting to false");
            return false;
        }

        try {
            return telegramBotService.get().checkChannelMembership(telegramId);
        } catch (Exception e) {
            log.error("Error checking Telegram subscription for user {}: {}", telegramId, e.getMessage());
            // Check if user has active subscription in database as fallback
            return checkDatabaseSubscription(telegramId);
        }
    }

    /**
     * Check subscription status from database
     */
    private boolean checkDatabaseSubscription(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            return false;
        }

        // Check if user has premium access flag
        if (Boolean.TRUE.equals(user.getPremiumAccess())) {
            return true;
        }

        // Check if user has active subscription period
        if (user.getSubscriptionExpiresAt() != null) {
            return user.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());
        }

        return false;
    }

    /**
     * Update user subscription status in database
     */
    private void updateUserSubscriptionStatus(Long telegramId, boolean isSubscribed) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createNewUser(telegramId));

        user.setSubscribed(isSubscribed);
        user.setSubscriptionVerifiedAt(LocalDateTime.now());

        if (isSubscribed && user.getSubscriptionStartedAt() == null) {
            user.setSubscriptionStartedAt(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    /**
     * Grant premium access to user
     */
    public void grantPremiumAccess(Long telegramId, int durationDays) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createNewUser(telegramId));

        user.setPremiumAccess(true);
        user.setSubscribed(true);
        user.setSubscriptionStartedAt(LocalDateTime.now());

        if (durationDays > 0) {
            user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(durationDays));
        }

        userRepository.save(user);

        // Invalidate cache
        redisCacheService.ifPresent(cache -> cache.invalidateSubscriptionStatus(telegramId));

        log.info("Granted premium access to user {} for {} days", telegramId, durationDays);
    }

    /**
     * Revoke premium access from user
     */
    public void revokePremiumAccess(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            return;
        }

        user.setPremiumAccess(false);
        user.setSubscribed(false);
        user.setSubscriptionExpiresAt(LocalDateTime.now());

        userRepository.save(user);

        // Invalidate cache
        redisCacheService.ifPresent(cache -> cache.invalidateSubscriptionStatus(telegramId));

        log.info("Revoked premium access from user {}", telegramId);
    }

    /**
     * Check if user has access to a specific lesson
     */
    public boolean hasAccessToLesson(Long telegramId, String lessonPath) {
        // Free lessons are always accessible
        if (!isPremiumLesson(lessonPath)) {
            return true;
        }

        // No user ID means no access to premium
        if (telegramId == null) {
            return false;
        }

        // Check subscription status
        SubscriptionStatusDTO status = getSubscriptionStatus(telegramId);
        return status.subscribed();
    }

    /**
     * Handle subscription verification callback
     */
    public void handleSubscriptionVerification(Long telegramId, boolean verified) {
        if (!verified) {
            log.info("Subscription verification failed for user {}", telegramId);
            return;
        }

        log.info("Subscription verified for user {}", telegramId);

        // Update status
        updateUserSubscriptionStatus(telegramId, true);

        // Cache the verification
        SubscriptionStatusDTO status = SubscriptionStatusDTO.builder()
                .telegramId(telegramId)
                .subscribed(true)
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(SUBSCRIPTION_CACHE_DURATION))
                .build();

        redisCacheService.ifPresent(cache ->
            cache.cacheSubscriptionStatus(telegramId, status, SUBSCRIPTION_CACHE_DURATION)
        );
    }

    /**
     * Create subscription response for API endpoints
     */
    public Map<String, Object> createSubscriptionResponse(Long userId, boolean isSubscribed) {
        return Map.<String, Object>of(
                "hasSubscription", isSubscribed,
                "isPremium", isAdmin(userId),
                "subscriptionType", isSubscribed ? "premium" : "free",
                "validUntil", System.currentTimeMillis() + 86400000L,
                "channelId", "@DailyTradiBlog"
        );
    }

    /**
     * Create error response for subscription failures
     */
    public Map<String, Object> createSubscriptionErrorResponse() {
        return Map.<String, Object>of(
                "hasSubscription", false,
                "isPremium", false,
                "subscriptionType", "free",
                "error", "Could not verify subscription"
        );
    }

    /**
     * Verify subscription via initData (placeholder implementation)
     */
    public Map<String, Object> verifySubscriptionViaInitData(String initData) {
        // TODO: Implement proper Telegram subscription verification
        return Map.<String, Object>of(
                "valid", true,
                "hasAccess", true,
                "subscriptionLevel", "premium",
                "message", "Subscription verified successfully"
        );
    }

    /**
     * Check if user is admin (temporary implementation until proper service separation)
     */
    private boolean isAdmin(Long userId) {
        // This should ideally be in TelegramAuthService, but to avoid circular dependency for now
        Set<Long> adminIds = Set.of(781182099L, 5974666109L);
        return adminIds.contains(userId);
    }

    private User createNewUser(Long telegramId) {
        User newUser = new User();
        newUser.setTelegramId(telegramId);
        newUser.setCreatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }
}