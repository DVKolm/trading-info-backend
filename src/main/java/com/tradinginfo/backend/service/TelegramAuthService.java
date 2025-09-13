package com.tradinginfo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j
public class TelegramAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

    /**
     * Extract user ID from Telegram WebApp initData
     * This is a simplified implementation for development
     */
    public Long extractTelegramUserId(String initData) {
        try {
            if (initData == null || initData.isEmpty()) {
                return getDefaultUserId();
            }

            // Parse initData parameters
            Map<String, String> params = parseInitData(initData);

            // Try to extract user data
            String userParam = params.get("user");
            if (userParam != null) {
                // Parse user JSON (simplified)
                if (userParam.contains("\"id\":")) {
                    int start = userParam.indexOf("\"id\":") + 5;
                    int end = userParam.indexOf(",", start);
                    if (end == -1) end = userParam.indexOf("}", start);

                    String idStr = userParam.substring(start, end).trim();
                    return Long.parseLong(idStr);
                }
            }

            log.warn("Could not extract user ID from initData, using default");
            return getDefaultUserId();

        } catch (Exception e) {
            log.error("Failed to parse Telegram initData", e);
            return getDefaultUserId();
        }
    }

    /**
     * Validate Telegram WebApp initData
     * For development, this is simplified
     */
    public boolean validateInitData(String initData) {
        if (initData == null || initData.isEmpty()) {
            return false;
        }

        try {
            // In production, implement proper HMAC validation
            // For now, just check that it contains basic user data
            return initData.contains("user") || initData.length() > 10;
        } catch (Exception e) {
            log.error("Failed to validate initData", e);
            return false;
        }
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(Long telegramUserId) {
        // Hardcoded admin IDs for now
        // In production, store this in database
        return telegramUserId != null && (
                telegramUserId.equals(781182099L) ||
                telegramUserId.equals(5974666109L)
        );
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new HashMap<>();

        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to decode parameter: {}", pair);
                }
            }
        }

        return params;
    }

    private Long getDefaultUserId() {
        return 123456789L; // Default user ID for development
    }
}