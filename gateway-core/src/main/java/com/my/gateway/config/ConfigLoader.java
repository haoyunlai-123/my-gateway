package com.my.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
public class ConfigLoader {

    private static final ConfigLoader INSTANCE = new ConfigLoader();
    private GatewayConfig config;

    private ConfigLoader() {
    }

    public static ConfigLoader getInstance() {
        return INSTANCE;
    }

    /**
     * 加载配置
     * 优先从 classpath 下读取 gateway.yaml
     */
    public GatewayConfig loadConfig() {
        if (config != null) {
            return config;
        }

        try {
            InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("gateway.yaml");
            if (inputStream == null) {
                log.warn("gateway.yaml not found, using default config.");
                return new GatewayConfig(); // 返回默认配置
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            config = mapper.readValue(inputStream, GatewayConfig.class);

            log.info("Configuration loaded success. Port: {}, Routes: {}", config.getPort(), config.getRoutes().size());
            return config;

        } catch (Exception e) {
            log.error("Failed to load gateway config", e);
            throw new RuntimeException("Init Config Failed", e);
        }
    }

    public GatewayConfig getConfig() {
        return config;
    }
}