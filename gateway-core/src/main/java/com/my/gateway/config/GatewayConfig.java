package com.my.gateway.config;

import com.my.gateway.context.GatewayRoute;
import lombok.Data;

import java.util.List;

@Data
public class GatewayConfig {
    // 默认端口 8080
    private int port = 8080;

    // 应用名称
    private String appName = "my-gateway";

    // 路由列表
    private List<GatewayRoute> routes;
}