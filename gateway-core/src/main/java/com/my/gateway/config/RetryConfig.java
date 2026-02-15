package com.my.gateway.config;

import lombok.Data;

import java.util.List;

@Data
public class RetryConfig {

    // 最大重试次数（不含首次请求），例如 2 => 最多发 3 次
    private int maxRetries = 1;

    // 只对幂等方法重试（默认 true）
    private boolean idempotentOnly = true;

    // 触发重试的 HTTP 状态码（一般只重试 502/503/504）
    private List<Integer> retryOnStatus = List.of(502, 503, 504);

    // 两次重试间隔（毫秒），做一点退避避免打爆上游
    private long backoffMs = 30;
}
