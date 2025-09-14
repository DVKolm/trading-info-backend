package com.tradinginfo.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.channel.id}")
    private String channelId;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    /**
     * Send message to Telegram channel
     */
    public void sendMessageToChannel(String message) {
        sendMessageToChannel(message, null);
    }

    /**
     * Send message to Telegram channel with parse mode
     */
    public void sendMessageToChannel(String message, String parseMode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", channelId);
            payload.put("text", message);
            if (parseMode != null) {
                payload.put("parse_mode", parseMode);
            }

            webClient.post()
                    .uri(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> {
                        log.info("📱 Message sent to Telegram channel: {}", channelId);
                        log.debug("Telegram API response: {}", response);
                    })
                    .doOnError(error -> {
                        log.error("❌ Failed to send message to Telegram channel: {}", error.getMessage());
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("❌ Error sending Telegram message", e);
        }
    }

    /**
     * Send direct message to user
     */
    public void sendMessageToUser(Long userId, String message) {
        sendMessageToUser(userId, message, null);
    }

    /**
     * Send direct message to user with parse mode
     */
    public void sendMessageToUser(Long userId, String message, String parseMode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", userId);
            payload.put("text", message);
            if (parseMode != null) {
                payload.put("parse_mode", parseMode);
            }

            webClient.post()
                    .uri(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> {
                        log.info("📱 Message sent to Telegram user: {}", userId);
                    })
                    .doOnError(error -> {
                        log.warn("⚠️ Failed to send message to user {}: {}", userId, error.getMessage());
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("❌ Error sending Telegram message to user {}", userId, e);
        }
    }

    /**
     * Check if user is subscribed to channel (synchronous)
     */
    public boolean checkChannelMembership(Long userId) {
        try {
            return checkChannelSubscription(userId).block();
        } catch (Exception e) {
            log.warn("Failed to check channel membership for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if user is subscribed to channel
     */
    public Mono<Boolean> checkChannelSubscription(Long userId) {
        return webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/getChatMember")
                .bodyValue(Map.of(
                        "chat_id", channelId,
                        "user_id", userId
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = (Map<String, Object>) response.get("result");
                    if (result != null) {
                        String status = (String) result.get("status");
                        boolean isSubscribed = !"left".equals(status) && !"kicked".equals(status);
                        log.debug("User {} subscription status: {} (subscribed: {})", userId, status, isSubscribed);
                        return isSubscribed;
                    }
                    return false;
                })
                .doOnError(error -> {
                    log.warn("⚠️ Failed to check subscription for user {}: {}", userId, error.getMessage());
                })
                .onErrorReturn(false);
    }

    /**
     * Get bot information
     */
    public Mono<Map<String, Object>> getBotInfo() {
        return webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/getMe")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(response -> {
                    log.debug("Bot info retrieved: {}", response);
                })
                .doOnError(error -> {
                    log.error("❌ Failed to get bot info: {}", error.getMessage());
                });
    }

    /**
     * Send lesson notification to channel
     */
    public void notifyNewLesson(String lessonTitle, String lessonPath) {
        String message = String.format(
                "🎓 *Новый урок доступен!*\n\n" +
                        "📖 %s\n\n" +
                        "Изучайте торговые стратегии вместе с H.E.A.R.T. Trading Academy!\n\n" +
                        "#урок #трейдинг #обучение",
                lessonTitle
        );

        sendMessageToChannel(message, "Markdown");
    }

    /**
     * Send lesson completion notification
     */
    public void notifyLessonCompletion(Long userId, String lessonTitle) {
        String message = String.format(
                "🎉 Поздравляем!\n\n" +
                        "Вы успешно завершили урок:\n" +
                        "📖 *%s*\n\n" +
                        "Продолжайте обучение в H.E.A.R.T. Trading Academy!",
                lessonTitle
        );

        sendMessageToUser(userId, message, "Markdown");
    }

    /**
     * Send welcome message to new user
     */
    public void sendWelcomeMessage(Long userId, String firstName) {
        String message = String.format(
                "👋 Добро пожаловать, %s!\n\n" +
                        "Спасибо за подписку на канал @DailyTradiBlog!\n\n" +
                        "🎓 Начните изучение торговых стратегий в нашей академии.\n" +
                        "📈 Получайте ежедневные аналитические обзоры.\n" +
                        "💡 Присоединяйтесь к сообществу профессиональных трейдеров.\n\n" +
                        "Удачных торгов! 🚀",
                firstName != null ? firstName : "трейдер"
        );

        sendMessageToUser(userId, message);
    }

    /**
     * Send admin notification about new lesson upload
     */
    public void notifyAdminLessonUploaded(String lessonTitle, String uploaderInfo) {
        String message = String.format(
                "📋 *Администрирование*\n\n" +
                        "✅ Загружен новый урок:\n" +
                        "📖 %s\n\n" +
                        "👤 Загрузил: %s\n" +
                        "🕐 Время: %s",
                lessonTitle,
                uploaderInfo,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        // Send to admin users
        sendMessageToUser(781182099L, message, "Markdown");
        sendMessageToUser(5974666109L, message, "Markdown");
    }

    /**
     * Check if bot token is valid
     */
    public boolean isBotConfigured() {
        return botToken != null &&
               !botToken.isEmpty() &&
               !botToken.equals("your_bot_token_here") &&
               botToken.contains(":");
    }

    /**
     * Get channel info
     */
    public String getChannelInfo() {
        return String.format("Channel: %s | Bot configured: %s",
                channelId, isBotConfigured());
    }
}