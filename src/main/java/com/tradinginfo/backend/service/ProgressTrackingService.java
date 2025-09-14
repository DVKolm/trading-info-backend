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

<<<<<<< HEAD
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
=======
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgressTrackingService {

<<<<<<< HEAD
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
=======
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final Optional<RedisCacheService> redisCacheService;

    // Active sessions storage
    private final Map<String, ReadingSessionDTO> activeSessions = new ConcurrentHashMap<>();

    // Constants for calculations
    private static final int WORDS_PER_MINUTE_AVERAGE = 200;
    private static final double TIME_MULTIPLIER = 0.4;
    private static final double SCROLL_MULTIPLIER = 0.4;
    private static final double ENGAGEMENT_MULTIPLIER = 0.2;
    private static final long COMPLETION_TIME_THRESHOLD = 5 * 60 * 1000; // 5 minutes
    private static final int COMPLETION_SCROLL_THRESHOLD = 80;

    /**
     * Start a new reading session for a user
     */
    public ReadingSessionDTO startSession(Long telegramId, String lessonPath, int wordCount) {
        log.debug("Starting reading session for user {} on lesson {}", telegramId, lessonPath);

        String sessionKey = generateSessionKey(telegramId, lessonPath);
        long now = System.currentTimeMillis();

        ReadingSessionDTO session = ReadingSessionDTO.builder()
                .startTime(now)
                .activeTime(0L)
                .lastActivityTime(now)
                .scrollProgress(0)
                .wordCount(wordCount)
                .lessonPath(lessonPath)
                .engagementPoints(0)
                .telegramId(telegramId)
                .build();

        activeSessions.put(sessionKey, session);

        // Update visit count
        incrementVisitCount(telegramId, lessonPath);

        return session;
    }

    /**
     * Update scroll progress for an active session
     */
    public void updateScrollProgress(Long telegramId, String lessonPath, int scrollProgress) {
        String sessionKey = generateSessionKey(telegramId, lessonPath);
        ReadingSessionDTO session = activeSessions.get(sessionKey);

        if (session != null) {
            long now = System.currentTimeMillis();

            // Update active time
            if (session.getLastActivityTime() != null) {
                long timeSinceLastActivity = now - session.getLastActivityTime();
                if (timeSinceLastActivity < 30000) { // Consider active if within 30 seconds
                    session.setActiveTime(session.getActiveTime() + timeSinceLastActivity);
                }
            }

            session.setScrollProgress(Math.max(session.getScrollProgress(), scrollProgress));
            session.setLastActivityTime(now);

            // Award engagement points for scroll milestones
            if (scrollProgress >= 25 && session.getEngagementPoints() < 25) {
                session.setEngagementPoints(25);
            } else if (scrollProgress >= 50 && session.getEngagementPoints() < 50) {
                session.setEngagementPoints(50);
            } else if (scrollProgress >= 75 && session.getEngagementPoints() < 75) {
                session.setEngagementPoints(75);
            } else if (scrollProgress >= 90 && session.getEngagementPoints() < 100) {
                session.setEngagementPoints(100);
            }

            activeSessions.put(sessionKey, session);
        }
    }

    /**
     * End a reading session and calculate final metrics
     */
    public ProgressMetricsDTO endSession(Long telegramId, String lessonPath) {
        String sessionKey = generateSessionKey(telegramId, lessonPath);
        ReadingSessionDTO session = activeSessions.remove(sessionKey);

        if (session == null) {
            log.warn("No active session found for user {} on lesson {}", telegramId, lessonPath);
            return null;
        }

        long now = System.currentTimeMillis();
        long totalTime = session.getActiveTime() + (now - session.getLastActivityTime());

        // Calculate metrics
        double readingSpeed = calculateReadingSpeed(session.getWordCount(), totalTime);
        double completionScore = calculateCompletionScore(
                totalTime,
                session.getScrollProgress(),
                session.getEngagementPoints(),
                session.getWordCount()
        );
        String engagementLevel = determineEngagementLevel(completionScore);

        // Save to database
        saveProgress(telegramId, lessonPath, totalTime, session.getScrollProgress(),
                    readingSpeed, completionScore, engagementLevel);

        return ProgressMetricsDTO.builder()
                .timeSpent(totalTime)
                .scrollProgress(session.getScrollProgress())
                .readingSpeed(readingSpeed)
                .completionScore(completionScore)
                .engagementLevel(engagementLevel)
                .build();
    }

    /**
     * Calculate reading speed in words per minute
     */
    private double calculateReadingSpeed(int wordCount, long timeSpent) {
        if (timeSpent <= 0) return 0;
        return (wordCount / (timeSpent / 60000.0));
    }

    /**
     * Calculate completion score based on multiple factors
     */
    private double calculateCompletionScore(long timeSpent, int scrollProgress,
                                           int engagementPoints, int wordCount) {
        // Expected reading time in milliseconds
        double expectedReadingTime = (wordCount / (double) WORDS_PER_MINUTE_AVERAGE) * 60000;

        // Time score (capped at 1.0)
        double timeScore = Math.min(timeSpent / expectedReadingTime, 1.0) * TIME_MULTIPLIER;

        // Scroll score
        double scrollScore = (scrollProgress / 100.0) * SCROLL_MULTIPLIER;

        // Engagement score
        double engagementScore = (engagementPoints / 100.0) * ENGAGEMENT_MULTIPLIER;

        // Calculate final score
        double finalScore = timeScore + scrollScore + engagementScore;

        // Consider lesson complete if criteria met
        if (timeSpent >= COMPLETION_TIME_THRESHOLD && scrollProgress >= COMPLETION_SCROLL_THRESHOLD) {
            finalScore = Math.max(finalScore, 0.8);
        }

        return Math.min(finalScore, 1.0);
    }

    /**
     * Determine engagement level based on completion score
     */
    private String determineEngagementLevel(double completionScore) {
        if (completionScore >= 0.7) return "high";
        if (completionScore >= 0.4) return "medium";
        return "low";
    }

    /**
     * Save progress to database
     */
    private void saveProgress(Long telegramId, String lessonPath, long timeSpent,
                             int scrollProgress, double readingSpeed,
                             double completionScore, String engagementLevel) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + telegramId));

        UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), lessonPath)
                .orElseGet(() -> createNewProgress(user, lessonPath));

        // Update progress data
        progress.setTimeSpent(progress.getTimeSpent() + timeSpent);
        progress.setScrollProgress(scrollProgress);
        progress.setReadingSpeed(readingSpeed);
        progress.setCompletionScore(completionScore);
        progress.setEngagementLevel(engagementLevel);
        progress.setLastVisited(LocalDateTime.now());

        // Mark as completed if score is high enough
        if (completionScore >= 0.8) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        userProgressRepository.save(progress);

        // Invalidate cache
        redisCacheService.ifPresent(cache -> cache.invalidateUserProgress(telegramId));
    }

    /**
     * Create new progress record
     */
    private UserProgress createNewProgress(User user, String lessonPath) {
        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setLessonPath(lessonPath);
        progress.setTimeSpent(0L);
        progress.setScrollProgress(0);
        progress.setReadingSpeed(0.0);
        progress.setCompletionScore(0.0);
        progress.setEngagementLevel("low");
        progress.setVisits(0);
        progress.setCompleted(false);
        progress.setCreatedAt(LocalDateTime.now());
        return progress;
    }

    /**
     * Increment visit count for a lesson
     */
    private void incrementVisitCount(Long telegramId, String lessonPath) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user != null) {
            UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), lessonPath)
                    .orElseGet(() -> createNewProgress(user, lessonPath));
            progress.setVisits(progress.getVisits() + 1);
            progress.setLastVisited(LocalDateTime.now());
            userProgressRepository.save(progress);
        }
    }

    /**
     * Get progress metrics for a user and lesson
     */
    public ProgressMetricsDTO getProgressMetrics(Long telegramId, String lessonPath) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) return null;

        UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), lessonPath)
                .orElse(null);

        if (progress == null) return null;

        return ProgressMetricsDTO.builder()
                .timeSpent(progress.getTimeSpent())
                .scrollProgress(progress.getScrollProgress())
                .readingSpeed(progress.getReadingSpeed())
                .completionScore(progress.getCompletionScore())
                .engagementLevel(progress.getEngagementLevel())
                .visits(progress.getVisits())
                .lastVisited(progress.getLastVisited().toInstant(ZoneOffset.UTC).toEpochMilli())
                .build();
    }

    /**
     * Track analytics event
     */
    public void trackEvent(Long telegramId, String eventType, String lessonPath, Map<String, Object> data) {
        log.debug("Tracking event {} for user {} on lesson {}", eventType, telegramId, lessonPath);

        // Store event in cache for batch processing
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("telegramId", telegramId);
        eventData.put("eventType", eventType);
        eventData.put("lessonPath", lessonPath);
        eventData.put("timestamp", System.currentTimeMillis());
        if (data != null) {
            eventData.putAll(data);
        }

        redisCacheService.ifPresent(cache -> cache.storeAnalyticsEvent(eventData));
    }

    private String generateSessionKey(Long telegramId, String lessonPath) {
        return telegramId + "_" + lessonPath;
>>>>>>> 5cc626e2d9bce6bd270d1d431747ddff1b1cdb50
    }
}