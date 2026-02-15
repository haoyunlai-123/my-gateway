package com.my.gateway.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FilterFactory {

    // 静态单例模式
    private static final FilterFactory INSTANCE = new FilterFactory();

    private final List<GatewayFilter> filters = new ArrayList<>();

    private FilterFactory() {
        // 在这里注册过滤器
        // 1. 监控过滤器 (Demo)
        filters.add(new com.my.gateway.filter.demo.MonitorFilter());
        filters.add(new com.my.gateway.filter.flow.RouteSetupFilter());
        filters.add(new com.my.gateway.filter.route.RouteFilter());
        // 2. 可以在这里加更多的过滤器...

        // 按照 Order 排序
        filters.sort(Comparator.comparingInt(GatewayFilter::getOrder));
    }

    public static FilterFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 构建一个新的过滤器链
     * 注意：每次请求都需要一个新的 Chain 对象，因为 Chain 内部有 index 状态
     */
    public GatewayFilterChain buildFilterChain() {
        return new DefaultGatewayFilterChain(filters);
    }
}