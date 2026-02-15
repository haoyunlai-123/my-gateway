package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.UpstreamInstance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smooth Weighted Round Robin (Nginx 同款思路)
 *
 * 每个实例维护 currentWeight：
 * 1) 每轮对所有实例 current += weight
 * 2) 选出 current 最大的实例作为本次结果
 * 3) 被选中实例 current -= totalWeight
 */
public class SmoothWeightedRoundRobinLoadBalancer {

    private static class Node {
        final String url;
        final int weight;
        int current;

        Node(String url, int weight) {
            this.url = url;
            this.weight = Math.max(1, weight);
            this.current = 0;
        }
    }

    // routeId -> (url -> Node)
    private final Map<String, Map<String, Node>> state = new ConcurrentHashMap<>();

    public String choose(List<UpstreamInstance> upstreams, GatewayContext ctx) {
        String routeId = (ctx.getRoute() != null && ctx.getRoute().getId() != null)
                ? ctx.getRoute().getId()
                : "default";

        Map<String, Node> nodes = state.computeIfAbsent(routeId, k -> new ConcurrentHashMap<>());

        // 同步一下节点集合（支持配置热更新时增删）
        // 1) 加入新节点
        for (UpstreamInstance ins : upstreams) {
            nodes.computeIfAbsent(ins.getUrl(), u -> new Node(ins.getUrl(), ins.getWeight()));
        }
        // 2) 移除已不存在节点
        nodes.keySet().removeIf(url -> upstreams.stream().noneMatch(i -> i.getUrl().equals(url)));

        int total = 0;
        Node best = null;

        //  关键：这里需要串行更新 current，否则并发下会乱
        synchronized (nodes) {
            for (UpstreamInstance ins : upstreams) {
                Node n = nodes.get(ins.getUrl());
                // 如果权重配置变了，简单处理：重建节点
                if (n == null || n.weight != Math.max(1, ins.getWeight())) {
                    nodes.put(ins.getUrl(), new Node(ins.getUrl(), ins.getWeight()));
                    n = nodes.get(ins.getUrl());
                }

                n.current += n.weight;
                total += n.weight;

                if (best == null || n.current > best.current) {
                    best = n;
                }
            }

            if (best == null) {
                return upstreams.get(0).getUrl();
            }
            best.current -= total;
            return best.url;
        }
    }
}
