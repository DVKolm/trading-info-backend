package com.tradinginfo.backend.service.upload;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UploadService {

    // Lesson upload methods
    Map<String, Object> uploadLessons(MultipartFile file, String targetFolder, Long telegramId);
    Map<String, Object> uploadSingleLesson(MultipartFile file, String targetFolder, Long telegramId);

    // Deletion methods
    void deleteLessonsFolder(String folder, Long telegramId);
    void deleteSingleLesson(String lessonPath, Long telegramId);

    // Folder management methods
    void createFolder(String folderName, Long telegramId);
    void createFolder(String folderName, Long telegramId, Boolean subscriptionRequired);
    void updateFolderSubscription(String folderPath, Boolean subscriptionRequired);

    // Admin methods
    Map<String, Object> getFileTreeForAdmin();
}