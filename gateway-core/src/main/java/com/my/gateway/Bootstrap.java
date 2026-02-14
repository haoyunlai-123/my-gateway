package com.my.gateway;

import com.my.gateway.netty.NettyHttpServer;

public class Bootstrap {
    public static void main(String[] args) {
        // 定义端口，以后从配置文件读取
        int port = 8080;

        NettyHttpServer server = new NettyHttpServer(port);
        server.start();
    }
}
