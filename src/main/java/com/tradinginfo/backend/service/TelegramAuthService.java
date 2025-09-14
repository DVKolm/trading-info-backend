package com.tradinginfo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TelegramAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

    public Long extractTelegramUserId(String initData) {
        return Optional.ofNullable(initData)
                .filter(data -> !data.trim().isEmpty())
                .map(this::parseInitData)
                .map(params -> params.get("user"))
                .flatMap(this::extractUserIdFromJson)
                .orElseGet(() -> {
                    log.warn("Could not extract user ID from initData, using default");
                    return getDefaultUserId();
                });
    }

    public boolean validateInitData(String initData) {
        return Optional.ofNullable(initData)
                .filter(data -> !data.trim().isEmpty())
                .map(data -> data.contains("user") || data.length() > 10)
                .orElse(false);
    }

    private static final Set<Long> ADMIN_IDS = Set.of(
            781182099L,
            5974666109L
    );

    public boolean isAdmin(Long telegramUserId) {
        return Optional.ofNullable(telegramUserId)
                .map(ADMIN_IDS::contains)
                .orElse(false);
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new HashMap<String, String>();
        String[] pairs = initData.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to decode parameter: {}", pair);
                }
            }
        }

        return params;
    }

    private Optional<Long> extractUserIdFromJson(String userParam) {
        if (!userParam.contains("\"id\":")) {
            return Optional.empty();
        }

        try {
            int start = userParam.indexOf("\"id\":") + 5;
            int end = userParam.indexOf(",", start);
            if (end == -1) {
                end = userParam.indexOf("}", start);
            }

            String idStr = userParam.substring(start, end).trim();
            return Optional.of(Long.parseLong(idStr));
        } catch (Exception e) {
            log.warn("Failed to parse user ID from JSON: {}", userParam, e);
            return Optional.empty();
        }
    }

    private static final Long DEFAULT_USER_ID = 123456789L;

    private Long getDefaultUserId() {
        return DEFAULT_USER_ID;
    }
}