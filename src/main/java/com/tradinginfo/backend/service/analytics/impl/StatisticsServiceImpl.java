package com.tradinginfo.backend.service.analytics.impl;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.repository.UserRepository;
import com.tradinginfo.backend.service.analytics.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;

    @Override
    public UserStatisticsDTO getUserStatistics(Long telegramId) {
        log.info("Getting simplified user statistics for telegramId: {}", telegramId);

        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            return createEmptyStatistics();
        }

        User user = userOpt.get();

        return UserStatisticsDTO.builder()
                .telegramId(telegramId)
                .totalLessonsRead(0)
                .totalLessonsViewed(0)
                .completedLessons(0)
                .totalLessonsCompleted(0)
                .totalTimeSpent(0L)
                .averageReadingSpeed(0.0)
                .completionRate(0.0)
                .totalVisits(0)
                .currentStreak(0)
                .longestStreak(0)
                .lastActivity(user.getLastActive())
                .engagementLevel("basic")
                .levelProgress(Map.of("basic", 0.0))
                .recentActivity(List.of())
                .achievements(List.of())
                .build();
    }

    @Override
    public int getCurrentStreak(Long telegramId) {
        log.info("Getting current streak for telegramId: {}", telegramId);
        // Simplified implementation - no progress tracking
        return 0;
    }

    @Override
    public Map<String, Double> getLevelProgress(Long telegramId) {
        log.info("Getting level progress for telegramId: {}", telegramId);
        Map<String, Double> progress = new HashMap<>();
        progress.put("basic", 0.0);
        progress.put("intermediate", 0.0);
        progress.put("advanced", 0.0);
        return progress;
    }

    @Override
    public Map<String, Object> getAchievementsSummary(Long telegramId) {
        log.info("Getting achievements summary for telegramId: {}", telegramId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAchievements", 0);
        summary.put("recentAchievements", List.of());
        summary.put("availableAchievements", List.of("First Login", "Lesson Explorer", "Knowledge Seeker"));
        return summary;
    }

    @Override
    public Map<String, Object> getLearningInsights(Long telegramId) {
        log.info("Getting learning insights for telegramId: {}", telegramId);
        Map<String, Object> insights = new HashMap<>();
        insights.put("message", "Start your learning journey by exploring available lessons");
        insights.put("suggestion", "Begin with basic trading concepts");
        insights.put("nextSteps", List.of("Browse lesson folders", "Start with fundamentals"));
        return insights;
    }

    private UserStatisticsDTO createEmptyStatistics() {
        return UserStatisticsDTO.builder()
                .telegramId(null)
                .totalLessonsRead(0)
                .totalLessonsViewed(0)
                .completedLessons(0)
                .totalLessonsCompleted(0)
                .totalTimeSpent(0L)
                .averageReadingSpeed(0.0)
                .completionRate(0.0)
                .totalVisits(0)
                .currentStreak(0)
                .longestStreak(0)
                .lastActivity(LocalDateTime.now())
                .engagementLevel("new")
                .levelProgress(Map.of("basic", 0.0))
                .recentActivity(List.of())
                .achievements(List.of())
                .build();
    }
}