package com.tradinginfo.backend.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record UserStatisticsDTO(
        Integer totalLessonsViewed,
        Integer totalLessonsCompleted,
        Long totalTimeSpent,
        Double averageReadingSpeed,
        Integer currentStreak,
        Integer longestStreak,
        Double completionRate,
        Map<String, Double> levelProgress,
        List<Map<String, Object>> recentActivity,
        List<String> achievements) {
}