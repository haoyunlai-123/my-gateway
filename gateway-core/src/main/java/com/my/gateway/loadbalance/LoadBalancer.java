package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.UpstreamInstance;

import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalancer {

    String choose(List<UpstreamInstance> upstreams, GatewayContext ctx);

    default String name() {
        return this.getClass().getSimpleName();
    }
}
