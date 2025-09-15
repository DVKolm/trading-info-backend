package com.tradinginfo.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadingSessionDTO {
    private Long startTime;
    private Long activeTime;
    private Long lastActivityTime;
    private Integer scrollProgress;
    private Integer wordCount;
    private String lessonPath;
    private Integer engagementPoints;
    private Long telegramId;
}