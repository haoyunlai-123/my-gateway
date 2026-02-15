package com.my.gateway.netty;

import com.my.gateway.context.GatewayContext;
import com.my.gateway.context.GatewayRequest;
import com.my.gateway.context.GatewayResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // 1. 获取客户端 IP (简单的获取方式)
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = socketAddress.getAddress().getHostAddress();

        // 2. 构建核心上下文 GatewayContext
        // 注意：这里必须 retain 一下 msg，因为 SimpleChannelInboundHandler 会自动 release，
        // 而 GatewayContext 需要持有它直到处理结束。
        // 但为了简单演示，暂且认为 Context 生命周期就在这个方法内结束，先不 retain，
        // 后面引入异步 Filter 时需要特别注意引用计数问题。
        GatewayRequest request = new GatewayRequest(clientIp, msg);
        GatewayContext gatewayContext = new GatewayContext(ctx, request);

        try {
            // ============================================
            // 核心：获取并执行过滤器链
            // ============================================
            com.my.gateway.filter.FilterFactory.getInstance()
                    .buildFilterChain()
                    .doFilter(gatewayContext);

            // ========================================================
            // 核心修改：删除了 writeResponse(gatewayContext)
            // ========================================================
            // 因为现在的逻辑是：
            // 1. 如果走到 RouteFilter，由 RouteFilter 的 callback 异步触发 writeResponse
            // 2. 如果中间某个 Filter 拦截了（比如鉴权失败），那个 Filter 必须自己调用 ctx.writeResponse()


        } catch (Exception e) {

            e.printStackTrace();
            // 发生异常，返回 500
            GatewayResponse errResponse = gatewayContext.getResponse();
            errResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            errResponse.setJsonContent("{\"error\": \"" + e.getMessage() + "\"}");
            gatewayContext.writeResponse();

        } finally {
            // 4. 确保资源释放 (虽然后面 writeResponse 会消耗掉，但加个保险是个好习惯)
            // 在这里如果是 SimpleChannelInboundHandler，它会在方法返回后自动 release msg。
            // 如果把 msg 传给了异步线程，这里就不能用 SimpleChannelInboundHandler 了。
        }
    }

    // 模拟业务逻辑
    private void handleBiz(GatewayContext context) {
        log.info("处理请求路径: " + context.getRequest().getPath());
        log.info("请求参数: " + context.getRequest().getParameters());

        GatewayResponse response = context.getResponse();
        response.setStatus(HttpResponseStatus.OK);
        response.setJsonContent("{\"message\": \"Context Refactor Success\", \"path\": \"" + context.getRequest().getPath() + "\"}");
    }

    /*// 模拟 Response 写回逻辑
    private void writeResponse(GatewayContext context) {
        GatewayResponse gatewayResponse = context.getResponse();
        FullHttpRequest nettyRequest = context.getRequest().getFullHttpRequest();

        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                gatewayResponse.getStatus(),
                Unpooled.copiedBuffer(gatewayResponse.getContent(), CharsetUtil.UTF_8)
        );

        // 设置头信息
        httpResponse.headers().add(gatewayResponse.getHeaders());
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        // 处理 Keep-Alive
        boolean keepAlive = HttpUtil.isKeepAlive(nettyRequest);
        if (keepAlive) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            context.getNettyCtx().writeAndFlush(httpResponse);
        } else {
            context.getNettyCtx().writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }*/
}