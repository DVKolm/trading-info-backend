package com.tradinginfo.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private static final Set<Long> ADMIN_USER_IDS = Set.of(781182099L, 5974666109L);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public void sendMessageToUser(Long userId, String message) {
        sendMessageToUser(userId, message, null);
    }

    public void sendMessageToUser(Long userId, String message, String parseMode) {
        try {
            Map<String, Object> payload = createMessagePayload(userId.toString(), message, parseMode);
            sendTelegramMessage(payload, "user " + userId);
        } catch (Exception e) {
            log.error("Error sending Telegram message to user {}", userId, e);
        }
    }

    public Mono<Boolean> checkChannelSubscription(Long userId) {
        return webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/getChatMember")
                .bodyValue(Map.of(
                        "chat_id", channelId,
                        "user_id", userId
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractSubscriptionStatus)
                .doOnError(error ->
                        log.warn("Failed to check subscription for user {}: {}", userId, error.getMessage()))
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

    private Map<String, Object> createMessagePayload(String chatId, String message, String parseMode) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("chat_id", chatId);
        payload.put("text", message);
        Optional.ofNullable(parseMode).ifPresent(mode -> payload.put("parse_mode", mode));
        return payload;
    }

    private void sendTelegramMessage(Map<String, Object> payload, String target) {
        webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/sendMessage")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> log.info("Message sent to Telegram {}", target))
                .doOnError(error -> log.error("Failed to send message to {}: {}", target, error.getMessage()))
                .subscribe();
    }

    /**
     * Get bot configuration status for API endpoints
     */
    public Map<String, Object> getBotConfigurationStatus() {
        if (!isBotConfigured()) {
            return Map.<String, Object>of(
                    "configured", false,
                    "message", "Bot token not configured"
            );
        }

        return Map.<String, Object>of(
                "configured", true,
                "channel", getChannelInfo()
        );
    }


    /**
     * Process lesson completion notification with validation
     */
    public Map<String, String> processLessonCompletionNotification(long userId, String lessonTitle) {
        return Optional.ofNullable(lessonTitle)
                .filter(title -> !title.trim().isEmpty())
                .map(title -> {
                    notifyLessonCompletion(userId, title);
                    return Map.of(
                            "message", "Completion notification sent",
                            "userId", String.valueOf(userId)
                    );
                })
                .orElse(Map.of("error", "Lesson title is required"));
    }

    /**
     * Create subscription check response
     */
    public Map<String, Object> createSubscriptionCheckResponse(Long userId, boolean subscribed, String error) {
        if (error != null) {
            return Map.<String, Object>of(
                    "userId", userId,
                    "subscribed", subscribed,
                    "channelId", "@DailyTradiBlog",
                    "error", error
            );
        }

        return Map.<String, Object>of(
                "userId", userId,
                "subscribed", subscribed,
                "channelId", "@DailyTradiBlog"
        );
    }

    /**
     * Process message sending with target routing
     */
    public Map<String, String> processMessageSending(String message, String target) {
        return Optional.ofNullable(message)
                .filter(msg -> !msg.trim().isEmpty())
                .map(msg -> sendMessageToTarget(msg, target))
                .orElse(Map.of("error", "Message is required"));
    }

    /**
     * Send message to target with validation and routing
     */
    private Map<String, String> sendMessageToTarget(String message, String target) {
        if ("channel".equals(target)) {
            return Map.of("error", "Channel notifications disabled");
        }

        try {
            Long userId = Long.parseLong(target);
            sendMessageToUser(userId, message);
            return Map.of("message", "Message sent to user " + userId);
        } catch (NumberFormatException e) {
            return Map.of("error", "Invalid target");
        }
    }

    /**
     * Check if user is subscribed to the channel
     */
    public boolean checkChannelMembership(Long telegramId) {
        if (!isBotConfigured()) {
            log.warn("Bot not configured, cannot check channel membership");
            return false;
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(TELEGRAM_API_URL + botToken + "/getChatMember?chat_id=" + channelId + "&user_id=" + telegramId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return extractSubscriptionStatus(response);
        } catch (Exception e) {
            log.error("Error checking channel membership for user {}: {}", telegramId, e.getMessage());
            return false;
        }
    }

    private boolean extractSubscriptionStatus(Map<String, Object> response) {
        return Optional.ofNullable(response.get("result"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(result -> (String) result.get("status"))
                .map(status -> !"left".equals(status) && !"kicked".equals(status))
                .orElse(false);
    }
}