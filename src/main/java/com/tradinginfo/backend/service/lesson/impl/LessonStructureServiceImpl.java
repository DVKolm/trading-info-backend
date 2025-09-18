package com.tradinginfo.backend.service.lesson.impl;

import com.tradinginfo.backend.dto.LessonStructureDTO;
import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.service.lesson.LessonStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LessonStructureServiceImpl implements LessonStructureService {

    private final LessonRepository lessonRepository;

    @Value("${app.lessons.default-category}")
    private String defaultCategory;

    private static final Map<String, Integer> LEVEL_ORDER = Map.of(
            "Начальный уровень (Бесплатно)", 1,
            "Средний уровень (Подписка)", 2,
            "Продвинутый уровень (Подписка)", 3,
            "Эксперт уровень (Подписка)", 4
    );

    @Override
    // @Cacheable(value = "lessonStructure", unless = "#result.isEmpty()") - DISABLED
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
}