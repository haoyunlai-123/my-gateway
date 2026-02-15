package com.my.gateway.health;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器实现类，基于经典的三态（CLOSED, OPEN, HALF_OPEN）设计
 */
public class UpstreamCircuitBreaker {

    private final int failureThreshold;
    private final long openCooldownMs;
    private final int halfOpenMaxCalls;
    private final int successThresholdToClose;

    private volatile UpstreamState state = UpstreamState.CLOSED;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger halfOpenInFlight = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger(0);

    private volatile long openUntilEpochMs = 0;

    public UpstreamCircuitBreaker(int failureThreshold,
                                  long openCooldownMs,
                                  int halfOpenMaxCalls,
                                  int successThresholdToClose) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openCooldownMs = Math.max(100, openCooldownMs);
        this.halfOpenMaxCalls = Math.max(1, halfOpenMaxCalls);
        this.successThresholdToClose = Math.max(1, successThresholdToClose);
    }

    /**
     * 是否允许本次请求打到该 upstream（fail-fast 的核心）
     */
    public boolean tryAcquire() {
        long now = System.currentTimeMillis();

        if (state == UpstreamState.OPEN) {
            if (now < openUntilEpochMs) {
                return false;
            }
            // 冷却结束 -> HALF_OPEN
            synchronized (this) {
                if (state == UpstreamState.OPEN && now >= openUntilEpochMs) {
                    state = UpstreamState.HALF_OPEN;
                    halfOpenInFlight.set(0);
                    halfOpenSuccesses.set(0);
                }
            }
        }

        if (state == UpstreamState.HALF_OPEN) {
            int inflight = halfOpenInFlight.incrementAndGet();
            if (inflight > halfOpenMaxCalls) {
                halfOpenInFlight.decrementAndGet();
                return false;
            }
            return true;
        }

        // CLOSED
        return true;
    }

    public void onSuccess() {
        if (state == UpstreamState.CLOSED) {
            consecutiveFailures.set(0);
            return;
        }

        if (state == UpstreamState.HALF_OPEN) {
            halfOpenInFlight.decrementAndGet();
            int s = halfOpenSuccesses.incrementAndGet();
            if (s >= successThresholdToClose) {
                synchronized (this) {
                    state = UpstreamState.CLOSED;
                    consecutiveFailures.set(0);
                    halfOpenInFlight.set(0);
                    halfOpenSuccesses.set(0);
                }
            }
        }
    }

    public void onFailure() {
        if (state == UpstreamState.CLOSED) {
            int f = consecutiveFailures.incrementAndGet();
            if (f >= failureThreshold) {
                tripOpen();
            }
            return;
        }

        if (state == UpstreamState.HALF_OPEN) {
            halfOpenInFlight.decrementAndGet();
            // 半开失败：立刻 OPEN
            tripOpen();
        }

        // OPEN 状态下失败无意义（本来也不该进来）
    }

    private void tripOpen() {
        synchronized (this) {
            state = UpstreamState.OPEN;
            openUntilEpochMs = System.currentTimeMillis() + openCooldownMs;
            consecutiveFailures.set(0);
            halfOpenInFlight.set(0);
            halfOpenSuccesses.set(0);
        }
    }

    public UpstreamState getState() {
        return state;
    }
}
