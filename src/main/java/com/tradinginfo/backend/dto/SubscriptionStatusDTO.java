package com.tradinginfo.backend.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SubscriptionStatusDTO(
        Long telegramId,
        boolean subscribed,
        boolean verified,
        LocalDateTime verifiedAt,
        LocalDateTime expiresAt,
        String message) {
}