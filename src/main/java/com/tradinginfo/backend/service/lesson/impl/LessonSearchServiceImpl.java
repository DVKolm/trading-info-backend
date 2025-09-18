package com.tradinginfo.backend.service.lesson.impl;

import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.mapper.LessonMapper;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.service.lesson.LessonSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonSearchServiceImpl implements LessonSearchService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;

    @Override
    public List<LessonDTO> searchLessons(String query) {
        return lessonRepository.searchLessons(query).stream()
                .map(lessonMapper::toDTO)
                .toList();
    }
}