package com.my.gateway.context;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class GatewayContext {

    // Netty 的连接上下文，用于写回响应
    private final ChannelHandlerContext nettyCtx;

    private final GatewayRequest request;

    private GatewayResponse response;

    // 当前请求匹配到的路由信息
    private GatewayRoute route;

    //标识位，标识是否已经写回响应，防止重复写入
    private boolean written = false;

    // 选中的上游服务地址（可以在路由匹配阶段设置）
    private String selectedUpstream;

    // 记录已经尝试过的上游服务地址，避免重试同一个地址
    private final Set<String> triedUpstreams = new java.util.HashSet<>();

    // 请求开始时间
    private long startNano;

    // 最终响应码（重试后最终结果）
    private int finalStatusCode;

    // 最终命中的 upstream（重试后最终）
    private String finalUpstream;


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

    /**
     * 核心方法：将最终结果写回客户端
     * 这个方法可以在任何线程中被调用（Netty 线程安全）
     */
    public void writeResponse() {
        if (written) {
            return; // 避免重复写入
        }
        written = true;

        if (response == null) {
            response = new GatewayResponse();
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.setJsonContent("{\"error\": \"No Response Generated\"}");
        }

        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                response.getStatus(),
                Unpooled.copiedBuffer(response.getContent() == null ? "" : response.getContent(), CharsetUtil.UTF_8)
        );

        httpResponse.headers().add(response.getHeaders());
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        // 处理 Keep-Alive
        boolean keepAlive = HttpUtil.isKeepAlive(request.getFullHttpRequest());
        if (keepAlive) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            nettyCtx.writeAndFlush(httpResponse);
        } else {
            nettyCtx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        }

        // 释放资源
        release();
    }
}
