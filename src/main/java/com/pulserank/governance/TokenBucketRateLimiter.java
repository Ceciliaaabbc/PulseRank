package com.pulserank.governance;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 分布式令牌桶限流。桶状态存在 Redis 里，多个应用实例共享同一份限流预算，
 * 不是每个 JVM 各算各的（那样实例数一多，总放行量会成倍超出预期）。
 * 令牌桶算法本身和"读桶-补令牌-扣令牌-写回"这套流程都是手写的，
 * Redisson 在这里只是执行 Lua 脚本的客户端，不是拿现成的限流器。
 */
@Component
public class TokenBucketRateLimiter {

    private final RedissonClient redissonClient;
    private final String script;

    public TokenBucketRateLimiter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = loadScript();
    }

    /**
     * @return true 表示放行，false 表示被限流
     */
    public boolean tryAcquire(String bucketKey, int capacity, int refillPerSecond) {
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        List<Object> result = rScript.eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.MULTI,
                Collections.singletonList(bucketKey),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond),
                String.valueOf(System.currentTimeMillis()),
                "1");
        String allowed = String.valueOf(result.get(0));
        return "1".equals(allowed);
    }

    private String loadScript() {
        try (InputStream in = new ClassPathResource("scripts/token_bucket.lua").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法加载 token_bucket.lua", e);
        }
    }
}
