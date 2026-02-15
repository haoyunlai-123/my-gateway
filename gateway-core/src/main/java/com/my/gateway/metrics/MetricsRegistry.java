package com.my.gateway.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于往 MetricBucket 中记录指标数据的注册中心，提供全局访问点
 */
public class MetricsRegistry {

    private static final MetricsRegistry INSTANCE = new MetricsRegistry();

    // routeId -> bucket
    private final Map<String, MetricBucket> routeBuckets = new ConcurrentHashMap<>();
    // routeId@upstream -> bucket
    private final Map<String, MetricBucket> upstreamBuckets = new ConcurrentHashMap<>();

    private MetricsRegistry() {}

    public static MetricsRegistry getInstance() {
        return INSTANCE;
    }

    public void record(String routeId, String upstream, int statusCode, long rtMs) {
        if (routeId == null) {
            routeId = "default";
        }
        routeBuckets.computeIfAbsent(routeId, k -> new MetricBucket())
                .record(statusCode, rtMs);

        if (upstream != null) {
            String key = routeId + "@" + upstream;
            upstreamBuckets.computeIfAbsent(key, k -> new MetricBucket())
                    .record(statusCode, rtMs);
        }
    }

    public Map<String, MetricBucket> routeView() {
        return routeBuckets;
    }

    public Map<String, MetricBucket> upstreamView() {
        return upstreamBuckets;
    }
}
