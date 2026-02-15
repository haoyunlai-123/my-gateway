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
        // RouteFilter 最后一环，不再 chain.doFilter

        // 第一次 upstream 已经在 LoadBalanceFilter 选过；兜底再选一次
        if (ctx.getSelectedUpstream() == null) {
            String chosen = com.my.gateway.filter.flow.LoadBalanceFilter.chooseUpstream(ctx);
            if (chosen == null) {
                ctx.getResponse().setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
                ctx.getResponse().setJsonContent("{\"error\":\"No upstream available\"}");
                ctx.writeResponse();
                return;
            }
            ctx.setSelectedUpstream(chosen);
            ctx.getTriedUpstreams().add(chosen);
        }

        int maxRetries = getRetryMaxRetries();
        boolean canRetry = canRetryRequest(ctx);

        attempt(ctx, canRetry ? maxRetries : 0);
    }

    private int getRetryMaxRetries() {
        var cfg = com.my.gateway.config.ConfigLoader.getInstance().getConfig();
        if (cfg == null || cfg.getRetry() == null) {
            return 0;
        }
        return Math.max(0, cfg.getRetry().getMaxRetries());
    }

    private boolean canRetryRequest(GatewayContext ctx) {
        var cfg = com.my.gateway.config.ConfigLoader.getInstance().getConfig();
        if (cfg == null || cfg.getRetry() == null) {
            return false;
        }

        if (!cfg.getRetry().isIdempotentOnly()) {
            return true;
        }

        var m = ctx.getRequest().getMethod().name();
        // 幂等：GET/HEAD/OPTIONS/PUT/DELETE（很多网关只放 GET/HEAD，保守起见可以只放 GET/HEAD）
        return m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS") || m.equals("PUT") || m.equals("DELETE");
    }

    private boolean shouldRetryByStatus(int code) {
        var cfg = com.my.gateway.config.ConfigLoader.getInstance().getConfig();
        if (cfg == null || cfg.getRetry() == null) {
            return false;
        }
        return cfg.getRetry().getRetryOnStatus() != null && cfg.getRetry().getRetryOnStatus().contains(code);
    }

    private long backoffMs() {
        var cfg = com.my.gateway.config.ConfigLoader.getInstance().getConfig();
        if (cfg == null || cfg.getRetry() == null) {
            return 0;
        }
        return Math.max(0, cfg.getRetry().getBackoffMs());
    }


    private org.asynchttpclient.Request buildRequest(GatewayContext ctx, String baseUrl) {
        String url = baseUrl + ctx.getRequest().getPath();

        var builder = new org.asynchttpclient.RequestBuilder(ctx.getRequest().getMethod().name());
        builder.setUrl(url);

        ctx.getRequest().getHeaders().forEach(h -> builder.addHeader(h.getKey(), h.getValue()));

        String body = ctx.getRequest().getBody();
        if (body != null && !body.isEmpty()) {
            builder.setBody(body);
        }
        return builder.build();
    }



    private void attempt(GatewayContext ctx, int remainingRetries) {
        String routeId = (ctx.getRoute() != null && ctx.getRoute().getId() != null) ? ctx.getRoute().getId() : "default";
        String upstream = ctx.getSelectedUpstream();

        // 构建请求
        org.asynchttpclient.Request downstreamRequest = buildRequest(ctx, upstream);

        var future = com.my.gateway.netty.AsyncHttpHelper.getInstance().executeRequest(downstreamRequest);

        future.whenComplete((resp, ex) -> {
            if (ex != null) {
                // 网络异常 / 连接异常：认为可重试
                com.my.gateway.health.PassiveHealthManager.getInstance().onFailure(routeId, upstream);

                if (remainingRetries > 0) {
                    String next = com.my.gateway.filter.flow.LoadBalanceFilter.chooseUpstream(ctx);
                    if (next != null) {
                        ctx.setSelectedUpstream(next);
                        ctx.getTriedUpstreams().add(next);

                        scheduleRetry(ctx, remainingRetries - 1);
                        return;
                    }
                }

                // 无法重试：返回错误
                ctx.getResponse().setStatus(HttpResponseStatus.BAD_GATEWAY);
                ctx.getResponse().setJsonContent("{\"error\":\"upstream error: " + safeMsg(ex.getMessage()) + "\"}");
                ctx.writeResponse();
                return;
            }

            int code = resp.getStatusCode();

            // 5xx 认为失败，4xx 不算上游不健康（服务是活的）
            if (code >= 500) {
                com.my.gateway.health.PassiveHealthManager.getInstance().onFailure(routeId, upstream);
            } else {
                com.my.gateway.health.PassiveHealthManager.getInstance().onSuccess(routeId, upstream);
            }

            // 是否触发重试：仅对配置的 502/503/504 等
            if (code >= 500 && remainingRetries > 0 && shouldRetryByStatus(code)) {
                String next = com.my.gateway.filter.flow.LoadBalanceFilter.chooseUpstream(ctx);
                if (next != null) {
                    ctx.setSelectedUpstream(next);
                    ctx.getTriedUpstreams().add(next);

                    scheduleRetry(ctx, remainingRetries - 1);
                    return;
                }
            }

            // 成功/不重试：正常写回
            ctx.getResponse().setStatus(HttpResponseStatus.valueOf(code));
            ctx.getResponse().getHeaders().add(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE, resp.getContentType());
            ctx.getResponse().getHeaders().add("X-Gateway-Upstream", upstream);
            ctx.getResponse().setContent(resp.getResponseBody());
            ctx.writeResponse();
        });
    }

    private void scheduleRetry(GatewayContext ctx, int remaining) {
        long delay = backoffMs();
        if (delay <= 0) {
            attempt(ctx, remaining);
            return;
        }
        // 用 Netty 的 eventLoop 做延迟，不额外开线程
        ctx.getNettyCtx().executor().schedule(() -> attempt(ctx, remaining), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private String safeMsg(String m) {
        if (m == null) {
            return "unknown";
        }
        return m.replace("\"", "'");
    }


    @Override
    public int getOrder() {
        return Integer.MAX_VALUE; // 路由过滤器必须是最后一个执行
    }
}