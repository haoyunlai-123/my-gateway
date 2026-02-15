package com.my.gateway.health;

import com.my.gateway.config.ConfigLoader;
import com.my.gateway.config.GatewayConfig;
import com.my.gateway.config.HealthConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务健康管理器
 */
public class PassiveHealthManager {

    private static final PassiveHealthManager INSTANCE = new PassiveHealthManager();

    // key = routeId + "@" + url
    private final Map<String, UpstreamCircuitBreaker> circuits = new ConcurrentHashMap<>();

    private PassiveHealthManager() {}

    public static PassiveHealthManager getInstance() {
        return INSTANCE;
    }

    private UpstreamCircuitBreaker circuit(String routeId, String url) {
        String key = routeId + "@" + url;

        GatewayConfig cfg = ConfigLoader.getInstance().getConfig();
        HealthConfig hc = (cfg != null && cfg.getHealth() != null) ? cfg.getHealth() : new HealthConfig();

        return circuits.computeIfAbsent(key, k ->
                new UpstreamCircuitBreaker(
                        hc.getFailureThreshold(),
                        hc.getOpenCooldownMs(),
                        hc.getHalfOpenMaxCalls(),
                        hc.getSuccessThresholdToClose()
                )
        );
    }

    public boolean tryAcquire(String routeId, String url) {
        return circuit(routeId, url).tryAcquire();
    }

    public void onSuccess(String routeId, String url) {
        circuit(routeId, url).onSuccess();
    }

    public void onFailure(String routeId, String url) {
        circuit(routeId, url).onFailure();
    }
}
