package com.my.gateway.loadbalance;

import com.my.gateway.context.GatewayContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

/**
 * 一致性哈希负载均衡
 *
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {

    @Override
    public String choose(List<String> upstreams, GatewayContext ctx) {
        // 可以换成 userId/header/token 等更业务的 key
        String key = ctx.getRequest().getClientIp() + "|" + ctx.getRequest().getPath();
        int h = hash(key);
        int idx = Math.floorMod(h, upstreams.size());
        return upstreams.get(idx);
    }

    private int hash(String s) {
        CRC32 crc32 = new CRC32();
        crc32.update(s.getBytes(StandardCharsets.UTF_8));
        long v = crc32.getValue();
        return (int) v;
    }
}
