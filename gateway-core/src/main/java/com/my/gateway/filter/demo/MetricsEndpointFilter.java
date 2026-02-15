package com.my.gateway.filter.demo;

import com.my.gateway.filter.GatewayFilter;
import com.my.gateway.filter.GatewayFilterChain;
import com.my.gateway.context.GatewayContext;
import com.my.gateway.metrics.MetricBucket;
import com.my.gateway.metrics.MetricsRegistry;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;

public class MetricsEndpointFilter implements GatewayFilter {

    @Override
    public void doFilter(GatewayContext ctx, GatewayFilterChain chain) throws Exception {
        if (!"/metrics".equals(ctx.getRequest().getPath())) {
            chain.doFilter(ctx);
            return;
        }

        String json = buildJson();
        ctx.getResponse().setStatus(HttpResponseStatus.OK);
        ctx.getResponse().setJsonContent(json);
        ctx.writeResponse();
    }

    private String buildJson() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{");

        sb.append("\"routes\":");
        appendBuckets(sb, MetricsRegistry.getInstance().routeView());

        sb.append(",\"upstreams\":");
        appendBuckets(sb, MetricsRegistry.getInstance().upstreamView());

        sb.append("}");
        return sb.toString();
    }

    private void appendBuckets(StringBuilder sb, Map<String, MetricBucket> view) {
        sb.append("{");
        boolean first = true;
        for (var e : view.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            String k = e.getKey().replace("\"", "'");
            MetricBucket b = e.getValue();

            sb.append("\"").append(k).append("\":{")
                    .append("\"requests\":").append(b.requests.sum()).append(",")
                    .append("\"success\":").append(b.success.sum()).append(",")
                    .append("\"fail\":").append(b.fail.sum()).append(",")
                    .append("\"4xx\":").append(b.status4xx.sum()).append(",")
                    .append("\"5xx\":").append(b.status5xx.sum()).append(",")
                    .append("\"avgRtMs\":").append(b.avgRtMs()).append(",")
                    .append("\"maxRtMs\":").append(b.maxRtMs.get()).append(",")
                    .append("\"p50RtMs\":").append(b.percentile(0.50)).append(",")
                    .append("\"p90RtMs\":").append(b.percentile(0.90)).append(",")
                    .append("\"p99RtMs\":").append(b.percentile(0.99))
                    .append("}");
        }
        sb.append("}");
    }

    @Override
    public int getOrder() {
        // 放在很前面，避免走后续链路
        return -200;
    }
}
