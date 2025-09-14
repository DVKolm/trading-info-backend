package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.service.UploadService;
import com.tradinginfo.backend.service.TelegramAuthService;
import com.tradinginfo.backend.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class UploadController {

    private final UploadService uploadService;
    private final TelegramAuthService telegramAuthService;
    private final Optional<RedisCacheService> redisCacheService;

    @PostMapping("/lessons")
    public ResponseEntity<Map<String, Object>> uploadLessons(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFolder") String targetFolder,
            @RequestHeader("X-Telegram-User-Id") Long telegramId) {

        log.info("📤 Uploading lessons to folder: {}", targetFolder);
        Map<String, Object> result = uploadService.uploadLessons(file, targetFolder, telegramId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/lessons/{folder}")
    public ResponseEntity<Map<String, String>> deleteLessonsFolder(
            @PathVariable String folder,
            @RequestHeader("X-Telegram-User-Id") Long telegramId) {

        log.info("🗑️ Deleting lessons folder: {}", folder);
        uploadService.deleteLessonsFolder(folder, telegramId);
        return ResponseEntity.ok(Map.of("message", "Folder deleted successfully"));
    }

    @PostMapping("/lesson")
    public ResponseEntity<Map<String, Object>> uploadSingleLesson(
            @RequestParam("lesson") MultipartFile file,
            @RequestParam(value = "targetFolder", required = false) String targetFolder,
            @RequestParam("initData") String initData) {

        log.info("📤 Uploading single lesson file: {}", file.getOriginalFilename());

        // Extract telegram user ID from initData
        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Validate initData and check admin permissions
        if (!telegramAuthService.validateInitData(initData)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid authentication data"));
        }

        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Map<String, Object> result = uploadService.uploadSingleLesson(file, targetFolder, telegramId);

        // Clear lesson cache after successful upload
        if ((Boolean) result.getOrDefault("success", false)) {
            redisCacheService.ifPresent(RedisCacheService::clearLessonCache);
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/lesson")
    public ResponseEntity<Map<String, String>> deleteSingleLesson(
            @RequestBody Map<String, String> request) {

        String initData = request.get("initData");
        String lessonPath = request.get("lessonPath");

        log.info("🗑️ Deleting single lesson: {}", lessonPath);

        // Extract telegram user ID from initData
        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Validate initData and check admin permissions
        if (!telegramAuthService.validateInitData(initData)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid authentication data"));
        }

        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            uploadService.deleteSingleLesson(lessonPath, telegramId);

            // Clear lesson cache after successful deletion
            redisCacheService.ifPresent(RedisCacheService::clearLessonCache);

            return ResponseEntity.ok(Map.of("message", "Lesson deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting lesson: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete lesson: " + e.getMessage()));
        }
    }

    @GetMapping("/file-tree")
    public ResponseEntity<Map<String, Object>> getFileTree(
            @RequestHeader("X-Telegram-User-Id") Long telegramId) {

        log.info("🗂️ Getting file tree for admin panel");

        // Check admin permissions
        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Get lesson structure and transform for admin panel
        Map<String, Object> result = uploadService.getFileTreeForAdmin();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/folder")
    public ResponseEntity<Map<String, String>> createFolder(
            @RequestBody Map<String, String> request) {

        String initData = request.get("initData");
        String folderName = request.get("folderName");

        log.info("📁 Creating folder: {}", folderName);

        // Extract telegram user ID from initData
        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Validate initData and check admin permissions
        if (!telegramAuthService.validateInitData(initData)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid authentication data"));
        }

        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            uploadService.createFolder(folderName, telegramId);

            // Clear lesson cache after folder creation
            redisCacheService.ifPresent(RedisCacheService::clearLessonCache);

            return ResponseEntity.ok(Map.of("message", "Folder created successfully: " + folderName));
        } catch (Exception e) {
            log.error("Error creating folder: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create folder: " + e.getMessage()));
        }
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam("initData") String initData) {

        log.info("🧹 Clearing lesson cache");
        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Validate admin permissions
        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Clear Redis cache
        if (redisCacheService.isPresent()) {
            redisCacheService.get().clearAllCache();
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
        } else {
            return ResponseEntity.ok(Map.of("message", "Redis not available, no cache to clear"));
        }
    }
}