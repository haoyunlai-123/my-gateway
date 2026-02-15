package com.my.gateway.netty;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;

/**
 * AsyncHttpClient工具类，单例模式，用于发送异步HTTP请求
 */
public class AsyncHttpHelper {

    private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
    private final AsyncHttpClient asyncHttpClient;

    private AsyncHttpHelper() {
        // 配置 HttpClient
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(new io.netty.channel.nio.NioEventLoopGroup(2)) // 独立线程池处理请求
                .setConnectTimeout(2000)
                .setRequestTimeout(3000)
                .setMaxConnections(10000);

        this.asyncHttpClient = Dsl.asyncHttpClient(builder);
    }

    public static AsyncHttpHelper getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<Response> executeRequest(Request request) {
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
        return future.toCompletableFuture();
    }
}