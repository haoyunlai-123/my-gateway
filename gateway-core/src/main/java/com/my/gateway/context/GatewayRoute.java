package com.my.gateway.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // 新增：Jackson反序列化需要
@AllArgsConstructor // 新增：Builder需要
public class GatewayRoute {
    // 路由ID
    private String id;
    // 匹配路径，例如 /api/user
    private String path;
    // 服务名称 (后续注册中心用)
    private String serviceId;
    // 后端具体的 URL，例如 http://localhost:8081
    private String backendUrl;
}