package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.SubscriptionStatusDTO;
import com.tradinginfo.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Check if user has access to a specific lesson
     */
    @GetMapping("/access/check")
    public ResponseEntity<Map<String, Object>> checkLessonAccess(
            @RequestParam(required = false) Long telegramId,
            @RequestParam String lessonPath) {

        var hasAccess = subscriptionService.hasAccessToLesson(telegramId, lessonPath);
        var isPremium = subscriptionService.isPremiumLesson(lessonPath);

        return ResponseEntity.ok(Map.of(
            "hasAccess", hasAccess,
            "isPremiumContent", isPremium,
            "requiresSubscription", isPremium && !hasAccess
        ));
    }

    /**
     * Get subscription status
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<SubscriptionStatusDTO> getSubscriptionStatus(@PathVariable Long userId) {
        log.info("Getting subscription status for user: {}", userId);
        var status = subscriptionService.getSubscriptionStatus(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * Handle subscription verification callback
     */
    @PostMapping("/callback/verified")
    public ResponseEntity<Void> handleVerificationCallback(
            @RequestParam Long telegramId,
            @RequestParam boolean verified) {

        subscriptionService.handleSubscriptionVerification(telegramId, verified);
        return ResponseEntity.ok().build();
    }

    /**
     * Grant premium access (admin only)
     */
    @PostMapping("/admin/grant")
    public ResponseEntity<Map<String, Object>> grantPremiumAccess(
            @RequestParam Long telegramId,
            @RequestParam(defaultValue = "30") int days) {

        subscriptionService.grantPremiumAccess(telegramId, days);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Premium access granted for " + days + " days"
        ));
    }

    /**
     * Revoke premium access (admin only)
     */
    @PostMapping("/admin/revoke")
    public ResponseEntity<Map<String, Object>> revokePremiumAccess(@RequestParam Long telegramId) {
        subscriptionService.revokePremiumAccess(telegramId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Premium access revoked"
        ));
    }
}