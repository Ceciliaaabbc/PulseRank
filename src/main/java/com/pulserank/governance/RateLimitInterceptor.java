package com.pulserank.governance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private static final String BUCKET_KEY = "ratelimit:products-score";

    private final TokenBucketRateLimiter rateLimiter;
    private final int capacity;
    private final int refillPerSecond;

    public RateLimitInterceptor(TokenBucketRateLimiter rateLimiter,
                                 @Value("${pulserank.ratelimit.capacity:50}") int capacity,
                                 @Value("${pulserank.ratelimit.refill-per-second:20}") int refillPerSecond) {
        this.rateLimiter = rateLimiter;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (rateLimiter.tryAcquire(BUCKET_KEY, capacity, refillPerSecond)) {
            return true;
        }
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"rate limited\"}");
        return false;
    }
}
