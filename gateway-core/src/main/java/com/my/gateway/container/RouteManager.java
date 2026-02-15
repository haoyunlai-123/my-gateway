package com.my.gateway.container;

import com.my.gateway.context.GatewayRoute;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RouteManager {

    private static final RouteManager INSTANCE = new RouteManager();

    private final AtomicReference<RouteTable> ref = new AtomicReference<>(RouteTable.empty());

    private RouteManager() {}

    public static RouteManager getInstance() {
        return INSTANCE;
    }

    public RouteTable current() {
        return ref.get();
    }

    public void refresh(List<GatewayRoute> routes) {
        ref.set(new RouteTable(routes));
    }
}
