package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.UpstreamInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {

    // 每个 route 维护一个序号（避免全局串扰）
    private final ConcurrentHashMap<String, AtomicInteger> seqMap = new ConcurrentHashMap<>();

    @Override
    public String choose(List<UpstreamInstance> upstreams, GatewayContext ctx) {
        String routeId = (ctx.getRoute() != null && ctx.getRoute().getId() != null)
                ? ctx.getRoute().getId()
                : "default";

        AtomicInteger seq = seqMap.computeIfAbsent(routeId, k -> new AtomicInteger(0));
        int idx = Math.floorMod(seq.getAndIncrement(), upstreams.size());
        return upstreams.get(idx).getUrl();
    }
}
