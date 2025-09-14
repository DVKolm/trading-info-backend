package com.tradinginfo.backend.dto;

import java.util.List;

public record LessonStructureDTO(
        String id,
        String name,
        String type,
        String path,
        String filename,
        List<LessonStructureDTO> children) {

    public static LessonStructureDTO createFolder(String id, String name, String path, List<LessonStructureDTO> children) {
        return new LessonStructureDTO(id, name, "folder", path, null, children);
    }

    public static LessonStructureDTO createFile(String id, String name, String path, String filename) {
        return new LessonStructureDTO(id, name, "file", path, filename, null);
    }
}