package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import com.tradinginfo.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
=======
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50
public class StatisticsController {

    private final StatisticsService statisticsService;

<<<<<<< HEAD
    @GetMapping("/user/{telegramId}")
    public ResponseEntity<UserStatisticsDTO> getUserStatistics(@PathVariable Long telegramId) {
        log.info("📈 Getting statistics for user {}", telegramId);
        UserStatisticsDTO statistics = statisticsService.getUserStatistics(telegramId);
        return ResponseEntity.ok(statistics);
    }
=======
    /**
     * Get user statistics
     */
    @GetMapping("/user/{telegramId}")
    public ResponseEntity<UserStatisticsDTO> getUserStatistics(@PathVariable Long telegramId) {
        log.info("Getting statistics for user {}", telegramId);
        UserStatisticsDTO statistics = statisticsService.getUserStatistics(telegramId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get current learning streak
     */
    @GetMapping("/streak/{telegramId}")
    public ResponseEntity<Integer> getCurrentStreak(@PathVariable Long telegramId) {
        log.info("Getting current streak for user {}", telegramId);
        Integer streak = statisticsService.getCurrentStreak(telegramId);
        return ResponseEntity.ok(streak);
    }

    /**
     * Get learning insights
     */
    @GetMapping("/insights/{telegramId}")
    public ResponseEntity<Map<String, Object>> getLearningInsights(@PathVariable Long telegramId) {
        Map<String, Object> insights = statisticsService.getLearningInsights(telegramId);
        return ResponseEntity.ok(insights);
    }

    /**
     * Get level progress
     */
    @GetMapping("/levels/{telegramId}")
    public ResponseEntity<Map<String, Double>> getLevelProgress(@PathVariable Long telegramId) {
        log.info("Getting level progress for user {}", telegramId);
        Map<String, Double> levelProgress = statisticsService.getLevelProgress(telegramId);
        return ResponseEntity.ok(levelProgress);
    }

    /**
     * Get achievements
     */
    @GetMapping("/achievements/{telegramId}")
    public ResponseEntity<Map<String, Object>> getAchievements(@PathVariable Long telegramId) {
        log.info("Getting achievements for user {}", telegramId);
        Map<String, Object> achievements = statisticsService.getAchievementsSummary(telegramId);
        return ResponseEntity.ok(achievements);
    }
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50
}