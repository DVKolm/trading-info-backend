package com.tradinginfo.backend.service.lesson;

import com.tradinginfo.backend.dto.LessonDTO;

import java.util.List;

public interface LessonSearchService {
    List<LessonDTO> searchLessons(String query);
}