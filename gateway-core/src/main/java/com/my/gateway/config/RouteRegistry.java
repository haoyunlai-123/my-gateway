package com.my.gateway.config;

import com.my.gateway.context.GatewayRoute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RouteRegistry {

    private static final RouteRegistry INSTANCE = new RouteRegistry();
    private final AtomicReference<List<GatewayRoute>> routesRef =
            new AtomicReference<>(Collections.emptyList());

    private RouteRegistry() {}

    public static RouteRegistry getInstance() {
        return INSTANCE;
    }

    public List<GatewayRoute> getRoutes() {
        return routesRef.get();
    }

    public void setRoutes(List<GatewayRoute> routes) {
        routesRef.set(routes == null ? Collections.emptyList() : List.copyOf(routes));
    }
}
