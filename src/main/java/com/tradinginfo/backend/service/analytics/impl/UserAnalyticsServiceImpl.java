package com.tradinginfo.backend.service.analytics.impl;

import com.tradinginfo.backend.service.analytics.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    @Override
    public void trackUserLogin(Long telegramId) {
        log.info("User login tracked: telegramId={} at {}", telegramId, LocalDateTime.now());
        // Здесь можно добавить логику сохранения в базу данных или отправки в систему аналитики
    }

    @Override
    public void trackLessonAccess(Long telegramId, String lessonPath) {
        log.info("Lesson access tracked: telegramId={}, lesson={} at {}", telegramId, lessonPath, LocalDateTime.now());
        // Здесь можно добавить логику сохранения в базу данных или отправки в систему аналитики
    }
}