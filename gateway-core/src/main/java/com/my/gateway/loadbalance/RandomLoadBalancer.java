package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public String choose(List<String> upstreams, GatewayContext ctx) {
        int idx = ThreadLocalRandom.current().nextInt(upstreams.size());
        return upstreams.get(idx);
    }
}

