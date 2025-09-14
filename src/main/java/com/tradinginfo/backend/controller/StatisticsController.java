package com.tradinginfo.backend.controller;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import com.tradinginfo.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/user/{telegramId}")
    public ResponseEntity<UserStatisticsDTO> getUserStatistics(@PathVariable Long telegramId) {
        log.info("📈 Getting statistics for user {}", telegramId);
        UserStatisticsDTO statistics = statisticsService.getUserStatistics(telegramId);
        return ResponseEntity.ok(statistics);
    }
}