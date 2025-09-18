package com.tradinginfo.backend.service.telegram.impl;

import com.tradinginfo.backend.service.telegram.TelegramBotConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotConfigServiceImpl implements TelegramBotConfigService {

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.channel.id:}")
    private String channelId;

    @Value("${telegram.channel.name:}")
    private String channelName;

    @Value("${telegram.channel.url:}")
    private String channelUrl;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    @Override
    public Mono<Map<String, Object>> getBotInfo() {
        if (!isBotConfigured()) {
            return Mono.just(createErrorResponse("Bot not configured"));
        }

        return webClient.post()
                .uri(TELEGRAM_API_URL + botToken + "/getMe")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    log.debug("Bot info response: {}", response);

                    Boolean ok = (Boolean) response.get("ok");
                    if (!Boolean.TRUE.equals(ok)) {
                        return createErrorResponse("Failed to get bot info");
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.get("result");
                    if (result == null) {
                        return createErrorResponse("No bot info in response");
                    }

                    Map<String, Object> botInfo = new HashMap<>();
                    botInfo.put("configured", true);
                    botInfo.put("botId", result.get("id"));
                    botInfo.put("botUsername", result.get("username"));
                    botInfo.put("botFirstName", result.get("first_name"));
                    botInfo.put("canJoinGroups", result.get("can_join_groups"));
                    botInfo.put("canReadAllGroupMessages", result.get("can_read_all_group_messages"));
                    botInfo.put("supportsInlineQueries", result.get("supports_inline_queries"));

                    return botInfo;
                })
                .doOnError(error -> log.error("Error getting bot info: {}", error.getMessage()))
                .onErrorReturn(createErrorResponse("Failed to connect to Telegram API"));
    }

    @Override
    public boolean isBotConfigured() {
        boolean configured = botToken != null && !botToken.trim().isEmpty() &&
                           channelId != null && !channelId.trim().isEmpty();

        log.debug("Bot configuration check: {}", configured);
        return configured;
    }

    @Override
    public String getChannelInfo() {
        if (!isBotConfigured()) {
            return "Channel not configured";
        }

        StringBuilder info = new StringBuilder();
        info.append("Channel ID: ").append(channelId);

        if (channelName != null && !channelName.trim().isEmpty()) {
            info.append(", Name: ").append(channelName);
        }

        if (channelUrl != null && !channelUrl.trim().isEmpty()) {
            info.append(", URL: ").append(channelUrl);
        }

        return info.toString();
    }

    @Override
    public Map<String, Object> getBotConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("botConfigured", isBotConfigured());
        status.put("hasToken", botToken != null && !botToken.trim().isEmpty());
        status.put("hasChannelId", channelId != null && !channelId.trim().isEmpty());
        status.put("hasChannelName", channelName != null && !channelName.trim().isEmpty());
        status.put("hasChannelUrl", channelUrl != null && !channelUrl.trim().isEmpty());
        status.put("channelInfo", getChannelInfo());

        return status;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("configured", false);
        errorResponse.put("error", message);
        return errorResponse;
    }
}