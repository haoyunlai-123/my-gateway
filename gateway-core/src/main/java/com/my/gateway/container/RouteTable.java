package com.my.gateway.container;

import com.my.gateway.context.GatewayRoute;

import java.util.Collections;
import java.util.List;

public class RouteTable {

    private final List<GatewayRoute> routes;

    public RouteTable(List<GatewayRoute> routes) {
        this.routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public List<GatewayRoute> getRoutes() {
        return routes;
    }

    public static RouteTable empty() {
        return new RouteTable(Collections.emptyList());
    }
}
