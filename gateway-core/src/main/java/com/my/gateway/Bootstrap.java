package com.my.gateway;

import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.netty.NettyHttpServer;

public class Bootstrap {
    public static void main(String[] args) {
        // 1. 加载配置
        GatewayConfig config = ConfigLoader.getInstance().loadConfig();
        System.out.println("Config Loaded. AppName: " + config.getAppName());

        // 2. 使用配置文件的端口启动
        int port = config.getPort();
        if (port <= 0) port = 8080; // 兜底

        NettyHttpServer server = new NettyHttpServer(port);
        server.start();
    }
}