package com.tradinginfo.backend.service.lesson.impl;

import com.tradinginfo.backend.dto.FolderDTO;
import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.repository.LessonRepository;
// import com.tradinginfo.backend.service.infrastructure.RedisCacheService; // REMOVED
import com.tradinginfo.backend.service.lesson.LessonFolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonFolderServiceImpl implements LessonFolderService {

    private final LessonRepository lessonRepository;
    // private final Optional<RedisCacheService> redisCacheService; // REMOVED

    @Value("${app.lessons.default-category}")
    private String defaultCategory;

    private static final Map<String, Integer> LEVEL_ORDER = Map.of(
            "Начальный уровень (Бесплатно)", 1,
            "Средний уровень (Подписка)", 2,
            "Продвинутый уровень (Подписка)", 3,
            "Эксперт уровень (Подписка)", 4
    );

    @Override
    public List<FolderDTO> getLessonFolders() {
        return loadLessonFolders(); // Direct call - no cache
    }

    private List<FolderDTO> loadLessonFolders() {
        log.debug("Loading lesson folders from database");

        // Get folders from lessons with parentFolder (existing logic)
        List<String> folderNamesFromParent = lessonRepository.findAll().stream()
                .map(this::extractLevel)
                .filter(Objects::nonNull)
                .filter(name -> !defaultCategory.equals(name))
                .distinct()
                .toList();

        // Get folders that are marked as isFolder = true
        List<String> folderNamesFromIsFolder = lessonRepository.findAll().stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsFolder()))
                .map(Lesson::getTitle)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Combine both lists and remove duplicates
        List<String> allFolderNames = Stream.concat(
                folderNamesFromParent.stream(),
                folderNamesFromIsFolder.stream()
        )
                .distinct()
                .sorted(this::sortFoldersByLevel)
                .toList();

        // Add default category if there are lessons without a parent folder and not in any isFolder
        boolean hasOrphanLessons = lessonRepository.findAll().stream()
                .anyMatch(lesson -> lesson.getParentFolder() == null &&
                         !Boolean.TRUE.equals(lesson.getIsFolder()));

        if (hasOrphanLessons) {
            allFolderNames.add(defaultCategory);
        }

        List<FolderDTO> folders = allFolderNames.stream()
                .map(folderName -> FolderDTO.create(folderName, folderName))
                .toList();

        // No caching - return directly
        return folders;
    }

    private String extractLevel(Lesson lesson) {
        String parentFolder = lesson.getParentFolder();

        if (parentFolder == null) {
            return defaultCategory;
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

    private int sortFoldersByLevel(String a, String b) {
        Integer orderA = LEVEL_ORDER.getOrDefault(a, 999);
        Integer orderB = LEVEL_ORDER.getOrDefault(b, 999);

        if (orderA.equals(orderB)) {
            return a.compareTo(b);
        }

        return Integer.compare(orderA, orderB);
    }
}