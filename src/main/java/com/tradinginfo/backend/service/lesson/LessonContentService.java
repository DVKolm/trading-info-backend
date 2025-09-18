package com.tradinginfo.backend.service.lesson;

import com.tradinginfo.backend.dto.LessonDTO;

public interface LessonContentService {
    LessonDTO getLessonContent(String path, Long telegramId);
    String resolveLessonLink(String name);
}