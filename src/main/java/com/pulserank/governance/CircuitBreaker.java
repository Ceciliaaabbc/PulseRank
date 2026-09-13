package com.pulserank.governance;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简化版熔断器：CLOSED -&gt; OPEN -&gt; HALF_OPEN -&gt; CLOSED/OPEN。
 * 保护的是"直接查库"这条路径——数据库变慢或者故障时，与其让所有请求都去
 * 排队等一个大概率会超时的数据库连接，不如快速失败、直接返回降级结果，
 * 减少故障期间对数据库雪上加霜的压力，也让调用方更快拿到响应（哪怕是降级的）。
 * 每个受保护的资源(比如每个分片)可以有独立实例，这里数据库查询按全局一个实例处理，
 * 因为最终都是同一个 MySQL 实例，没必要按分片再细分熔断状态。
 */
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openDurationMillis;
    private final int halfOpenTrialCount;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);
    private final AtomicInteger halfOpenTrialsInFlight = new AtomicInteger(0);

    public CircuitBreaker(int failureThreshold, long openDurationMillis, int halfOpenTrialCount) {
        this.failureThreshold = failureThreshold;
        this.openDurationMillis = openDurationMillis;
        this.halfOpenTrialCount = halfOpenTrialCount;
    }

    /**
     * 请求发起前调用：决定这次请求是放行去打数据库，还是直接快速失败。
     */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            long elapsed = System.currentTimeMillis() - openedAt.get();
            if (elapsed >= openDurationMillis) {
                // 冷却时间到了，尝试转入 HALF_OPEN 放几个探测请求
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenTrialsInFlight.set(0);
                }
                return allowRequest();
            }
            return false;
        }
        // HALF_OPEN：只放行有限的几个探测请求，其余仍然快速失败
        int inFlight = halfOpenTrialsInFlight.incrementAndGet();
        if (inFlight > halfOpenTrialCount) {
            halfOpenTrialsInFlight.decrementAndGet();
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            // 探测请求成功，说明下游已经恢复，直接关闭熔断
            state.set(State.CLOSED);
            consecutiveFailures.set(0);
        } else if (current == State.CLOSED) {
            consecutiveFailures.set(0);
        }
    }

    public void recordFailure() {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            // 探测请求还是失败，说明下游没恢复，重新回到 OPEN 继续冷却
            trip();
            return;
        }
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            trip();
        }
    }

    private void trip() {
        state.set(State.OPEN);
        openedAt.set(System.currentTimeMillis());
        consecutiveFailures.set(0);
    }

    public State getState() {
        return state.get();
    }
}
