package com.tradinginfo.backend.service.lesson;

import com.tradinginfo.backend.dto.FolderDTO;

import java.util.List;

public interface LessonFolderService {
    List<FolderDTO> getLessonFolders();
}