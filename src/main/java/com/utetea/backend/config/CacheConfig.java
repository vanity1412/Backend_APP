package com.utetea.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    // Cache names constants
    public static final String DRINKS_CACHE = "drinks";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String STORES_CACHE = "stores";
    public static final String PROMOTIONS_CACHE = "promotions";
    
    // GHN Cache names (địa chỉ ít thay đổi nên cache lâu hơn)
    public static final String GHN_PROVINCES_CACHE = "ghn-provinces";
    public static final String GHN_DISTRICTS_CACHE = "ghn-districts";
    public static final String GHN_WARDS_CACHE = "ghn-wards";
}
