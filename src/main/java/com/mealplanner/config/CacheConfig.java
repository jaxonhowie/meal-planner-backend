package com.mealplanner.config;

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
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        // 允许缓存 null 值，防止缓存穿透
        manager.setAllowNullValues(true);
        return manager;
    }

    @Bean
    public CacheManager longTermCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(30, TimeUnit.MINUTES));
        manager.setAllowNullValues(true);
        return manager;
    }

    @Bean
    public CacheManager statsCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        manager.setAllowNullValues(true);
        return manager;
    }
}
