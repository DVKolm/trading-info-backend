package com.tradinginfo.backend.service;

import com.tradinginfo.backend.dto.FolderDTO;
import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.dto.LessonStructureDTO;
import com.tradinginfo.backend.dto.ProgressDTO;
import com.tradinginfo.backend.entity.AnalyticsEvent;
import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.entity.UserProgress;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.repository.UserProgressRepository;
import com.tradinginfo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public List<FolderDTO> getLessonFolders() {
        return redisCacheService
                .map(cache -> (List<FolderDTO>) cache.getCachedLessonFolders())
                .filter(Objects::nonNull)
                .orElseGet(this::loadAndCacheLessonFolders);
    }

    private List<FolderDTO> loadAndCacheLessonFolders() {
        log.debug("Loading lesson folders from database");

        // Get folders from lessons with parentFolder (existing logic)
        List<String> folderNamesFromParent = lessonRepository.findAll().stream()
                .map(this::extractLevel)
                .filter(Objects::nonNull)
                .filter(name -> !"Без категории".equals(name))
                .distinct()
                .collect(Collectors.toList());

        // Get folders that are marked as isFolder = true
        List<String> folderNamesFromIsFolder = lessonRepository.findAll().stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsFolder()))
                .map(Lesson::getTitle)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Combine both lists and remove duplicates
        List<String> allFolderNames = Stream.concat(
                folderNamesFromParent.stream(),
                folderNamesFromIsFolder.stream()
        )
                .distinct()
                .sorted(this::sortFoldersByLevel)
                .collect(Collectors.toList());

        // Add "Без категории" if there are lessons without a parent folder and not in any isFolder
        boolean hasOrphanLessons = lessonRepository.findAll().stream()
                .anyMatch(lesson -> lesson.getParentFolder() == null &&
                         !Boolean.TRUE.equals(lesson.getIsFolder()));

        if (hasOrphanLessons) {
            allFolderNames.add("Без категории");
        }

        List<FolderDTO> folders = allFolderNames.stream()
                .map(folderName -> FolderDTO.create(folderName, folderName))
                .collect(Collectors.toList());

        redisCacheService.ifPresent(cache -> cache.cacheLessonFolders(folders));
        return folders;
    }

    private static final Map<String, Integer> LEVEL_ORDER = Map.of(
            "Начальный уровень (Бесплатно)", 1,
            "Средний уровень (Подписка)", 2,
            "Продвинутый уровень (Подписка)", 3,
            "Эксперт уровень (Подписка)", 4
    );

    private int sortFoldersByLevel(String a, String b) {
        Integer orderA = LEVEL_ORDER.getOrDefault(a, 999);
        Integer orderB = LEVEL_ORDER.getOrDefault(b, 999);

        if (orderA.equals(orderB)) {
            return a.compareTo(b);
        }

        return Integer.compare(orderA, orderB);
    }

    public List<LessonStructureDTO> getLessonStructure() {
        List<Lesson> allLessons = lessonRepository.findAll();

        Map<String, List<Lesson>> lessonsByLevel = allLessons.stream()
                .filter(lesson -> lesson.getParentFolder() != null)
                .collect(Collectors.groupingBy(this::extractLevel));

        return lessonsByLevel.entrySet().stream()
                .map(this::createLevelStructure)
                .sorted(this::sortLevelsByOrder)
                .collect(Collectors.toList());
    }

    private LessonStructureDTO createLevelStructure(Map.Entry<String, List<Lesson>> entry) {
        String levelName = entry.getKey();
        String levelId = generateId(levelName, "level");

        List<LessonStructureDTO> children = entry.getValue().stream()
                .sorted(Comparator.comparing(Lesson::getLessonNumber, Comparator.nullsLast(Integer::compareTo)))
                .map(this::createLessonStructure)
                .collect(Collectors.toList());

        return LessonStructureDTO.createFolder(levelId, levelName, levelName, children);
    }

    private LessonStructureDTO createLessonStructure(Lesson lesson) {
        String lessonId = generateId(lesson.getPath(), "lesson");
        String filename = extractFilename(lesson.getPath());

        return LessonStructureDTO.createFile(
                lessonId,
                lesson.getTitle(),
                lesson.getPath(),
                filename
        );
    }

    private String extractLevel(Lesson lesson) {
        String parentFolder = lesson.getParentFolder();

        if (parentFolder == null) {
            return "Без категории";
        }

        if (parentFolder.startsWith("Начальный уровень")) {
            return "Начальный уровень (Бесплатно)";
        } else if (parentFolder.startsWith("Средний уровень")) {
            return "Средний уровень (Подписка)";
        } else if (parentFolder.startsWith("Продвинутый уровень")) {
            return "Продвинутый уровень (Подписка)";
        } else if (parentFolder.startsWith("Эксперт")) {
            return "Эксперт уровень (Подписка)";
        }

        String[] parts = parentFolder.split("/");
        return parts[0];
    }

    private int sortLevelsByOrder(LessonStructureDTO a, LessonStructureDTO b) {
        Integer orderA = LEVEL_ORDER.getOrDefault(a.name(), 999);
        Integer orderB = LEVEL_ORDER.getOrDefault(b.name(), 999);

        if (orderA.equals(orderB)) {
            return a.name().compareTo(b.name());
        }

        return Integer.compare(orderA, orderB);
    }

    private String generateId(String input, String type) {
        String safeInput = input.replaceAll("[^a-zA-Z0-9]", "_");
        return type + "_" + safeInput.toLowerCase() + "_" + Math.abs(input.hashCode() % 10000);
    }

    private String extractFilename(String path) {
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown.md";
    }

    public LessonDTO getLessonContent(String path, Long telegramId) {
        // Normalize path: remove leading slash if present
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        Optional<LessonDTO> cachedLesson = redisCacheService
                .map(cache -> cache.getCachedLessonContent(normalizedPath))
                .filter(LessonDTO.class::isInstance)
                .map(LessonDTO.class::cast);

        if (cachedLesson.isPresent()) {
            log.debug("Retrieved lesson content from Redis cache: {}", normalizedPath);
            Optional.ofNullable(telegramId).ifPresent(id -> trackLessonOpen(id, normalizedPath));
            return cachedLesson.get();
        }

        Lesson lesson = lessonRepository.findByPath(normalizedPath)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + normalizedPath));

        LessonDTO lessonDTO = convertToDTO(lesson);
        redisCacheService.ifPresent(cache -> cache.cacheLessonContent(normalizedPath, lessonDTO));
        Optional.ofNullable(telegramId).ifPresent(id -> trackLessonOpen(id, normalizedPath));

        return lessonDTO;
    }

    public String resolveLessonLink(String name) {
        List<Lesson> lessons = lessonRepository.findAll();
        String lowerCaseName = name.toLowerCase();

        return lessons.stream()
                .filter(lesson -> lesson.getTitle().toLowerCase().contains(lowerCaseName))
                .findFirst()
                .map(Lesson::getPath)
                .orElseThrow(() -> new IllegalArgumentException("Link not found: " + name));
    }

    public List<LessonDTO> searchLessons(String query) {
        return lessonRepository.searchLessons(query).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void updateProgress(Long telegramId, ProgressDTO progressData) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + telegramId));

        UserProgress progress = userProgressRepository.findByUserIdAndLessonPath(user.getId(), progressData.lessonPath())
                .orElseGet(() -> createNewUserProgress(user, progressData.lessonPath()));

        updateProgressData(progress, progressData);
        userProgressRepository.save(progress);
    }

    public void trackAnalyticsEvent(Long telegramId, Map<String, Object> eventData) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + telegramId));

        AnalyticsEvent event = new AnalyticsEvent();
        event.setUser(user);
        event.setEventType((String) eventData.get("eventType"));
        event.setLessonPath((String) eventData.get("lessonPath"));
        event.setTimestamp(LocalDateTime.now());
        event.setData(eventData);
    }

    private UserProgress createNewUserProgress(User user, String lessonPath) {
        UserProgress newProgress = new UserProgress();
        newProgress.setUser(user);
        newProgress.setLessonPath(lessonPath);
        return newProgress;
    }

    private void updateProgressData(UserProgress progress, ProgressDTO progressData) {
        progress.setTimeSpent(progress.getTimeSpent() + progressData.timeSpent());
        progress.setScrollProgress(progressData.scrollProgress());
        progress.setReadingSpeed(progressData.readingSpeed());
        progress.setCompletionScore(progressData.completionScore());
        progress.setEngagementLevel(progressData.engagementLevel());
        progress.setCompleted(progressData.completed());
        progress.setLastVisited(LocalDateTime.now());
        progress.setVisits(progress.getVisits() + 1);
    }

    private void trackLessonOpen(Long telegramId, String lessonPath) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createNewUser(telegramId));

        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);
    }

    private User createNewUser(Long telegramId) {
        User newUser = new User();
        newUser.setTelegramId(telegramId);
        return userRepository.save(newUser);
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