package com.my.gateway.filter.flow;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.context.UpstreamInstance;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import com.my.gateway.health.PassiveHealthManager;
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

        // 复用 chooseUpstream：包含「排除已试过 + health.tryAcquire + lb选择」
        String chosen = chooseUpstream(ctx);

        if (chosen == null) {
            // 这里区分一下：是没配置 upstream，还是全都不健康/都试过了
            List<UpstreamInstance> upstreams = route.getUpstreams();
            if ((upstreams == null || upstreams.isEmpty()) && route.getBackendUrl() == null) {
                ctx.getResponse().setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
                ctx.getResponse().setJsonContent("{\"error\":\"No upstreams configured\"}");
            } else {
                ctx.getResponse().setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
                ctx.getResponse().setJsonContent("{\"error\":\"No available upstream (all unhealthy or already tried)\"}");
            }
            ctx.writeResponse();
            return;
        }

        ctx.setSelectedUpstream(chosen);

        // 关键：把本次 chosen 记入 tried，保证重试不会再选回同一个
        ctx.getTriedUpstreams().add(chosen);

        chain.doFilter(ctx);
    }

    /**
     * 选择一个上游实例
     * @param ctx
     * @return
     */
    public static String chooseUpstream(GatewayContext ctx) {
        GatewayRoute route = ctx.getRoute();
        if (route == null) {
            return null;
        }

        String routeId = route.getId() == null ? "default" : route.getId();
        PassiveHealthManager hm = PassiveHealthManager.getInstance();

        List<UpstreamInstance> upstreams = route.getUpstreams();
        if ((upstreams == null || upstreams.isEmpty()) && route.getBackendUrl() != null) {
            return route.getBackendUrl();
        }
        if (upstreams == null || upstreams.isEmpty()) {
            return null;
        }

        // 过滤：健康 + 没试过
        List<UpstreamInstance> candidates = upstreams.stream()
                .filter(u -> !ctx.getTriedUpstreams().contains(u.getUrl()))
                .filter(u -> hm.tryAcquire(routeId, u.getUrl()))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        return LoadBalancerFactory.get(route.getLb()).choose(candidates, ctx);
    }

    @Override
    public int getOrder() {
        // RouteSetupFilter 是 0 的话，这里给 10，确保它在路由匹配之后
        return 10;
    }

}
