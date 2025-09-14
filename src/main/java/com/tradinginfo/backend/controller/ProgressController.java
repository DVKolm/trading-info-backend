package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.ProgressMetricsDTO;
import com.tradinginfo.backend.dto.ReadingSessionDTO;
import com.tradinginfo.backend.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgressController {

    private final ProgressTrackingService progressTrackingService;

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