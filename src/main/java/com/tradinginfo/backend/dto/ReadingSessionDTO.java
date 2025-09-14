package com.tradinginfo.backend.dto;

import lombok.Builder;

@Builder
public record ReadingSessionDTO(
        Long startTime,
        Long activeTime,
        Long lastActivityTime,
        Integer scrollProgress,
        Integer wordCount,
        String lessonPath,
        Integer engagementPoints,
        Long telegramId) {
}