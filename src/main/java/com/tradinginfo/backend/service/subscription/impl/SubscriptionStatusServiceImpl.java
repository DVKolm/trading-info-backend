package com.tradinginfo.backend.service.subscription.impl;

import com.tradinginfo.backend.dto.SubscriptionStatusDTO;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.repository.UserRepository;
import com.tradinginfo.backend.service.telegram.TelegramUserAuthService;
import com.tradinginfo.backend.service.subscription.SubscriptionStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionStatusServiceImpl implements SubscriptionStatusService {

    private final UserRepository userRepository;
    private final TelegramUserAuthService telegramAuthService;

    @Value("${app.subscription.cache-duration-minutes}")
    private long subscriptionCacheDuration;

    @Override
    public SubscriptionStatusDTO getSubscriptionStatus(Long telegramId) {
        if (telegramId == null) {
            return SubscriptionStatusDTO.builder()
                    .telegramId(null)
                    .subscribed(false)
                    .verified(false)
                    .message("User ID is required")
                    .build();
        }

        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            return SubscriptionStatusDTO.builder()
                    .telegramId(telegramId)
                    .subscribed(false)
                    .verified(false)
                    .message("User not found")
                    .build();
        }

        User user = userOpt.get();
        boolean isSubscribed = Boolean.TRUE.equals(user.getSubscribed());
        boolean hasActiveSubscription = user.getSubscriptionExpiresAt() != null &&
                                      user.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());

        return SubscriptionStatusDTO.builder()
                .telegramId(telegramId)
                .subscribed(isSubscribed || hasActiveSubscription)
                .verified(user.getSubscriptionVerifiedAt() != null)
                .verifiedAt(user.getSubscriptionVerifiedAt())
                .expiresAt(user.getSubscriptionExpiresAt())
                .message(hasActiveSubscription ? "Active subscription" : "No active subscription")
                .build();
    }

    @Override
    public Map<String, Object> createSubscriptionResponse(Long userId, boolean isSubscribed) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("subscribed", isSubscribed);
        response.put("timestamp", LocalDateTime.now());

        if (isSubscribed) {
            response.put("message", "User has active subscription");
            response.put("status", "active");
        } else {
            response.put("message", "User subscription is not active");
            response.put("status", "inactive");
        }

        return response;
    }

    @Override
    public Map<String, Object> createSubscriptionErrorResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unable to verify subscription status");
        response.put("subscribed", false);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @Override
    public Map<String, Object> verifySubscriptionViaInitData(String initData) {
        try {
            if (!telegramAuthService.validateInitData(initData)) {
                return createSubscriptionErrorResponse();
            }

            Long telegramId = telegramAuthService.extractTelegramUserId(initData);
            SubscriptionStatusDTO status = getSubscriptionStatus(telegramId);

            return createSubscriptionResponse(telegramId, status.subscribed());
        } catch (Exception e) {
            log.error("Error verifying subscription via initData", e);
            return createSubscriptionErrorResponse();
        }
    }
}