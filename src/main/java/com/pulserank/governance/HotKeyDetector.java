package com.pulserank.governance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 热点探测：统计每个 productId 在最近 1 秒内被访问的次数，超过阈值就标记为热点，
 * 标记持续 10 秒。标记期间 {@link com.pulserank.config.CacheConfig} 里的自定义
 * Expiry 会给这个 key 更长的本地缓存 TTL——用少量内存换取对 Redis/数据库更少的
 * 回源次数，这是应对"个别商品评分请求量远超其他商品"这种热点场景的常见手段。
 */
@Component
public class HotKeyDetector {

    private static final Logger log = LoggerFactory.getLogger(HotKeyDetector.class);

    private static final int WINDOW_THRESHOLD = 20;

    private final Cache<Long, LongAdder> windowCounters = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    private final Cache<Long, Boolean> hotKeys = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    public void recordAccess(Long productId) {
        LongAdder adder = windowCounters.get(productId, id -> new LongAdder());
        adder.increment();
        if (adder.sum() >= WINDOW_THRESHOLD && hotKeys.getIfPresent(productId) == null) {
            hotKeys.put(productId, Boolean.TRUE);
            log.info("HOT_KEY_DETECTED productId={} 1秒内请求数>={}, 标记热点10秒", productId, WINDOW_THRESHOLD);
        }
    }

    public boolean isHot(Long productId) {
        return hotKeys.getIfPresent(productId) != null;
    }
}
