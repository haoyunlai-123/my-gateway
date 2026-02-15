package com.my.gateway.health;

/**
 * 服务状态
 */
public enum UpstreamState {

    CLOSED,     // 正常
    OPEN,       // 熔断（fail-fast）
    HALF_OPEN   // 半开（探测）

}
