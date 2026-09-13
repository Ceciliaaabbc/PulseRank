package com.pulserank.governance;

import com.github.benmanes.caffeine.cache.Expiry;
import com.pulserank.dto.CachedScore;

import java.util.concurrent.TimeUnit;

/**
 * L1 本地缓存的过期策略：普通 key 5 秒过期（和阶段1一致），被
 * {@link HotKeyDetector} 标记为热点的 key 给 30 秒——回源频率降到 1/6，
 * 代价只是本地多存这一个 key 更久，对内存几乎没有影响。
 */
public class HotKeyAwareExpiry implements Expiry<Long, CachedScore> {

    private static final long NORMAL_TTL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long HOT_TTL_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final HotKeyDetector hotKeyDetector;

    public HotKeyAwareExpiry(HotKeyDetector hotKeyDetector) {
        this.hotKeyDetector = hotKeyDetector;
    }

    @Override
    public long expireAfterCreate(Long key, CachedScore value, long currentTime) {
        return hotKeyDetector.isHot(key) ? HOT_TTL_NANOS : NORMAL_TTL_NANOS;
    }

    @Override
    public long expireAfterUpdate(Long key, CachedScore value, long currentTime, long currentDuration) {
        return hotKeyDetector.isHot(key) ? HOT_TTL_NANOS : NORMAL_TTL_NANOS;
    }

    @Override
    public long expireAfterRead(Long key, CachedScore value, long currentTime, long currentDuration) {
        return currentDuration;
    }
}
