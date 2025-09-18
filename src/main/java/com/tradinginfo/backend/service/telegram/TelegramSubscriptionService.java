package com.tradinginfo.backend.service.telegram;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TelegramSubscriptionService {
    Mono<Boolean> checkChannelSubscription(Long userId);
    boolean checkChannelMembership(Long telegramId);
    Map<String, Object> createSubscriptionCheckResponse(Long userId, boolean subscribed, String error);
}