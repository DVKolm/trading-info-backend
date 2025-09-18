package com.tradinginfo.backend.service.subscription.impl;

import com.tradinginfo.backend.entity.User;
import com.tradinginfo.backend.repository.UserRepository;
import com.tradinginfo.backend.service.subscription.SubscriptionAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessServiceImpl implements SubscriptionAccessService {

    private final UserRepository userRepository;

    @Override
    public boolean isPremiumLesson(String lessonPath) {
        if (lessonPath == null || lessonPath.isEmpty()) {
            return false;
        }

        // Логика определения премиум-уроков
        // Например, уроки в папке "premium" или с определенным префиксом
        return lessonPath.toLowerCase().contains("premium") ||
               lessonPath.toLowerCase().contains("advanced") ||
               lessonPath.toLowerCase().startsWith("pro/");
    }

    @Override
    public boolean hasAccessToLesson(Long telegramId, String lessonPath) {
        if (!isPremiumLesson(lessonPath)) {
            return true; // Свободный доступ к обычным урокам
        }

        if (telegramId == null) {
            return false; // Неавторизованный пользователь не имеет доступа к премиум-урокам
        }

        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isEmpty()) {
            return false;
        }

        User userEntity = user.get();

        // Проверяем активную подписку
        if (userEntity.getSubscriptionExpiresAt() != null && userEntity.getSubscriptionExpiresAt().isAfter(LocalDateTime.now())) {
            return true;
        }

        // Проверяем флаг премиум доступа
        if (Boolean.TRUE.equals(userEntity.getPremiumAccess())) {
            return true;
        }

        log.debug("User {} does not have access to premium lesson: {}", telegramId, lessonPath);
        return false;
    }
}