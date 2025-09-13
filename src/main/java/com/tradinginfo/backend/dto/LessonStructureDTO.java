package com.tradinginfo.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonStructureDTO {
    private String name;
    private String path;
    private List<LessonItemDTO> lessons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonItemDTO {
        private String title;
        private String path;
        private Integer lessonNumber;
        private Integer wordCount;
    }
}