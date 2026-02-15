package com.my.gateway.filter.demo;

import com.my.gateway.container.RouteManager;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import com.my.gateway.context.GatewayContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;

public class RoutesEndpointFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        if (!"/routes".equals(ctx.getRequest().getPath())) {
            chain.doFilter(ctx);
            return;
        }

        List<GatewayRoute> routes = RouteManager.getInstance().current().getRoutes();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"size\":").append(routes.size()).append(",\"routes\":[");
        for (int i = 0; i < routes.size(); i++) {
            GatewayRoute r = routes.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(r.getId()).append("\",")
                    .append("\"path\":\"").append(r.getPath()).append("\",")
                    .append("\"lb\":\"").append(r.getLb()).append("\"}");
        }
        sb.append("]}");

        ctx.getResponse().setStatus(HttpResponseStatus.OK);
        ctx.getResponse().setJsonContent(sb.toString());
        ctx.writeResponse();
    }

    @Override
    public int getOrder() {
        return -190; // 跟 metrics 差不多前
    }
}
