package com.my.gateway.loadbalance;

import java.util.Locale;

public class LoadBalancerFactory {

    private static final LoadBalancer RANDOM = new RandomLoadBalancer();
    private static final LoadBalancer RR = new RoundRobinLoadBalancer();
    private static final LoadBalancer CH = new ConsistentHashLoadBalancer();
    // 平滑加权轮询负载均衡器实现类，线程安全，可以共享实例
    private static final SmoothWeightedRoundRobinLoadBalancer SWRR_IMPL = new SmoothWeightedRoundRobinLoadBalancer();

    private static final LoadBalancer SWRR = (upstreams, ctx) -> SWRR_IMPL.choose(upstreams, ctx);


    public static LoadBalancer get(String lb) {
        if (lb == null) {
            return RR; // 默认轮询更“像网关”
        }
        String v = lb.toLowerCase(Locale.ROOT);
        return switch (v) {
            case "random" -> RANDOM;
            case "consistent_hash", "ch" -> CH;
            case "round_robin", "rr" -> RR;
            case "smooth_wrr", "swrr", "weighted_rr" -> SWRR;
            default -> RR;
        };
    }
}
