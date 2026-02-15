package com.my.gateway.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    //  新增：多实例 upstream 列表（本地配置负载均衡）
//    private List<String> upstreams;
    private List<UpstreamInstance> upstreams;

    //  新增：负载均衡策略：random / round_robin / consistent_hash
    private String lb;
}