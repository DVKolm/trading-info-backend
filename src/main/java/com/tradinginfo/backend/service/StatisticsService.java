package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.entity.UserProgress;
import com.tradinginfo.backend.repository.UserProgressRepository;
import com.tradinginfo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {

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
    }
}