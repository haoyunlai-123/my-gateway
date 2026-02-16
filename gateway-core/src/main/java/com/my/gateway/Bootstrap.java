package com.my.gateway;

import com.my.gateway.bootstrap.RouteHotReloaderPoller;
import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.config.RouteRegistry;
import com.my.gateway.container.RouteManager;
import com.my.gateway.netty.NettyHttpServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bootstrap {

    /**
     * 热更新轮询版
     * @param args
     */
    /*public static void main1(String[] args) {
        String configPath = System.getProperty("gateway.config");

        // 1) 设置外部配置路径
        if (configPath != null && !configPath.isBlank()) {
            ConfigLoader.getInstance().setConfigFilePath(configPath);
        }

        // 2) 启动时 reload 一次
        GatewayConfig cfg = ConfigLoader.getInstance().reload();
        RouteManager.getInstance().refresh(cfg.getRoutes());

        // 3) 启动热更新线程（轮询版）
        if (configPath != null && !configPath.isBlank()) {
            Thread t = new Thread(new RouteHotReloaderPoller(configPath, 500), "route-hot-reloader");
            t.setDaemon(true);
            t.start();
        } else {
            // 没传外部文件：就没法热更新（classpath 在 jar 里不可写）
            log.warn("[RouteHotReload] -Dgateway.config not set, hot reload disabled (classpath config only)");
        }

        // 4) 启动 Netty Server
        int port = cfg.getPort();
        if (port <= 0) port = 8080; // 兜底

        NettyHttpServer server = new NettyHttpServer(port);
        server.start();
    }*/

    /**
     * 热更新监控版
     * @param args
     */
    public static void main(String[] args) {
        String configPath = System.getProperty("gateway.config");

        // 1) 先加载一次配置
        if (configPath != null && !configPath.isBlank()) {
            ConfigLoader.getInstance().loadConfig();
        }
        var cfg = ConfigLoader.getInstance().reload();

        // 2) 初始化路由表快照
        RouteManager.getInstance().refresh(cfg.getRoutes());
        RouteRegistry.getInstance().setRoutes(cfg.getRoutes());

        // 3) 启动监听（只有外部路径才监听，classpath 文件不好监听）
        if (configPath != null && !configPath.isBlank()) {
            Thread t = new Thread(new com.my.gateway.bootstrap.RouteHotReloader(configPath), "route-hot-reloader");
            t.setDaemon(true);
            t.start();
        }
        // 4) 启动 Netty Server
        int port = cfg.getPort();
        if (port <= 0) port = 8080; // 兜底

        NettyHttpServer server = new NettyHttpServer(port);
        server.start();
    }


}