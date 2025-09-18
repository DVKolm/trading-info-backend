package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.FolderDTO;
import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.dto.LessonStructureDTO;
import com.tradinginfo.backend.service.lesson.LessonFolderService;
import com.tradinginfo.backend.service.lesson.LessonStructureService;
import com.tradinginfo.backend.service.lesson.LessonContentService;
import com.tradinginfo.backend.service.lesson.LessonSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class LessonController {

    private final LessonFolderService lessonFolderService;
    private final LessonStructureService lessonStructureService;
    private final LessonContentService lessonContentService;
    private final LessonSearchService lessonSearchService;

    @GetMapping("/folders")
    public ResponseEntity<Map<String, List<FolderDTO>>> getLessonFolders() {
        log.info("Retrieving available lesson folders");
        List<FolderDTO> folders = lessonFolderService.getLessonFolders();
        log.info("Found {} lesson folders", folders.size());
        return ResponseEntity.ok(Map.of("folders", folders));
    }

    @GetMapping("/structure")
    public ResponseEntity<Map<String, List<LessonStructureDTO>>> getLessonStructure() {
        log.info("Building lesson structure tree");
        List<LessonStructureDTO> structure = lessonStructureService.getLessonStructure();
        log.info("Lesson structure built with {} folders", structure.size());
        return ResponseEntity.ok(Map.of("structure", structure));
    }

    @GetMapping("/content/{*path}")
    public ResponseEntity<LessonDTO> getLessonContent(
            @PathVariable String path,
            @RequestHeader(value = "X-Telegram-User-Id", required = false) Long telegramId) {
        log.info("Getting lesson content for path: {}", path);
        LessonDTO lesson = lessonContentService.getLessonContent(path, telegramId);
        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/resolve")
    public ResponseEntity<Map<String, String>> resolveLessonLink(@RequestParam String name) {
        log.info("Resolving internal link: {}", name);
        String resolvedPath = lessonContentService.resolveLessonLink(name);
        return ResponseEntity.ok(Map.of("path", resolvedPath));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, List<LessonDTO>>> searchLessons(@RequestParam("q") String query) {
        log.info("Searching lessons with query: {}", query);
        List<LessonDTO> results = lessonSearchService.searchLessons(query);
        return ResponseEntity.ok(Map.of("results", results));
    }

}