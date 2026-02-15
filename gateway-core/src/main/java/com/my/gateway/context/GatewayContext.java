package com.my.gateway.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GatewayContext {

    // Netty 的连接上下文，用于写回响应
    private final ChannelHandlerContext nettyCtx;

    private final GatewayRequest request;

    private GatewayResponse response;

    private GatewayRoute route; // 当前请求匹配到的路由信息

    // 可以在这里加一个 generic 容器用来在 Filter 间传递参数
    // private Map<String, Object> attributes = new ConcurrentHashMap<>();

    // 构造函数
    public GatewayContext(ChannelHandlerContext nettyCtx, GatewayRequest request) {
        this.nettyCtx = nettyCtx;
        this.request = request;
        this.response = new GatewayResponse(); // 默认初始化一个空响应
    }

    /**
     * 释放资源（非常重要！）
     * Netty 的 ByteBuf 是引用计数的，必须手动释放，否则内存泄漏
     */
    public void release() {
        if (this.request.getFullHttpRequest() != null) {
            ReferenceCountUtil.release(this.request.getFullHttpRequest());
        }
    }
}
