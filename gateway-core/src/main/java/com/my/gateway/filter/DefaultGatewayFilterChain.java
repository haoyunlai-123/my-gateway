package com.my.gateway.filter;

import com.my.gateway.context.GatewayContext;
import java.util.List;

/**
 * 过滤器链，责任链模式
 */
public class DefaultGatewayFilterChain implements GatewayFilterChain {

    private final List<GatewayFilter> filters;
    private int index = 0;

    public DefaultGatewayFilterChain(List<GatewayFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 如果所有过滤器都执行完了，就结束
        if (index >= filters.size()) {
            return;
        }

        // 取出当前过滤器
        GatewayFilter filter = filters.get(index);
        index++; // 索引后移，准备给下一次调用

        // 执行当前过滤器的逻辑
        // 注意：把 this (当前链对象) 传进去，过滤器内部会调用 chain.doFilter(ctx) 来触发下一次递归
        filter.doFilter(ctx, this);
    }
}