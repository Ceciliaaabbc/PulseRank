package com.pulserank.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pulserank.dto.CachedScore;
import com.pulserank.governance.HotKeyAwareExpiry;
import com.pulserank.governance.HotKeyDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    /**
     * L1 本地缓存，普通 key TTL 5s，比 L2 Redis(60s) 短很多：本地缓存不一致
     * 的时间窗口要尽量小。过期策略换成 HotKeyAwareExpiry 后，被识别为热点的
     * key 会拿到更长的 TTL，见阶段3"热点探测"部分。
     */
    @Bean
    public Cache<Long, CachedScore> localScoreCache(HotKeyDetector hotKeyDetector) {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new HotKeyAwareExpiry(hotKeyDetector))
                .build();
    }
}
