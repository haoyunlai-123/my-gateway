package com.my.gateway.filter.flow;

import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.config.RouteRegistry;
import com.my.gateway.container.RouteManager;
import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;

import java.util.List;

public class RouteSetupFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        // 1. 获取全局配置
        GatewayConfig config = ConfigLoader.getInstance().getConfig();
        if (config != null && config.getRoutes() != null) {
            String reqPath = ctx.getRequest().getPath();

            // 2. 遍历路由表进行匹配 (这里用最简单的 前缀匹配)
            // 生产环境通常使用 AntPathMatcher 或 Tire 树算法优化匹配效率
            /*for (GatewayRoute route : config.getRoutes()) {
                if (reqPath.startsWith(route.getPath())) {
                    ctx.setRoute(route);
                    break;
                }
            }*/

            List<GatewayRoute> routes = RouteRegistry.getInstance().getRoutes();
            for (GatewayRoute route : routes) {
                if (reqPath.startsWith(route.getPath())) {
                    ctx.setRoute(route);
                    break;
                }
            }
        }

        // 3. 继续执行链条
        chain.doFilter(ctx);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}