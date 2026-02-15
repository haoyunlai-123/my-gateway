package com.my.gateway.filter.demo;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitorFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        long startTime = System.currentTimeMillis();

        // --- 前置逻辑 ---
        log.info("[MonitorFilter] Request Start: path={}", ctx.getRequest().getPath());

        // 执行下一个过滤器 (递归调用)
        chain.doFilter(ctx);

        ctx.getResponse().setJsonContent("Filter Works!");

        // --- 后置逻辑 (等后面的过滤器都执行完，栈弹回来时执行) ---
        long endTime = System.currentTimeMillis();
        log.info("[MonitorFilter] Request End: cost={}ms", (endTime - startTime));
    }

    @Override
    public int getOrder() {
        // 优先级最高，最先执行前置逻辑，最后执行后置逻辑
        return -100;
    }
}