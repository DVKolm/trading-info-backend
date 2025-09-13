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
@RequestMapping("/telegram")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class TelegramController {

    private final TelegramBotService telegramBotService;
    private final TelegramAuthService telegramAuthService;

    @GetMapping("/bot/info")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        log.info("📱 Getting bot information");

        if (!telegramBotService.isBotConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "configured", false,
                    "message", "Bot token not configured"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "configured", true,
                "channel", telegramBotService.getChannelInfo()
        ));
    }

    @PostMapping("/notify/lesson")
    public ResponseEntity<Map<String, String>> notifyNewLesson(
            @RequestBody Map<String, String> request,
            @RequestParam("initData") String initData) {

        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Check admin permissions
        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String lessonTitle = request.get("title");
        String lessonPath = request.get("path");

        if (lessonTitle == null || lessonTitle.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lesson title is required"));
        }

        telegramBotService.notifyNewLesson(lessonTitle, lessonPath);

        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to channel",
                "lessonTitle", lessonTitle
        ));
    }

    @PostMapping("/check-subscription/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> checkSubscription(@PathVariable Long userId) {
        log.info("📱 Checking subscription for user: {}", userId);

        return telegramBotService.checkChannelSubscription(userId)
                .map(isSubscribed -> ResponseEntity.ok(Map.of(
                        "userId", userId,
                        "subscribed", isSubscribed,
                        "channelId", "@DailyTradiBlog"
                )))
                .onErrorReturn(ResponseEntity.ok(Map.of(
                        "userId", userId,
                        "subscribed", false,
                        "error", "Could not check subscription"
                )));
    }

    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeMessage(
            @RequestBody Map<String, Object> request) {

        Long userId = ((Number) request.get("userId")).longValue();
        String firstName = (String) request.get("firstName");

        telegramBotService.sendWelcomeMessage(userId, firstName);

        return ResponseEntity.ok(Map.of(
                "message", "Welcome message sent",
                "userId", userId.toString()
        ));
    }

    @PostMapping("/notify/completion")
    public ResponseEntity<Map<String, String>> notifyLessonCompletion(
            @RequestBody Map<String, Object> request) {

        Long userId = ((Number) request.get("userId")).longValue();
        String lessonTitle = (String) request.get("lessonTitle");

        if (lessonTitle == null || lessonTitle.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lesson title is required"));
        }

        telegramBotService.notifyLessonCompletion(userId, lessonTitle);

        return ResponseEntity.ok(Map.of(
                "message", "Completion notification sent",
                "userId", userId.toString()
        ));
    }

    @PostMapping("/send-message")
    public ResponseEntity<Map<String, String>> sendMessage(
            @RequestBody Map<String, String> request,
            @RequestParam("initData") String initData) {

        Long telegramId = telegramAuthService.extractTelegramUserId(initData);

        // Check admin permissions
        if (!telegramAuthService.isAdmin(telegramId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String message = request.get("message");
        String target = request.get("target"); // "channel" or user ID

        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        if ("channel".equals(target)) {
            telegramBotService.sendMessageToChannel(message);
            return ResponseEntity.ok(Map.of("message", "Message sent to channel"));
        } else {
            try {
                Long userId = Long.parseLong(target);
                telegramBotService.sendMessageToUser(userId, message);
                return ResponseEntity.ok(Map.of("message", "Message sent to user " + userId));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid target"));
            }
        }
    }
}