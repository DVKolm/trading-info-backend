package com.tradinginfo.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {
    private Long id;
    private String path;
    private String title;
    private String content;
    private String htmlContent;
    private Map<String, Object> frontmatter;
    private Integer wordCount;
    private String parentFolder;
    private Integer lessonNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}