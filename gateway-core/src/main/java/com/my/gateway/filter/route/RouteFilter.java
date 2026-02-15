package com.my.gateway.filter.route;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRoute;
import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
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

        // 1. 如果没有配置路由，直接放行或报错（这里简单处理为打印日志）
        if (route == null) {
            log.warn("No route found for path: {}", ctx.getRequest().getPath());
            return;
        }

        log.info("Forwarding to: {}", route.getBackendUrl());

        // 2. 构建转发请求 (将 GatewayRequest 转为 AsyncHttpClient Request)
        Request downstreamRequest = buildRequest(ctx, route);

        // 3. 执行异步调用
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(downstreamRequest);

        // 4. 等待结果 (重要说明：为了保持 Netty 架构的简单性，演示阶段在这里使用了 get() 阻塞
        // 在生产级高性能网关中，这里应该配合 Netty 的异步回调机制，不应该阻塞 Worker 线程。
        // 但为了不大幅改动当前的 Chain 结构，我们先用 get() 跑通流程)
        Response response = future.get(); // 阻塞等待后端响应

        // 5. 将后端响应写回 GatewayContext
        ctx.getResponse().setStatus(HttpResponseStatus.valueOf(response.getStatusCode()));
        ctx.getResponse().getHeaders().add(HttpHeaderNames.CONTENT_TYPE, response.getContentType());
        ctx.getResponse().setContent(response.getResponseBody());

        // 6. 链条结束，不需要再调用 chain.doFilter(ctx) 因为这是最后一环
    }

    private Request buildRequest(GatewayContext ctx, GatewayRoute route) {
        String backendUrl = route.getBackendUrl() + ctx.getRequest().getPath(); // 简单拼接: backend + path

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