package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ProgressDTO(
        String lessonPath,
        Integer timeSpent,
        BigDecimal scrollProgress,
        BigDecimal readingSpeed,
        BigDecimal completionScore,
        String engagementLevel,
        Boolean completed) {
}