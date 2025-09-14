package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record FolderDTO(
        String name,
        String path,
        Integer lessonCount,
        List<String> lessonPaths) {

    public static FolderDTO create(String name, String path) {
        return FolderDTO.builder()
                .name(name)
                .path(path)
                .lessonCount(0)
                .lessonPaths(List.of())
                .build();
    }
}