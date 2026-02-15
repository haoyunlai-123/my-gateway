package com.my.gateway.loadbalance;

import java.util.Locale;

public class LoadBalancerFactory {

    private static final LoadBalancer RANDOM = new RandomLoadBalancer();
    private static final LoadBalancer RR = new RoundRobinLoadBalancer();
    private static final LoadBalancer CH = new ConsistentHashLoadBalancer();

    public static LoadBalancer get(String lb) {
        if (lb == null) {
            return RR; // 默认轮询更“像网关”
        }
        String v = lb.toLowerCase(Locale.ROOT);
        return switch (v) {
            case "random" -> RANDOM;
            case "consistent_hash", "ch" -> CH;
            case "round_robin", "rr" -> RR;
            default -> RR;
        };
    }
}
