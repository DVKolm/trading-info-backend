package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.service.telegram.TelegramUserAuthService;
import com.tradinginfo.backend.service.telegram.TelegramSubscriptionService;
import com.tradinginfo.backend.service.telegram.TelegramBotConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class TelegramController {

    private final TelegramSubscriptionService telegramSubscriptionService;
    private final TelegramBotConfigService telegramBotConfigService;

    @GetMapping("/bot/info")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        log.info("Getting bot information");
        Map<String, Object> botInfo = telegramBotConfigService.getBotConfigurationStatus();
        return ResponseEntity.ok(botInfo);
    }


    @PostMapping("/check-subscription/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> checkSubscription(@PathVariable Long userId) {
        log.info("Checking subscription for user: {}", userId);

        return telegramSubscriptionService.checkChannelSubscription(userId)
                .map(isSubscribed -> ResponseEntity.ok(telegramSubscriptionService.createSubscriptionCheckResponse(userId, isSubscribed, null)))
                .onErrorReturn(ResponseEntity.ok(telegramSubscriptionService.createSubscriptionCheckResponse(userId, false, "Could not check subscription")));
    }


}