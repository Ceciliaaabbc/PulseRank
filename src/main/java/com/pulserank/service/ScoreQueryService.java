package com.pulserank.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.pulserank.dto.CachedScore;
import com.pulserank.dto.ProductScoreResponse;
import com.pulserank.repository.ShardedRatingRepository;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ScoreQueryService {

    private static final Logger log = LoggerFactory.getLogger(ScoreQueryService.class);

    private static final Duration REDIS_TTL = Duration.ofSeconds(60);
    private static final long LOCK_WAIT_MS = 200;
    private static final long LOCK_LEASE_MS = 3000;
    private static final int MISS_POLL_RETRIES = 5;
    private static final long MISS_POLL_INTERVAL_MS = 100;

    private final ShardedRatingRepository ratingRepository;
    private final RedissonClient redissonClient;
    private final Cache<Long, CachedScore> localCache;

    public ScoreQueryService(ShardedRatingRepository ratingRepository,
                              RedissonClient redissonClient,
                              Cache<Long, CachedScore> localCache) {
        this.ratingRepository = ratingRepository;
        this.redissonClient = redissonClient;
        this.localCache = localCache;
    }

    public ProductScoreResponse getProductScore(Long productId) {
        CachedScore cached = localCache.getIfPresent(productId);
        if (cached != null) {
            return toResponse(productId, cached, "L1");
        }

        RBucket<CachedScore> bucket = redissonClient.getBucket(redisKey(productId));
        cached = bucket.get();
        if (cached != null) {
            localCache.put(productId, cached);
            return toResponse(productId, cached, "L2");
        }

        return loadWithLock(productId, bucket);
    }

    /**
     * L1、L2 都未命中时才会走到这里。用 Redisson 分布式锁保证同一个 productId
     * 在同一时刻只有一个请求真正去查库、重建缓存——这是缓存击穿的标准防护手段：
     * 热点数据缓存失效瞬间，成百上千个并发请求不能全部穿透到数据库。
     * 拿不到锁的请求短暂轮询等待锁持有者填好缓存；等不到就降级直接查库，
     * 避免无限期阻塞影响可用性。
     */
    private ProductScoreResponse loadWithLock(Long productId, RBucket<CachedScore> bucket) {
        RLock lock = redissonClient.getLock(lockKey(productId));
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_MS, LOCK_LEASE_MS, TimeUnit.MILLISECONDS);
            if (locked) {
                CachedScore cached = bucket.get();
                if (cached != null) {
                    localCache.put(productId, cached);
                    return toResponse(productId, cached, "L2");
                }
                CachedScore fresh = queryDatabase(productId);
                bucket.set(fresh, REDIS_TTL);
                localCache.put(productId, fresh);
                return toResponse(productId, fresh, "DB");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        for (int i = 0; i < MISS_POLL_RETRIES; i++) {
            sleep(MISS_POLL_INTERVAL_MS);
            CachedScore cached = bucket.get();
            if (cached != null) {
                localCache.put(productId, cached);
                return toResponse(productId, cached, "L2-wait");
            }
        }

        log.warn("productId={} 等锁超时仍未等到缓存重建，降级直接查库", productId);
        return toResponse(productId, queryDatabase(productId), "DB-fallback");
    }

    private CachedScore queryDatabase(Long productId) {
        log.info("DB_QUERY productId={} shard={}", productId, ShardedRatingRepository.shardOf(productId));
        return ratingRepository.aggregateByProductId(productId);
    }

    public void evict(Long productId) {
        localCache.invalidate(productId);
        redissonClient.getBucket(redisKey(productId)).delete();
    }

    private String redisKey(Long productId) {
        return "score:" + productId;
    }

    private String lockKey(Long productId) {
        return "lock:score:" + productId;
    }

    private ProductScoreResponse toResponse(Long productId, CachedScore cached, String source) {
        return new ProductScoreResponse(productId, cached.getAvgScore(), cached.getRatingCount(), source);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
