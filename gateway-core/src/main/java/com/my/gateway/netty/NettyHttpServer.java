package com.my.gateway.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyHttpServer {

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyHttpServer(int port) {
        this.port = port;
    }

    public void start() {
        // 1. 创建 Boss 和 Worker 线程组
        // Boss 只负责处理连接，Worker 负责读写业务
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    // .option(ChannelOption.SO_BACKLOG, 1024) // 连接队列大小，以后优化再加
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 核心管道配置
                            ch.pipeline().addLast(new HttpServerCodec()); // HTTP 编解码
                            ch.pipeline().addLast(new HttpObjectAggregator(65535)); // HTTP 消息聚合(解决大数据包拆分问题)
                            ch.pipeline().addLast(new NettyHttpServerHandler()); // 自定义业务逻辑
                        }
                    });

            // 2. 启动服务
            ChannelFuture f = b.bind().sync();
            log.info("Gateway Server started on port: {}", port);

            // 3. 阻塞主线程，直到 Channel 关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Gateway start failed", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
