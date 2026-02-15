package com.my.gateway.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GatewayRoute {
    private String id;          // 路由ID
    private String path;        // 匹配路径，例如 /api/user
    private String serviceId;   // 服务名称 (后续注册中心用)
    private String backendUrl;  // 后端具体的 URL，例如 http://localhost:8081
}
