package com.my.gateway.filter.flow;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import com.my.gateway.loadbalance.LoadBalancerFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class LoadBalanceFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        GatewayRoute route = ctx.getRoute();
        if (route == null) {
            chain.doFilter(ctx);
            return;
        }

        List<String> upstreams = route.getUpstreams();
        // 兼容老配置：只有 backendUrl
        if ((upstreams == null || upstreams.isEmpty()) && route.getBackendUrl() != null) {
            ctx.setSelectedUpstream(route.getBackendUrl());
            chain.doFilter(ctx);
            return;
        }

        if (upstreams == null || upstreams.isEmpty()) {
            ctx.getResponse().setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
            ctx.getResponse().setJsonContent("{\"error\":\"No upstreams available\"}");
            ctx.writeResponse();
            return;
        }

        String chosen = LoadBalancerFactory.get(route.getLb()).choose(upstreams, ctx);
        ctx.setSelectedUpstream(chosen);
        log.info("[LB] routeId={}, strategy={}, chosen={}", route.getId(), route.getLb(), chosen);

        chain.doFilter(ctx);
    }

    @Override
    public int getOrder() {
        // RouteSetupFilter 是 0 的话，这里给 10，确保它在路由匹配之后
        return 10;
    }
}
