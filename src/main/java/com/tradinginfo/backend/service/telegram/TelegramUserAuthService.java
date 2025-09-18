package com.tradinginfo.backend.service.telegram;

public interface TelegramUserAuthService {
    Long extractTelegramUserId(String initData);
    boolean validateInitData(String initData);
    boolean isAdmin(Long telegramUserId);
}