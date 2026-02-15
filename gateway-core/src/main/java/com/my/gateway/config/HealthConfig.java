package com.my.gateway.config;

import lombok.Data;

@Data
public class HealthConfig {
    // 连续失败多少次 -> OPEN
    private int failureThreshold = 3;

    // OPEN 后冷却多久进入 HALF_OPEN
    private long openCooldownMs = 10_000;

    // HALF_OPEN 允许并发探测请求数（避免一下子把半开节点打爆）
    private int halfOpenMaxCalls = 1;

    // HALF_OPEN 连续成功多少次 -> CLOSED
    private int successThresholdToClose = 2;
}
