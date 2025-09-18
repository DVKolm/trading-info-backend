package com.tradinginfo.backend.service.subscription;

public interface SubscriptionManagementService {
    void grantPremiumAccess(Long telegramId, int durationDays);
    void revokePremiumAccess(Long telegramId);
    void handleSubscriptionVerification(Long telegramId, boolean verified);
}