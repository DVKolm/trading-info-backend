package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.ProgressMetricsDTO;
import com.tradinginfo.backend.dto.ReadingSessionDTO;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgressTrackingService {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final Optional<RedisCacheService> redisCacheService;

    public void trackReadingSession(ReadingSessionDTO session) {
        User user = userRepository.findByTelegramId(session.telegramId())
                .orElseGet(() -> createNewUser(session.telegramId()));

        UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), session.lessonPath())
                .orElseGet(() -> {
                    UserProgress newProgress = new UserProgress();
                    newProgress.setUser(user);
                    newProgress.setLessonPath(session.lessonPath());
                    newProgress.setVisits(0);
                    newProgress.setTimeSpent(0);
                    return newProgress;
                });

        progress.setTimeSpent(progress.getTimeSpent() + session.activeTime().intValue());
        progress.setScrollProgress(BigDecimal.valueOf(session.scrollProgress()));
        progress.setLastVisited(LocalDateTime.now());
        progress.setVisits(progress.getVisits() + 1);

        userProgressRepository.save(progress);
        log.info("📊 Tracked reading session for user {} on lesson {}", session.telegramId(), session.lessonPath());
    }

    public ProgressMetricsDTO getProgressMetrics(Long telegramId, String lessonPath) {
        UserProgress progress = userProgressRepository.findByTelegramId(telegramId, lessonPath).orElse(null);

        if (progress == null) {
            return ProgressMetricsDTO.builder()
                    .timeSpent(0L)
                    .scrollProgress(0)
                    .readingSpeed(0.0)
                    .completionScore(0.0)
                    .engagementLevel("new")
                    .visits(0)
                    .lastVisited(System.currentTimeMillis())
                    .build();
        }

        return ProgressMetricsDTO.builder()
                .timeSpent(progress.getTimeSpent().longValue())
                .scrollProgress(progress.getScrollProgress().intValue())
                .readingSpeed(progress.getReadingSpeed().doubleValue())
                .completionScore(progress.getCompletionScore().doubleValue())
                .engagementLevel(progress.getEngagementLevel())
                .visits(progress.getVisits())
                .lastVisited(progress.getLastVisited().toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                .build();
    }

    public List<UserProgress> getUserProgress(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            return List.of();
        }
        return userProgressRepository.findByUserId(user.getId());
    }

    private User createNewUser(Long telegramId) {
        User newUser = new User();
        newUser.setTelegramId(telegramId);
        newUser.setCreatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }
}