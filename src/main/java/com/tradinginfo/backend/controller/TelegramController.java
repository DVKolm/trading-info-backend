package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.service.TelegramAuthService;
import com.tradinginfo.backend.service.TelegramBotService;
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

    private final TelegramBotService telegramBotService;
    private final TelegramAuthService telegramAuthService;

    @GetMapping("/bot/info")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        log.info("Getting bot information");
        Map<String, Object> botInfo = telegramBotService.getBotConfigurationStatus();
        return ResponseEntity.ok(botInfo);
    }

    @PostMapping("/notify/lesson")
    public ResponseEntity<Map<String, String>> notifyNewLesson(
            @RequestBody Map<String, String> request,
            @RequestParam("initData") String initData) {

        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String lessonTitle = request.get("title");
        String lessonPath = request.get("path");

        Map<String, String> result = telegramBotService.processLessonNotification(lessonTitle, lessonPath);

        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/check-subscription/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> checkSubscription(@PathVariable Long userId) {
        log.info("Checking subscription for user: {}", userId);

        return telegramBotService.checkChannelSubscription(userId)
                .map(isSubscribed -> ResponseEntity.ok(telegramBotService.createSubscriptionCheckResponse(userId, isSubscribed, null)))
                .onErrorReturn(ResponseEntity.ok(telegramBotService.createSubscriptionCheckResponse(userId, false, "Could not check subscription")));
    }

    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeMessage(
            @RequestBody Map<String, Object> request) {

        long userId = ((Number) request.get("userId")).longValue();
        String firstName = (String) request.get("firstName");

        telegramBotService.sendWelcomeMessage(userId, firstName);

        return ResponseEntity.ok(Map.of(
                "message", "Welcome message sent",
                "userId", String.valueOf(userId)
        ));
    }

    @PostMapping("/notify/completion")
    public ResponseEntity<Map<String, String>> notifyLessonCompletion(
            @RequestBody Map<String, Object> request) {

        long userId = ((Number) request.get("userId")).longValue();
        String lessonTitle = (String) request.get("lessonTitle");

        Map<String, String> result = telegramBotService.processLessonCompletionNotification(userId, lessonTitle);

        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send-message")
    public ResponseEntity<Map<String, String>> sendMessage(
            @RequestBody Map<String, String> request,
            @RequestParam("initData") String initData) {

        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String message = request.get("message");
        String target = request.get("target");

        Map<String, String> result = telegramBotService.processMessageSending(message, target);

        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

}