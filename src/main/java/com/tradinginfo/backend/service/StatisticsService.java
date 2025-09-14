package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
<<<<<<< HEAD
import com.tradinginfo.backend.entity.User;
=======
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50
import com.tradinginfo.backend.entity.UserProgress;
import com.tradinginfo.backend.repository.UserProgressRepository;
import com.tradinginfo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
=======
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {

<<<<<<< HEAD
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;

    public UserStatisticsDTO getUserStatistics(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            return UserStatisticsDTO.builder()
                    .telegramId(telegramId)
                    .totalLessonsRead(0)
                    .completedLessons(0)
                    .totalTimeSpent(0L)
                    .averageReadingSpeed(0.0)
                    .completionRate(0.0)
                    .totalVisits(0)
                    .lastActivity(LocalDateTime.now())
                    .engagementLevel("new")
                    .build();
        }

        List<UserProgress> progressList = userProgressRepository.findByUserId(user.getId());

        int totalLessonsRead = progressList.size();
        int completedLessons = (int) progressList.stream()
                .filter(p -> Boolean.TRUE.equals(p.getCompleted()))
                .count();

        long totalTimeSpent = progressList.stream()
                .mapToLong(p -> p.getTimeSpent() != null ? p.getTimeSpent() : 0)
                .sum();

        double averageReadingSpeed = progressList.stream()
                .filter(p -> p.getReadingSpeed() != null && p.getReadingSpeed().compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(p -> p.getReadingSpeed().doubleValue())
                .average()
                .orElse(0.0);

        double completionRate = totalLessonsRead > 0 ? (double) completedLessons / totalLessonsRead * 100 : 0.0;

        int totalVisits = progressList.stream()
                .mapToInt(p -> p.getVisits() != null ? p.getVisits() : 0)
                .sum();

        LocalDateTime lastActivity = progressList.stream()
                .filter(p -> p.getLastVisited() != null)
                .map(UserProgress::getLastVisited)
                .max(LocalDateTime::compareTo)
                .orElse(user.getLastActive());

        String engagementLevel = calculateEngagementLevel(totalVisits, completionRate, totalTimeSpent);

        return UserStatisticsDTO.builder()
                .telegramId(telegramId)
                .totalLessonsRead(totalLessonsRead)
                .completedLessons(completedLessons)
                .totalTimeSpent(totalTimeSpent)
                .averageReadingSpeed(averageReadingSpeed)
                .completionRate(completionRate)
                .totalVisits(totalVisits)
                .lastActivity(lastActivity)
                .engagementLevel(engagementLevel)
                .build();
    }

    private String calculateEngagementLevel(int totalVisits, double completionRate, long totalTimeSpent) {
        if (totalVisits == 0) return "new";
        if (totalVisits < 5) return "beginner";
        if (completionRate > 75 && totalTimeSpent > 3600) return "expert";
        if (completionRate > 50 && totalTimeSpent > 1800) return "advanced";
        if (completionRate > 25) return "intermediate";
        return "casual";
=======
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final Optional<RedisCacheService> redisCacheService;

    /**
     * Get comprehensive statistics for a user
     */
    public UserStatisticsDTO getUserStatistics(Long telegramId) {
        // Check cache first
        Optional<UserStatisticsDTO> cached = redisCacheService
                .flatMap(cache -> Optional.ofNullable(cache.getCachedUserStatistics(telegramId)))
                .map(UserStatisticsDTO.class::cast);

        if (cached.isPresent()) {
            return cached.get();
        }

        // Calculate statistics
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            return UserStatisticsDTO.builder()
                    .totalLessonsViewed(0)
                    .totalLessonsCompleted(0)
                    .totalTimeSpent(0L)
                    .averageReadingSpeed(0.0)
                    .currentStreak(0)
                    .longestStreak(0)
                    .completionRate(0.0)
                    .build();
        }

        List<UserProgress> allProgress = userProgressRepository.findByUserId(user.getId());

        UserStatisticsDTO statistics = UserStatisticsDTO.builder()
                .totalLessonsViewed(allProgress.size())
                .totalLessonsCompleted(countCompletedLessons(allProgress))
                .totalTimeSpent(calculateTotalTimeSpent(allProgress))
                .averageReadingSpeed(calculateAverageReadingSpeed(allProgress))
                .currentStreak(calculateCurrentStreak(allProgress))
                .longestStreak(calculateLongestStreak(allProgress))
                .completionRate(calculateCompletionRate(allProgress))
                .levelProgress(calculateLevelProgress(allProgress))
                .recentActivity(getRecentActivity(allProgress))
                .achievements(calculateAchievements(allProgress))
                .build();

        // Cache the result
        redisCacheService.ifPresent(cache -> cache.cacheUserStatistics(telegramId, statistics));

        return statistics;
    }

    /**
     * Calculate learning streak (consecutive days)
     */
    public int calculateCurrentStreak(List<UserProgress> progressList) {
        if (progressList.isEmpty()) return 0;

        // Get unique days when user was active
        List<LocalDate> activeDays = progressList.stream()
                .map(p -> p.getLastVisited().toLocalDate())
                .collect(Collectors.toSet())
                .stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (activeDays.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate currentDate = today;

        // Check if user was active today or yesterday to start the streak
        if (!activeDays.contains(today) && !activeDays.contains(today.minusDays(1))) {
            return 0;
        }

        // If not active today, start from yesterday
        if (!activeDays.contains(today)) {
            currentDate = today.minusDays(1);
        }

        // Count consecutive days
        while (activeDays.contains(currentDate)) {
            streak++;
            currentDate = currentDate.minusDays(1);
        }

        return streak;
    }

    /**
     * Calculate longest streak ever achieved
     */
    private int calculateLongestStreak(List<UserProgress> progressList) {
        if (progressList.isEmpty()) return 0;

        List<LocalDate> activeDays = progressList.stream()
                .map(p -> p.getLastVisited().toLocalDate())
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .collect(Collectors.toList());

        if (activeDays.isEmpty()) return 0;

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < activeDays.size(); i++) {
            LocalDate prevDay = activeDays.get(i - 1);
            LocalDate currentDay = activeDays.get(i);

            if (ChronoUnit.DAYS.between(prevDay, currentDay) == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    /**
     * Count completed lessons
     */
    private int countCompletedLessons(List<UserProgress> progressList) {
        return (int) progressList.stream()
                .filter(UserProgress::getCompleted)
                .count();
    }

    /**
     * Calculate total time spent across all lessons
     */
    private long calculateTotalTimeSpent(List<UserProgress> progressList) {
        return progressList.stream()
                .mapToLong(UserProgress::getTimeSpent)
                .sum();
    }

    /**
     * Calculate average reading speed
     */
    private double calculateAverageReadingSpeed(List<UserProgress> progressList) {
        List<Double> speeds = progressList.stream()
                .map(UserProgress::getReadingSpeed)
                .filter(speed -> speed > 0)
                .collect(Collectors.toList());

        if (speeds.isEmpty()) return 0.0;

        return speeds.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate overall completion rate
     */
    private double calculateCompletionRate(List<UserProgress> progressList) {
        if (progressList.isEmpty()) return 0.0;

        int completed = countCompletedLessons(progressList);
        return (completed * 100.0) / progressList.size();
    }

    /**
     * Calculate progress by level
     */
    private Map<String, Double> calculateLevelProgress(List<UserProgress> progressList) {
        Map<String, Double> levelProgress = new HashMap<String, Double>();

        List<String> levels = Arrays.asList(
                "Начальный уровень (Бесплатно)",
                "Средний уровень (Подписка)",
                "Продвинутый уровень (Подписка)",
                "Эксперт уровень (Подписка)"
        );

        for (String level : levels) {
            List<UserProgress> levelLessons = progressList.stream()
                    .filter(p -> p.getLessonPath().contains(level))
                    .collect(Collectors.toList());

            if (!levelLessons.isEmpty()) {
                long completed = levelLessons.stream()
                        .filter(UserProgress::getCompleted)
                        .count();
                double progress = (completed * 100.0) / levelLessons.size();
                levelProgress.put(level, progress);
            } else {
                levelProgress.put(level, 0.0);
            }
        }

        return levelProgress;
    }

    /**
     * Get recent activity summary
     */
    private List<Map<String, Object>> getRecentActivity(List<UserProgress> progressList) {
        return progressList.stream()
                .sorted(Comparator.comparing(UserProgress::getLastVisited).reversed())
                .limit(10)
                .map(progress -> {
                    Map<String, Object> activity = new HashMap<String, Object>();
                    activity.put("lessonPath", progress.getLessonPath());
                    activity.put("lastVisited", progress.getLastVisited());
                    activity.put("timeSpent", progress.getTimeSpent());
                    activity.put("completed", progress.getCompleted());
                    activity.put("completionScore", progress.getCompletionScore());
                    return activity;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate user achievements
     */
    private List<String> calculateAchievements(List<UserProgress> progressList) {
        List<String> achievements = new ArrayList<String>();

        // First lesson completed
        if (countCompletedLessons(progressList) >= 1) {
            achievements.add("FIRST_LESSON");
        }

        // 5 lessons completed
        if (countCompletedLessons(progressList) >= 5) {
            achievements.add("FIVE_LESSONS");
        }

        // 10 lessons completed
        if (countCompletedLessons(progressList) >= 10) {
            achievements.add("TEN_LESSONS");
        }

        // Week streak
        if (calculateCurrentStreak(progressList) >= 7) {
            achievements.add("WEEK_STREAK");
        }

        // Month streak
        if (calculateCurrentStreak(progressList) >= 30) {
            achievements.add("MONTH_STREAK");
        }

        // Speed reader (average > 250 wpm)
        if (calculateAverageReadingSpeed(progressList) > 250) {
            achievements.add("SPEED_READER");
        }

        // Dedicated learner (total time > 10 hours)
        if (calculateTotalTimeSpent(progressList) > 10 * 60 * 60 * 1000) {
            achievements.add("DEDICATED_LEARNER");
        }

        // Level master (completed all lessons in a level)
        Map<String, Double> levelProgress = calculateLevelProgress(progressList);
        for (Map.Entry<String, Double> entry : levelProgress.entrySet()) {
            if (entry.getValue() >= 100.0) {
                achievements.add("LEVEL_MASTER_" + entry.getKey().toUpperCase().replace(" ", "_"));
            }
        }

        return achievements;
    }

    /**
     * Get current streak for a user
     */
    public int getCurrentStreak(Long telegramId) {
        UserStatisticsDTO statistics = getUserStatistics(telegramId);
        return statistics.currentStreak();
    }

    /**
     * Get level progress for a user
     */
    public Map<String, Double> getLevelProgress(Long telegramId) {
        UserStatisticsDTO statistics = getUserStatistics(telegramId);
        return statistics.levelProgress();
    }

    /**
     * Get achievements summary for a user
     */
    public Map<String, Object> getAchievementsSummary(Long telegramId) {
        UserStatisticsDTO statistics = getUserStatistics(telegramId);
        Map<String, Object> achievementSummary = new HashMap<>();
        achievementSummary.put("achievements", statistics.achievements());
        achievementSummary.put("totalCompleted", statistics.totalLessonsCompleted());
        achievementSummary.put("currentStreak", statistics.currentStreak());
        return achievementSummary;
    }

    /**
     * Get learning insights and recommendations
     */
    public Map<String, Object> getLearningInsights(Long telegramId) {
        UserStatisticsDTO statistics = getUserStatistics(telegramId);
        Map<String, Object> insights = new HashMap<String, Object>();

        // Reading pace insight
        if (statistics.getAverageReadingSpeed() < 150) {
            insights.put("readingPace", "Your reading speed is below average. Try to focus more during reading sessions.");
        } else if (statistics.getAverageReadingSpeed() > 250) {
            insights.put("readingPace", "Excellent reading speed! You're a fast learner.");
        } else {
            insights.put("readingPace", "Good reading speed. Keep it up!");
        }

        // Streak insight
        if (statistics.getCurrentStreak() == 0) {
            insights.put("streak", "Start your learning streak today!");
        } else if (statistics.getCurrentStreak() < 7) {
            insights.put("streak", "Keep going! " + (7 - statistics.getCurrentStreak()) + " more days to a week streak!");
        } else {
            insights.put("streak", "Amazing! You've been learning for " + statistics.getCurrentStreak() + " days straight!");
        }

        // Completion rate insight
        if (statistics.getCompletionRate() < 50) {
            insights.put("completion", "Try to complete more lessons to improve your understanding.");
        } else if (statistics.getCompletionRate() < 80) {
            insights.put("completion", "Good progress! Aim to complete more lessons.");
        } else {
            insights.put("completion", "Excellent completion rate! You're very thorough.");
        }

        // Next recommended action
        insights.put("nextAction", getNextRecommendedAction(statistics));

        return insights;
    }

    private String getNextRecommendedAction(UserStatisticsDTO statistics) {
        if (statistics.getTotalLessonsViewed() == 0) {
            return "Start with your first lesson in the Beginner level";
        }
        if (statistics.getCurrentStreak() == 0) {
            return "Resume your learning to maintain your streak";
        }
        if (statistics.getCompletionRate() < 50) {
            return "Focus on completing the lessons you've started";
        }
        return "Continue to the next lesson in your current level";
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50
    }
}