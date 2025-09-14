package com.tradinginfo.backend.dto;

public record FolderDTO(String name, String path) {
    public static FolderDTO create(String name, String path) {
        return new FolderDTO(name, path);
    }
}