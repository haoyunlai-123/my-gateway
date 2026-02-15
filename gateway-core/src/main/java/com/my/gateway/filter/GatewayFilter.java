package com.my.gateway.filter;

import com.my.gateway.context.GatewayContext;

public interface GatewayFilter {

    /**
     * 执行过滤逻辑
     *
     * @param ctx   网关上下文
     * @param chain 过滤器链（用于放行到下一个过滤器）
     */
    void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception;

    /**
     * 过滤器优先级，值越小越先执行
     * 用于排序
     */
    default int getOrder() {
        return 0;
    }
}
