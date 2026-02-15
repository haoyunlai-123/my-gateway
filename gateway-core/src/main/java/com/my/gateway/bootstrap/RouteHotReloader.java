package com.my.gateway.bootstrap;

import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.container.RouteManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;

@Slf4j
public class RouteHotReloader implements Runnable {

    private final Path yamlPath;

    public RouteHotReloader(String yamlFilePath) {
        this.yamlPath = Paths.get(yamlFilePath).toAbsolutePath().normalize();
    }

    @Override
    public void run() {
        Path dir = yamlPath.getParent();
        if (dir == null) {
            log.warn("[RouteHotReload] invalid path: {}", yamlPath);
            return;
        }

        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            dir.register(ws,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            log.info("[RouteHotReload] watching: {}", yamlPath);

            while (true) {
                WatchKey key = ws.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Path changed = dir.resolve((Path) event.context()).toAbsolutePath().normalize();
                    if (!changed.equals(yamlPath)) continue;

                    // 很多编辑器会触发多次事件，做个轻微防抖
                    try { Thread.sleep(80); } catch (InterruptedException ignored) {}

                    try {
                        GatewayConfig cfg = ConfigLoader.getInstance().reload();
                        RouteManager.getInstance().refresh(cfg.getRoutes());
                        log.info("[RouteHotReload] reloaded routes, size={}", cfg.getRoutes() == null ? 0 : cfg.getRoutes().size());
                    } catch (Exception e) {
                        log.error("[RouteHotReload] reload failed, keep old routes. err={}", e.toString());
                    }
                }
                boolean valid = key.reset();
                if (!valid) break;
            }
        } catch (Exception e) {
            log.error("[RouteHotReload] watcher stopped: {}", e.toString());
        }
    }
}
