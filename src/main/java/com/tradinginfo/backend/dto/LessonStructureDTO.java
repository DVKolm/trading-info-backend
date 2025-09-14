package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record LessonStructureDTO(
        String name,
        String path,
        List<LessonItemDTO> lessons) {

    @Builder
    public record LessonItemDTO(
            String title,
            String path,
            Integer lessonNumber,
            Integer wordCount) {
    }
}