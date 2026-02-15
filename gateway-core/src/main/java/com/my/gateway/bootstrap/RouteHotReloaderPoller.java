package com.my.gateway.bootstrap;

import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.container.RouteManager;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class RouteHotReloaderPoller implements Runnable {

    private final File configFile;
    private final long intervalMs;

    private volatile long lastModified = -1;

    public RouteHotReloaderPoller(String configFilePath, long intervalMs) {
        this.configFile = new File(configFilePath);
        this.intervalMs = Math.max(200, intervalMs);
    }

    @Override
    public void run() {
        log.info("[RouteHotReload] poller started, file={}, intervalMs={}",
                configFile.getAbsolutePath(), intervalMs);

        while (true) {
            try {
                if (!configFile.exists() || !configFile.isFile()) {
                    log.warn("[RouteHotReload] config file not found: {}", configFile.getAbsolutePath());
                } else {
                    long lm = configFile.lastModified();
                    if (lastModified < 0) {
                        lastModified = lm;
                    } else if (lm != lastModified) {
                        lastModified = lm;

                        GatewayConfig cfg = ConfigLoader.getInstance().reload();
                        RouteManager.getInstance().refresh(cfg.getRoutes());

                        log.info("[RouteHotReload] reloaded routes, size={}",
                                cfg.getRoutes() == null ? 0 : cfg.getRoutes().size());
                    }
                }
                Thread.sleep(intervalMs);
            } catch (Exception e) {
                // 不要退出线程，继续轮询
                log.error("[RouteHotReload] reload failed (keep old routes). err={}", e.toString());
                try { Thread.sleep(intervalMs); } catch (InterruptedException ignored) {}
            }
        }
    }
}
