package com.tradinginfo.backend.service.telegram;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TelegramBotConfigService {
    Mono<Map<String, Object>> getBotInfo();
    boolean isBotConfigured();
    String getChannelInfo();
    Map<String, Object> getBotConfigurationStatus();
}