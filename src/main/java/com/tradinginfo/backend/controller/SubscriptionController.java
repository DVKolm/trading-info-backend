package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.service.TelegramBotService;
import com.tradinginfo.backend.service.TelegramAuthService;
import com.tradinginfo.backend.service.SubscriptionService;
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
    private final SubscriptionService subscriptionService;

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
        Long telegramId = (Long) request.get("telegramId");

        log.info("✅ Verifying subscription with initData for user: {}", telegramId);

        if (telegramId == null) {
            log.warn("⚠️ Missing telegramId in verification request");
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "hasAccess", false,
                    "error", "Missing telegramId"
            ));
        }

        try {
            // Check subscription status
            var subscriptionStatus = subscriptionService.getSubscriptionStatus(telegramId);

            Map<String, Object> response = Map.of(
                    "valid", true,
                    "hasAccess", subscriptionStatus.subscribed(),
                    "subscriptionLevel", subscriptionStatus.subscribed() ? "premium" : "free",
                    "message", subscriptionStatus.subscribed() ?
                        "Subscription verified successfully" :
                        "No active subscription found",
                    "subscriptionStatus", subscriptionStatus
            );

            log.info("✅ Subscription verification completed for user {}: hasAccess={}",
                    telegramId, subscriptionStatus.subscribed());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error verifying subscription for user {}: {}", telegramId, e.getMessage());
            return ResponseEntity.ok(subscriptionService.createSubscriptionErrorResponse());
        }
    }
}