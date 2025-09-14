package com.tradinginfo.backend.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record FolderDTO(
        String name,
        String path,
        Integer lessonCount,
        List<String> lessonPaths) {
}