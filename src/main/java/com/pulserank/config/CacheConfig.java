package com.pulserank.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pulserank.dto.CachedScore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * L1 本地缓存，TTL 故意设得比 L2 Redis 短很多（5s vs 60s）：
     * 本地缓存不一致的时间窗口要尽量小，多副本部署时各实例的本地缓存允许短暂不一致，
     * 但不能久到影响可用性判断。
     */
    @Bean
    public Cache<Long, CachedScore> localScoreCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }
}
