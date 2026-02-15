package com.my.gateway.filter.route;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import com.my.gateway.health.PassiveHealthManager;
import com.my.gateway.netty.AsyncHttpHelper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

/**
 * 路由过滤器：负责将请求转发到后端服务
 * 重要说明：为了保持 Netty 架构的简单性，演示阶段使用了 get() 阻塞
 * 在生产级高性能网关中，这里应该配合 Netty 的异步回调机制，不应该阻塞 Worker 线程。
 * 但为了不大幅改动当前的 Chain 结构，我们先用 get() 跑通流程)
 */
@Slf4j
public class RouteFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        GatewayRoute route = ctx.getRoute();
        if (route == null) {
            // 如果没有路由，直接返回，或者写入 404
            log.warn("No route found");
            ctx.getResponse().setStatus(HttpResponseStatus.NOT_FOUND);
            ctx.getResponse().setJsonContent("{\"error\":\"Route Not Found\"}");
            ctx.writeResponse(); // 必须手动触发写回
            return;
        }

        Request downstreamRequest = buildRequest(ctx, route);

        // 获取 Future
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(downstreamRequest);

        // ==========================================
        // 关键修改：异步回调，非阻塞！
        // ==========================================
        future.whenComplete((response, throwable) -> {
            String routeId = (ctx.getRoute() != null && ctx.getRoute().getId() != null) ? ctx.getRoute().getId() : "default";
            String upstream = ctx.getSelectedUpstream() != null ? ctx.getSelectedUpstream() : (ctx.getRoute() != null ? ctx.getRoute().getBackendUrl() : null);

            try {
                if (throwable != null) {
                    PassiveHealthManager.getInstance().onFailure(routeId, upstream);

                    ctx.getResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    ctx.getResponse().setJsonContent("{\"error\":\"" + throwable.getMessage() + "\"}");
                } else {
                    int code = response.getStatusCode();
                    if (code >= 500) {
                        PassiveHealthManager.getInstance().onFailure(routeId, upstream);
                    } else {
                        PassiveHealthManager.getInstance().onSuccess(routeId, upstream);
                    }

                    ctx.getResponse().setStatus(HttpResponseStatus.valueOf(code));
                    ctx.getResponse().getHeaders().add(HttpHeaderNames.CONTENT_TYPE, response.getContentType());
                    ctx.getResponse().getHeaders().add("X-Gateway-Upstream", upstream); // 调试很好用
                    ctx.getResponse().setContent(response.getResponseBody());
                }
            } finally {
                ctx.writeResponse();
            }
        });

        // 方法直接返回，Netty 线程释放去处理其他请求了，不在这里等待
    }

    private Request buildRequest(GatewayContext ctx, GatewayRoute route) {
        // 构建后端请求 URL
        String base = ctx.getSelectedUpstream();
        if (base == null) {
            base = route.getBackendUrl(); // 兜底兼容
        }
        String backendUrl = base + ctx.getRequest().getPath();

        RequestBuilder builder = new RequestBuilder(ctx.getRequest().getMethod().name());
        builder.setUrl(backendUrl);

        // 复制 Header (简单演示，复制 Content-Type 即可，生产环境要剔除 Hop-by-Hop Headers)
        ctx.getRequest().getHeaders().forEach(entry -> {
            builder.addHeader(entry.getKey(), entry.getValue());
        });

        // 如果有 Body (如 POST)，也要复制
        if (ctx.getRequest().getBody() != null) {
            builder.setBody(ctx.getRequest().getBody());
        }

        return builder.build();
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE; // 路由过滤器必须是最后一个执行
    }
}