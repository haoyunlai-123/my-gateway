package com.my.gateway.filter;

import com.my.gateway.context.GatewayContext;

public interface GatewayFilterChain {
    /**
     * 继续执行链条中的下一个节点
     */
    void doFilter(GatewayContext ctx) throws Exception;
}
