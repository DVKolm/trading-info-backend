package com.tradinginfo.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "spring.data.redis.host", matchIfMissing = false)
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Lesson content cache
    private static final String LESSON_CONTENT_PREFIX = "lesson:content:";
    private static final String LESSON_STRUCTURE_KEY = "lesson:structure";
    private static final String LESSON_FOLDERS_KEY = "lesson:folders";
    private static final String USER_PROGRESS_PREFIX = "user:progress:";

    // Cache TTL (10 minutes)
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    // Subscription cache prefix
    private static final String SUBSCRIPTION_STATUS_PREFIX = "subscription:status:";

    /**
     * Cache lesson content
     */
    public void cacheLessonContent(String lessonPath, Object content) {
        try {
            String key = LESSON_CONTENT_PREFIX + lessonPath;
            redisTemplate.opsForValue().set(key, content, CACHE_TTL);
            log.debug("📦 Cached lesson content: {}", lessonPath);
        } catch (Exception e) {
            log.warn("Failed to cache lesson content: {}", lessonPath, e);
        }
    }

    /**
     * Get cached lesson content
     */
    public Object getCachedLessonContent(String lessonPath) {
        try {
            String key = LESSON_CONTENT_PREFIX + lessonPath;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cached lesson content: {}", lessonPath, e);
            return null;
        }
    }

    /**
     * Cache lesson structure
     */
    public void cacheLessonStructure(Object structure) {
        try {
            redisTemplate.opsForValue().set(LESSON_STRUCTURE_KEY, structure, CACHE_TTL);
            log.debug("📦 Cached lesson structure");
        } catch (Exception e) {
            log.warn("Failed to cache lesson structure", e);
        }
    }

    /**
     * Get cached lesson structure
     */
    public Object getCachedLessonStructure() {
        try {
            return redisTemplate.opsForValue().get(LESSON_STRUCTURE_KEY);
        } catch (Exception e) {
            log.warn("Failed to get cached lesson structure", e);
            return null;
        }
    }

    /**
     * Cache lesson folders
     */
    public void cacheLessonFolders(Object folders) {
        try {
            redisTemplate.opsForValue().set(LESSON_FOLDERS_KEY, folders, CACHE_TTL);
            log.debug("📦 Cached lesson folders");
        } catch (Exception e) {
            log.warn("Failed to cache lesson folders", e);
        }
    }

    /**
     * Get cached lesson folders
     */
    public Object getCachedLessonFolders() {
        try {
            return redisTemplate.opsForValue().get(LESSON_FOLDERS_KEY);
        } catch (Exception e) {
            log.warn("Failed to get cached lesson folders", e);
            return null;
        }
    }

    /**
     * Cache user progress
     */
    public void cacheUserProgress(Long userId, String lessonPath, Object progress) {
        try {
            String key = USER_PROGRESS_PREFIX + userId + ":" + lessonPath;
            redisTemplate.opsForValue().set(key, progress, Duration.ofHours(24));
            log.debug("📦 Cached user progress: {} - {}", userId, lessonPath);
        } catch (Exception e) {
            log.warn("Failed to cache user progress: {} - {}", userId, lessonPath, e);
        }
    }

    /**
     * Get cached user progress
     */
    public Object getCachedUserProgress(Long userId, String lessonPath) {
        try {
            String key = USER_PROGRESS_PREFIX + userId + ":" + lessonPath;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cached user progress: {} - {}", userId, lessonPath, e);
            return null;
        }
    }

    /**
     * Clear all lesson-related cache
     */
    public void clearLessonCache() {
        try {
            // Clear lesson content cache
            Set<String> lessonContentKeys = redisTemplate.keys(LESSON_CONTENT_PREFIX + "*");
            if (lessonContentKeys != null && !lessonContentKeys.isEmpty()) {
                redisTemplate.delete(lessonContentKeys);
                log.info("🧹 Cleared {} lesson content cache entries", lessonContentKeys.size());
            }

            // Clear lesson structure and folders
            redisTemplate.delete(LESSON_STRUCTURE_KEY);
            redisTemplate.delete(LESSON_FOLDERS_KEY);

            log.info("🧹 Cleared lesson cache successfully");
        } catch (Exception e) {
            log.warn("Failed to clear lesson cache", e);
        }
    }

    /**
     * Clear user progress cache
     */
    public void clearUserProgressCache(Long userId) {
        try {
            String pattern = USER_PROGRESS_PREFIX + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("🧹 Cleared {} user progress cache entries for user {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Failed to clear user progress cache for user {}", userId, e);
        }
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        try {
            Set<String> allKeys = redisTemplate.keys("lesson:*");
            if (allKeys != null && !allKeys.isEmpty()) {
                redisTemplate.delete(allKeys);
            }

            Set<String> userKeys = redisTemplate.keys("user:*");
            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
            }

            log.info("🧹 Cleared all cache successfully");
        } catch (Exception e) {
            log.warn("Failed to clear all cache", e);
        }
    }

    /**
     * Cache subscription status
     */
    public void cacheSubscriptionStatus(Long telegramId, Object status, long durationMinutes) {
        try {
            String key = SUBSCRIPTION_STATUS_PREFIX + telegramId;
            redisTemplate.opsForValue().set(key, status, Duration.ofMinutes(durationMinutes));
            log.debug("📦 Cached subscription status for user {}", telegramId);
        } catch (Exception e) {
            log.warn("Failed to cache subscription status for user {}", telegramId, e);
        }
    }

    /**
     * Get cached subscription status
     */
    public Object getCachedSubscriptionStatus(Long telegramId) {
        try {
            String key = SUBSCRIPTION_STATUS_PREFIX + telegramId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cached subscription status for user {}", telegramId, e);
            return null;
        }
    }

    /**
     * Invalidate subscription status cache
     */
    public void invalidateSubscriptionStatus(Long telegramId) {
        try {
            String key = SUBSCRIPTION_STATUS_PREFIX + telegramId;
            redisTemplate.delete(key);
            log.debug("🗑️ Invalidated subscription status cache for user {}", telegramId);
        } catch (Exception e) {
            log.warn("Failed to invalidate subscription status cache for user {}", telegramId, e);
        }
    }

    /**
     * Check if Redis is connected
     */
    public boolean isConnected() {
        try {
            redisTemplate.opsForValue().get("test-connection");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}