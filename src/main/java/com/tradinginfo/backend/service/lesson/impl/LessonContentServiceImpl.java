package com.tradinginfo.backend.service.lesson.impl;

import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.mapper.LessonMapper;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.repository.UserRepository;
// import com.tradinginfo.backend.service.infrastructure.RedisCacheService; // REMOVED
import com.tradinginfo.backend.service.analytics.UserAnalyticsService;
import com.tradinginfo.backend.service.lesson.LessonContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LessonContentServiceImpl implements LessonContentService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    // private final Optional<RedisCacheService> redisCacheService; // REMOVED
    private final LessonMapper lessonMapper;
    private final UserAnalyticsService userAnalyticsService;

    @Override
    // @Cacheable(value = "lessons", key = "#path", unless = "#result == null") - DISABLED
    public LessonDTO getLessonContent(String path, Long telegramId) {
        // Normalize path: remove leading slash if present
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // No caching - direct database access

        Lesson lesson = lessonRepository.findByPath(normalizedPath)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + normalizedPath));

        LessonDTO lessonDTO = lessonMapper.toDTO(lesson);
        // No caching
        Optional.ofNullable(telegramId).ifPresent(id -> userAnalyticsService.trackLessonAccess(id, normalizedPath));

        return lessonDTO;
    }

    @Override
    public String resolveLessonLink(String name) {
        List<Lesson> lessons = lessonRepository.findAll();
        String lowerCaseName = name.toLowerCase();

        return lessons.stream()
                .filter(lesson -> lesson.getTitle().toLowerCase().contains(lowerCaseName))
                .findFirst()
                .map(Lesson::getPath)
                .orElseThrow(() -> new IllegalArgumentException("Link not found: " + name));
    }

}