package com.tradinginfo.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private String lessonPath;
    private Integer timeSpent;
    private BigDecimal scrollProgress;
    private BigDecimal readingSpeed;
    private BigDecimal completionScore;
    private String engagementLevel;
    private Boolean completed;
}