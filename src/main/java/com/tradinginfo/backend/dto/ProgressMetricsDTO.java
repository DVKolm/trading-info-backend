package com.tradinginfo.backend.dto;

import lombok.Builder;

@Builder
public record ProgressMetricsDTO(
        Long timeSpent,
        Integer scrollProgress,
        Double readingSpeed,
        Double completionScore,
        String engagementLevel,
        Integer visits,
        Long lastVisited) {
}