package com.pulserank.governance;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 挂在查询接口上的分布式限流拦截器。限流粒度是全局一个桶，不是按 IP/用户
 * 细分——目的是保护后端整体不被瞬时流量打垮，不是做用户级别的配额管理，
 * 两者是不同的问题，配额管理留给真正需要的时候再加。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String BUCKET_KEY = "ratelimit:products-score";

    private final TokenBucketRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final int capacity;
    private final int refillPerSecond;

    public RateLimitInterceptor(TokenBucketRateLimiter rateLimiter,
                                 MeterRegistry meterRegistry,
                                 @Value("${pulserank.ratelimit.capacity:50}") int capacity,
                                 @Value("${pulserank.ratelimit.refill-per-second:20}") int refillPerSecond) {
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    /**
     * 限流依赖的 Redis 本身不可用时，选择放行(fail-open)而不是拒绝(fail-closed)。
     * 限流器是用来保护后端不被打垮的辅助手段，它自己的依赖出故障不应该
     * 变成把整个接口打挂的新故障源——那样限流组件本身就成了单点故障。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        boolean allowed;
        try {
            allowed = rateLimiter.tryAcquire(BUCKET_KEY, capacity, refillPerSecond);
        } catch (RuntimeException e) {
            log.warn("RATE_LIMITER_UNAVAILABLE 限流依赖的Redis不可用，本次请求放行(fail-open): {}", e.getMessage());
            return true;
        }
        if (allowed) {
            return true;
        }
        meterRegistry.counter("pulserank.ratelimit.rejected").increment();
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"rate limited\"}");
        return false;
    }
}
