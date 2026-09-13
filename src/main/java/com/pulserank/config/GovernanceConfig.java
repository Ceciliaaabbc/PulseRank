package com.pulserank.config;

import com.pulserank.governance.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GovernanceConfig {

    /**
     * 保护数据库查询路径的熔断器：连续失败达到阈值就跳闸(OPEN)，冷却期结束后
     * 放几个探测请求(HALF_OPEN)试探数据库是否恢复，成功就关闭(CLOSED)，
     * 失败就重新回到 OPEN 继续冷却。阈值调得比较低(5次)是为了在本地演示/压测
     * 时几秒内就能观察到状态切换，生产环境这几个数字应该按下游真实的
     * 故障恢复时间来调。
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(
            MeterRegistry meterRegistry,
            @Value("${pulserank.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${pulserank.circuit-breaker.open-duration-ms:5000}") long openDurationMillis,
            @Value("${pulserank.circuit-breaker.half-open-trial-count:2}") int halfOpenTrialCount) {
        CircuitBreaker circuitBreaker = new CircuitBreaker(failureThreshold, openDurationMillis, halfOpenTrialCount);
        // Gauge 直接读状态枚举的 ordinal(0=CLOSED,1=OPEN,2=HALF_OPEN)，Grafana 上画成时间线
        // 能直观看到熔断跳闸和恢复的时刻，比翻日志方便。
        meterRegistry.gauge("pulserank.circuitbreaker.state", circuitBreaker,
                cb -> cb.getState().ordinal());
        return circuitBreaker;
    }
}
