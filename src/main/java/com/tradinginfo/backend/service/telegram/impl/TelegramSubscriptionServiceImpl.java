package com.tradinginfo.backend.service.telegram.impl;

import com.tradinginfo.backend.service.telegram.TelegramSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramSubscriptionServiceImpl implements TelegramSubscriptionService {

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.channel.id:}")
    private String channelId;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    @Override
    public Mono<Boolean> checkChannelSubscription(Long userId) {
        if (!isBotConfigured()) {
            log.warn("Bot not configured, returning false for subscription check");
            return Mono.just(false);
        }

        return webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/getChatMember")
                .bodyValue(Map.of(
                        "chat_id", channelId,
                        "user_id", userId
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    log.debug("Telegram API response for user {}: {}", userId, response);

                    Boolean ok = (Boolean) response.get("ok");
                    if (!Boolean.TRUE.equals(ok)) {
                        log.warn("Telegram API returned ok=false for user {}", userId);
                        return false;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.get("result");
                    if (result == null) {
                        log.warn("No result in Telegram API response for user {}", userId);
                        return false;
                    }

                    String status = (String) result.get("status");
                    boolean isSubscribed = "creator".equals(status) ||
                                         "administrator".equals(status) ||
                                         "member".equals(status);

                    log.info("User {} subscription status: {} (subscribed: {})", userId, status, isSubscribed);
                    return isSubscribed;
                })
                .doOnError(error -> log.error("Error checking subscription for user {}: {}", userId, error.getMessage()))
                .onErrorReturn(false);
    }

    @Override
    public boolean checkChannelMembership(Long telegramId) {
        try {
            return checkChannelSubscription(telegramId).block();
        } catch (Exception e) {
            log.error("Error checking channel membership for user {}", telegramId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> createSubscriptionCheckResponse(Long userId, boolean subscribed, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("subscribed", subscribed);
        response.put("timestamp", LocalDateTime.now());

        if (error != null) {
            response.put("error", error);
        }

        if (subscribed) {
            response.put("message", "User is subscribed to the channel");
            response.put("status", "active");
        } else {
            response.put("message", "User is not subscribed to the channel");
            response.put("status", "inactive");
        }

        return response;
    }

    private boolean isBotConfigured() {
        return botToken != null && !botToken.trim().isEmpty() &&
               channelId != null && !channelId.trim().isEmpty();
    }
}