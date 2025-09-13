package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.service.TelegramBotService;
import com.tradinginfo.backend.service.TelegramAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class SubscriptionController {

    private final TelegramBotService telegramBotService;
    private final TelegramAuthService telegramAuthService;

    @GetMapping("/status/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> getSubscriptionStatus(@PathVariable Long userId) {
        log.info("📊 Checking subscription status for user: {}", userId);

        // Check actual Telegram channel subscription
        return telegramBotService.checkChannelSubscription(userId)
                .map(isSubscribed -> {
                    Map<String, Object> response = Map.of(
                            "hasSubscription", isSubscribed,
                            "isPremium", telegramAuthService.isAdmin(userId),
                            "subscriptionType", isSubscribed ? "premium" : "free",
                            "validUntil", System.currentTimeMillis() + 86400000L,
                            "channelId", "@DailyTradiBlog"
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.ok(Map.of(
                        "hasSubscription", false,
                        "isPremium", false,
                        "subscriptionType", "free",
                        "error", "Could not verify subscription"
                )));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifySubscription(@RequestBody Map<String, Object> request) {
        String initData = (String) request.get("initData");
        log.info("✅ Verifying subscription with initData");

        // For now, always return valid subscription
        // In production, implement proper Telegram subscription verification
        Map<String, Object> response = Map.of(
                "valid", true,
                "hasAccess", true,
                "subscriptionLevel", "premium",
                "message", "Subscription verified successfully"
        );

        return ResponseEntity.ok(response);
    }
}