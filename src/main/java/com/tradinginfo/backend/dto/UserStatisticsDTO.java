package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record UserStatisticsDTO(
        Long telegramId,
        Integer totalLessonsRead,
        Integer completedLessons,
        Long totalTimeSpent,
        Double averageReadingSpeed,
        Double completionRate,
        Integer totalVisits,
        LocalDateTime lastActivity,
        String engagementLevel) {
}