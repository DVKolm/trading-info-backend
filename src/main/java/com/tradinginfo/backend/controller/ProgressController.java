package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.ProgressMetricsDTO;
import com.tradinginfo.backend.dto.ReadingSessionDTO;
import com.tradinginfo.backend.entity.UserProgress;
import com.tradinginfo.backend.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgressController {

    private final ProgressTrackingService progressTrackingService;

    // Legacy endpoint for backward compatibility
    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackReadingSession(
            @RequestBody ReadingSessionDTO session) {
        log.info("📊 Tracking reading session for user {}", session.getTelegramId());
        progressTrackingService.trackReadingSession(session);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // Legacy endpoint for backward compatibility
    @GetMapping("/metrics")
    public ResponseEntity<ProgressMetricsDTO> getProgressMetrics(
            @RequestParam Long telegramId,
            @RequestParam String lessonPath) {
        log.info("📈 Getting progress metrics for user {} on lesson {}", telegramId, lessonPath);
        ProgressMetricsDTO metrics = progressTrackingService.getProgressMetrics(telegramId, lessonPath);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/user/{telegramId}")
    public ResponseEntity<List<UserProgress>> getUserProgress(@PathVariable Long telegramId) {
        log.info("📊 Getting user progress for {}", telegramId);
        List<UserProgress> progress = progressTrackingService.getUserProgress(telegramId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Start a new reading session
     */
    @PostMapping("/session/start")
    public ResponseEntity<ReadingSessionDTO> startSession(
            @RequestParam Long telegramId,
            @RequestParam String lessonPath,
            @RequestParam Integer wordCount) {

        log.info("Starting session for user {} on lesson {}", telegramId, lessonPath);
        ReadingSessionDTO session = progressTrackingService.startSession(telegramId, lessonPath, wordCount);
        return ResponseEntity.ok(session);
    }

    /**
     * Update scroll progress
     */
    @PostMapping("/session/scroll")
    public ResponseEntity<Void> updateScroll(
            @RequestParam Long telegramId,
            @RequestParam String lessonPath,
            @RequestParam Integer scrollProgress) {

        progressTrackingService.updateScrollProgress(telegramId, lessonPath, scrollProgress);
        return ResponseEntity.ok().build();
    }

    /**
     * End reading session
     */
    @PostMapping("/session/end")
    public ResponseEntity<ProgressMetricsDTO> endSession(
            @RequestParam Long telegramId,
            @RequestParam String lessonPath) {

        log.info("Ending session for user {} on lesson {}", telegramId, lessonPath);
        ProgressMetricsDTO metrics = progressTrackingService.endSession(telegramId, lessonPath);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get progress metrics
     */
    @GetMapping("/metrics/{telegramId}/{lessonPath}")
    public ResponseEntity<ProgressMetricsDTO> getMetrics(
            @PathVariable Long telegramId,
            @PathVariable String lessonPath) {

        ProgressMetricsDTO metrics = progressTrackingService.getProgressMetrics(telegramId, lessonPath);
        return metrics != null ? ResponseEntity.ok(metrics) : ResponseEntity.noContent().build();
    }

    /**
     * Track analytics event
     */
    @PostMapping("/event")
    public ResponseEntity<Void> trackEvent(@RequestBody Map<String, Object> eventData) {
        Long telegramId = Long.valueOf(eventData.get("telegramId").toString());
        String eventType = (String) eventData.get("eventType");
        String lessonPath = (String) eventData.get("lessonPath");

        progressTrackingService.trackEvent(telegramId, eventType, lessonPath, eventData);
        return ResponseEntity.ok().build();
    }
}