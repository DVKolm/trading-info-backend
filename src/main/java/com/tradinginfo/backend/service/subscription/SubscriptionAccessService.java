package com.tradinginfo.backend.service.subscription;

public interface SubscriptionAccessService {
    boolean isPremiumLesson(String lessonPath);
    boolean hasAccessToLesson(Long telegramId, String lessonPath);
}