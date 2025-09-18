package com.tradinginfo.backend.service.subscription;

import com.tradinginfo.backend.dto.SubscriptionStatusDTO;

import java.util.Map;

public interface SubscriptionStatusService {
    SubscriptionStatusDTO getSubscriptionStatus(Long telegramId);
    Map<String, Object> createSubscriptionResponse(Long userId, boolean isSubscribed);
    Map<String, Object> createSubscriptionErrorResponse();
    Map<String, Object> verifySubscriptionViaInitData(String initData);
}