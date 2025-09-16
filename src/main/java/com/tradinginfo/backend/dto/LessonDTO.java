package com.tradinginfo.backend.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record LessonDTO(
        Long id,
        String path,
        String title,
        String content,
        String htmlContent,
        Map<String, Object> frontmatter,
        Integer wordCount,
        String parentFolder,
        Integer lessonNumber) {
}