package com.my.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class ConfigLoader {

    private static final ConfigLoader INSTANCE = new ConfigLoader();

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // 注意：这里有缓存
    private volatile GatewayConfig config;

    // 启动时配置文件真实路径（优先使用 -Dgateway.config=xxx）
    private volatile Path configPath;

    private ConfigLoader() {}

    public static ConfigLoader getInstance() {
        return INSTANCE;
    }

    public synchronized GatewayConfig loadConfig() {
        if (config != null) return config;

        // 1) 优先从 JVM 参数读取真实文件路径
        String path = System.getProperty("gateway.config");
        if (path != null && !path.isBlank()) {
            configPath = Path.of(path);
            config = loadFromFile(configPath);
            return config;
        }

        // 2) 否则退化为 classpath
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("gateway.yaml")) {
            if (in == null) {
                log.warn("gateway.yaml not found in classpath, using default config.");
                config = new GatewayConfig();
                return config;
            }
            config = mapper.readValue(in, GatewayConfig.class);
            log.info("Configuration loaded (classpath). Port: {}, Routes: {}",
                    config.getPort(), config.getRoutes() == null ? 0 : config.getRoutes().size());
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Init Config Failed", e);
        }
    }

    /** 热更新专用：强制从文件 reload（清缓存） */
    public synchronized GatewayConfig reload() {
        if (configPath == null) {
            // 如果启动时走的 classpath，这里无法保证热更新正确，直接提示
            log.warn("reload skipped: configPath is null (you are using classpath config). " +
                    "Please start with -Dgateway.config=E:\\...\\gateway.yaml");
            return config;
        }
        config = loadFromFile(configPath);
        return config;
    }

    public GatewayConfig getConfig() {
        return config;
    }

    public Path getConfigPath() {
        return configPath;
    }

    private GatewayConfig loadFromFile(Path path) {
        Objects.requireNonNull(path, "config file path is null");
        try (InputStream in = Files.newInputStream(path)) {
            GatewayConfig cfg = mapper.readValue(in, GatewayConfig.class);
            log.info("Configuration loaded (file). Port: {}, Routes: {} , file={}",
                    cfg.getPort(), cfg.getRoutes() == null ? 0 : cfg.getRoutes().size(), path.toAbsolutePath());
            return cfg;
        } catch (Exception e) {
            log.error("Failed to load gateway config from file: {}", path.toAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }
}
