package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record UserStatisticsDTO(
        Long telegramId,
        Integer totalLessonsRead,
        Integer totalLessonsViewed,
        Integer completedLessons,
        Integer totalLessonsCompleted,
        Long totalTimeSpent,
        Double averageReadingSpeed,
        Double completionRate,
        Integer totalVisits,
        Integer currentStreak,
        Integer longestStreak,
        LocalDateTime lastActivity,
        String engagementLevel,
        Map<String, Double> levelProgress,
        List<Map<String, Object>> recentActivity,
        List<String> achievements) {
}