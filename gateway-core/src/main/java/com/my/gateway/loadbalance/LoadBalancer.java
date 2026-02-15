package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;

import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalancer {

    String choose(List<String> upstreams, GatewayContext ctx);

    default String name() {
        return this.getClass().getSimpleName();
    }
}
