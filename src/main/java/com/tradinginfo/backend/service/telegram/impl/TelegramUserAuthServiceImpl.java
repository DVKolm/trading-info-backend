package com.tradinginfo.backend.service.telegram.impl;

import com.tradinginfo.backend.service.telegram.TelegramUserAuthService;
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
public class TelegramUserAuthServiceImpl implements TelegramUserAuthService {

    @Value("${app.admin.user-ids:}")
    private Set<Long> adminUserIds;

    @Override
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

    @Override
    public boolean validateInitData(String initData) {
        if (initData == null || initData.trim().isEmpty()) {
            log.debug("InitData is null or empty");
            return false;
        }

        try {
            Map<String, String> params = parseInitData(initData);

            // Basic validation - check if essential parameters are present
            if (!params.containsKey("user") || !params.containsKey("auth_date")) {
                log.debug("Essential parameters missing from initData");
                return false;
            }

            // TODO: Implement proper HMAC validation with bot token
            // For now, just check if user data is parseable
            Optional<Long> userId = extractUserIdFromJson(params.get("user"));
            if (userId.isEmpty()) {
                log.debug("Cannot extract valid user ID from initData");
                return false;
            }

            log.debug("InitData validation passed for user: {}", userId.get());
            return true;

        } catch (Exception e) {
            log.error("Error validating initData", e);
            return false;
        }
    }

    @Override
    public boolean isAdmin(Long telegramUserId) {
        if (telegramUserId == null) {
            return false;
        }

        log.info("Admin check for user {}: configured IDs = {}", telegramUserId, adminUserIds);
        boolean isAdmin = adminUserIds.contains(telegramUserId);
        log.info("Admin check result for user {}: {}", telegramUserId, isAdmin);
        return isAdmin;
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new HashMap<>();
        try {
            String[] pairs = initData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing initData", e);
        }
        return params;
    }

    private Optional<Long> extractUserIdFromJson(String userJson) {
        try {
            if (userJson != null && userJson.contains("\"id\":")) {
                String[] parts = userJson.split("\"id\":");
                if (parts.length > 1) {
                    String idPart = parts[1].split("[,}]")[0].trim();
                    return Optional.of(Long.parseLong(idPart));
                }
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from JSON: {}", userJson, e);
        }
        return Optional.empty();
    }

    private Long getDefaultUserId() {
        // Return a default user ID for development/testing
        return 123456789L;
    }
}