package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.io.Serializable;
import java.util.List;

@Builder
public record LessonStructureDTO(
        String id,
        String name,
        String type,
        String path,
        String filename,
        List<LessonStructureDTO> children,
        List<LessonItemDTO> lessons) implements Serializable {

    public static LessonStructureDTO createFolder(String id, String name, String path, List<LessonStructureDTO> children) {
        return new LessonStructureDTO(id, name, "folder", path, null, children, null);
    }

    public static LessonStructureDTO createFile(String id, String name, String path, String filename) {
        return new LessonStructureDTO(id, name, "file", path, filename, null, null);
    }

    @Builder
    public record LessonItemDTO(
            String title,
            String path,
            Integer lessonNumber,
            Integer wordCount) implements Serializable {
    }
}