package com.utetea.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * ⏰ Timezone Configuration
 * Set default timezone to Asia/Ho_Chi_Minh (UTC+7)
 * Đảm bảo toàn bộ app sử dụng timezone Việt Nam
 */
@Configuration
@Slf4j
public class TimezoneConfig {

    private static final String VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(VIETNAM_TIMEZONE));
        log.info("⏰ Default timezone set to: {} (UTC+7)", VIETNAM_TIMEZONE);
    }
}
