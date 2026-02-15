package com.my.gateway.filter.flow;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;

/**
 * 这是一个简单的路由定位过滤器
 * 它的作用是模拟“根据 URL 找到对应服务”的过程
 */
public class RouteSetupFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        // 模拟路由表：如果访问 /baidu，就转发到 www.baidu.com
        if (ctx.getRequest().getPath().startsWith("/baidu")) {
            GatewayRoute route = GatewayRoute.builder()
                    .id("baidu-route")
                    .path("/baidu")
                    .backendUrl("http://www.baidu.com") // 这里注意，百度会校验Host，可能直接转发会403，建议换成 httpbin.org
                    .build();
            ctx.setRoute(route);
        }
        // 模拟测试服务：如果访问 /json，转发到 httpbin.org/json
        else if (ctx.getRequest().getPath().startsWith("/json")) {
            GatewayRoute route = GatewayRoute.builder()
                    .id("httpbin-route")
                    .path("/json") // httpbin 的路径
                    .backendUrl("http://httpbin.org")
                    .build();
            ctx.setRoute(route);
        }

        chain.doFilter(ctx);
    }

    @Override
    public int getOrder() {
        return 0; // 必须在 MonitorFilter 之后，RouteFilter 之前
    }
}