package com.tradinginfo.backend.service.analytics;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import java.util.Map;

public interface StatisticsService {

    // Primary statistics methods
    UserStatisticsDTO getUserStatistics(Long telegramId);

    // Simplified statistics without progress tracking
    int getCurrentStreak(Long telegramId);
    Map<String, Double> getLevelProgress(Long telegramId);
    Map<String, Object> getAchievementsSummary(Long telegramId);
    Map<String, Object> getLearningInsights(Long telegramId);
}