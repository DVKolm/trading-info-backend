package com.tradinginfo.backend.dto;

import java.math.BigDecimal;

public record ProgressDTO(
        String lessonPath,
        Integer timeSpent,
        BigDecimal scrollProgress,
        BigDecimal readingSpeed,
        BigDecimal completionScore,
        String engagementLevel,
        Boolean completed) {
}