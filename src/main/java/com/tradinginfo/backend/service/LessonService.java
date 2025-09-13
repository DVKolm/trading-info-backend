package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.dto.LessonStructureDTO;
import com.tradinginfo.backend.dto.ProgressDTO;
import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.entity.UserProgress;
import com.tradinginfo.backend.entity.AnalyticsEvent;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.repository.UserRepository;
import com.tradinginfo.backend.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;
    private final Optional<RedisCacheService> redisCacheService;

    @SuppressWarnings("unchecked")
    public List<String> getLessonFolders() {
        // Try to get from Redis cache first
        if (redisCacheService.isPresent()) {
            Object cached = redisCacheService.get().getCachedLessonFolders();
            if (cached instanceof List) {
                log.debug("📦 Retrieved lesson folders from Redis cache");
                return (List<String>) cached;
            }
        }

        // Get from database
        List<String> folders = lessonRepository.findAll().stream()
                .map(Lesson::getParentFolder)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Cache in Redis
        redisCacheService.ifPresent(cache -> cache.cacheLessonFolders(folders));

        return folders;
    }

    public List<LessonStructureDTO> getLessonStructure() {
        List<Lesson> allLessons = lessonRepository.findAll();
        Map<String, List<Lesson>> lessonsByFolder = allLessons.stream()
                .filter(l -> l.getParentFolder() != null)
                .collect(Collectors.groupingBy(Lesson::getParentFolder));

        return lessonsByFolder.entrySet().stream()
                .map(entry -> LessonStructureDTO.builder()
                        .name(entry.getKey())
                        .path(entry.getKey())
                        .lessons(entry.getValue().stream()
                                .sorted(Comparator.comparing(Lesson::getLessonNumber, Comparator.nullsLast(Integer::compareTo)))
                                .map(lesson -> LessonStructureDTO.LessonItemDTO.builder()
                                        .title(lesson.getTitle())
                                        .path(lesson.getPath())
                                        .lessonNumber(lesson.getLessonNumber())
                                        .wordCount(lesson.getWordCount())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .sorted(Comparator.comparing(LessonStructureDTO::getName))
                .collect(Collectors.toList());
    }

    public LessonDTO getLessonContent(String path, Long telegramId) {
        // Try to get from Redis cache first
        if (redisCacheService.isPresent()) {
            Object cached = redisCacheService.get().getCachedLessonContent(path);
            if (cached instanceof LessonDTO) {
                log.debug("📦 Retrieved lesson content from Redis cache: {}", path);
                if (telegramId != null) {
                    trackLessonOpen(telegramId, path);
                }
                return (LessonDTO) cached;
            }
        }

        // Get from database
        Lesson lesson = lessonRepository.findByPath(path)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        LessonDTO lessonDTO = convertToDTO(lesson);

        // Cache in Redis
        redisCacheService.ifPresent(cache -> cache.cacheLessonContent(path, lessonDTO));

        if (telegramId != null) {
            trackLessonOpen(telegramId, path);
        }

        return lessonDTO;
    }

    public String resolveLessonLink(String name) {
        List<Lesson> lessons = lessonRepository.findAll();
        return lessons.stream()
                .filter(l -> l.getTitle().toLowerCase().contains(name.toLowerCase()))
                .findFirst()
                .map(Lesson::getPath)
                .orElseThrow(() -> new RuntimeException("Link not found"));
    }

    public List<LessonDTO> searchLessons(String query) {
        return lessonRepository.searchLessons(query).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void updateProgress(Long telegramId, ProgressDTO progressData) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), progressData.getLessonPath())
                .orElseGet(() -> {
                    UserProgress newProgress = new UserProgress();
                    newProgress.setUser(user);
                    newProgress.setLessonPath(progressData.getLessonPath());
                    return newProgress;
                });

        progress.setTimeSpent(progress.getTimeSpent() + progressData.getTimeSpent());
        progress.setScrollProgress(progressData.getScrollProgress());
        progress.setReadingSpeed(progressData.getReadingSpeed());
        progress.setCompletionScore(progressData.getCompletionScore());
        progress.setEngagementLevel(progressData.getEngagementLevel());
        progress.setCompleted(progressData.getCompleted());
        progress.setLastVisited(LocalDateTime.now());
        progress.setVisits(progress.getVisits() + 1);

        userProgressRepository.save(progress);
    }

    public void trackAnalyticsEvent(Long telegramId, Map<String, Object> eventData) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AnalyticsEvent event = new AnalyticsEvent();
        event.setUser(user);
        event.setEventType((String) eventData.get("eventType"));
        event.setLessonPath((String) eventData.get("lessonPath"));
        event.setTimestamp(LocalDateTime.now());
        event.setData(eventData);
    }

    private void trackLessonOpen(Long telegramId, String lessonPath) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setTelegramId(telegramId);
                    return userRepository.save(newUser);
                });

        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);
    }

    private LessonDTO convertToDTO(Lesson lesson) {
        return LessonDTO.builder()
                .id(lesson.getId())
                .path(lesson.getPath())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .htmlContent(lesson.getHtmlContent())
                .frontmatter(lesson.getFrontmatter())
                .wordCount(lesson.getWordCount())
                .parentFolder(lesson.getParentFolder())
                .lessonNumber(lesson.getLessonNumber())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}