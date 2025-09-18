package com.tradinginfo.backend.dto;

import java.io.Serializable;
import java.util.List;

public record FolderDTO(
        String name,
        String path,
        Integer lessonCount,
        List<String> lessonPaths) implements Serializable {

    public static FolderDTO create(String name, String path) {
        return new FolderDTO(name, path, 0, List.of());
    }
}