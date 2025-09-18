package com.tradinginfo.backend.service.analytics;

public interface UserAnalyticsService {
    void trackUserLogin(Long telegramId);
    void trackLessonAccess(Long telegramId, String lessonPath);
}