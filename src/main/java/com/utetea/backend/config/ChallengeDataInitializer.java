package com.utetea.backend.config;

import com.utetea.backend.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Khởi tạo dữ liệu Challenge mặc định khi ứng dụng khởi động
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeDataInitializer implements CommandLineRunner {
    
    private final ChallengeService challengeService;
    
    @Override
    public void run(String... args) {
        log.info("🎯 Initializing default challenges...");
        challengeService.initDefaultChallenges();
    }
}
