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
@RequestMapping("/progress")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class ProgressController {

    private final ProgressTrackingService progressTrackingService;

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackReadingSession(
            @RequestBody ReadingSessionDTO session) {
        log.info("📊 Tracking reading session for user {}", session.telegramId());
        progressTrackingService.trackReadingSession(session);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

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
}