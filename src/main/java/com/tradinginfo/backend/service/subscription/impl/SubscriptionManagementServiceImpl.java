package com.tradinginfo.backend.service.subscription.impl;

import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.repository.UserRepository;
import com.tradinginfo.backend.service.subscription.SubscriptionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionManagementServiceImpl implements SubscriptionManagementService {

    private final UserRepository userRepository;

    @Override
    public void grantPremiumAccess(Long telegramId, int durationDays) {
        log.info("Granting premium access to user {} for {} days", telegramId, durationDays);

        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for telegramId: {}", telegramId);
            return;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDate = now.plusDays(durationDays);

        user.setPremiumAccess(true);
        user.setSubscribed(true);
        user.setSubscriptionStartedAt(now);
        user.setSubscriptionExpiresAt(expirationDate);
        user.setSubscriptionVerifiedAt(now);

        userRepository.save(user);
        log.info("Premium access granted to user {} until {}", telegramId, expirationDate);
    }

    @Override
    public void revokePremiumAccess(Long telegramId) {
        log.info("Revoking premium access for user {}", telegramId);

        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for telegramId: {}", telegramId);
            return;
        }

        User user = userOpt.get();
        user.setPremiumAccess(false);
        user.setSubscribed(false);
        user.setSubscriptionExpiresAt(LocalDateTime.now()); // Set to current time to expire immediately

        userRepository.save(user);
        log.info("Premium access revoked for user {}", telegramId);
    }

    @Override
    public void handleSubscriptionVerification(Long telegramId, boolean verified) {
        log.info("Handling subscription verification for user {}: {}", telegramId, verified);

        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for telegramId: {}", telegramId);
            return;
        }

        User user = userOpt.get();

        if (verified) {
            user.setSubscribed(true);
            user.setSubscriptionVerifiedAt(LocalDateTime.now());

            // If no expiration date set, set default subscription period
            if (user.getSubscriptionExpiresAt() == null) {
                user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30)); // Default 30 days
            }

            // If no start date set, set current time
            if (user.getSubscriptionStartedAt() == null) {
                user.setSubscriptionStartedAt(LocalDateTime.now());
            }
        } else {
            user.setSubscribed(false);
            user.setSubscriptionVerifiedAt(null);
        }

        userRepository.save(user);
        log.info("Subscription verification updated for user {}: {}", telegramId, verified);
    }
}