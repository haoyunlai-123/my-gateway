package com.my.gateway.context;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网关请求上下文
 * 这个类封装了 Netty 的 FullHttpRequest，并提供了便捷的方法来访问请求的各个部分。
 * 设计时考虑了后续可能需要扩展的属性，比如客户端 IP、请求体等。
 */
@Getter
public class GatewayRequest {

    // 存储 Netty 原生请求，用于后续获取 Body 等
    private final FullHttpRequest fullHttpRequest;

    // 核心属性
    private final String uri;
    private final String path;
    private final HttpMethod method;
    private final Map<String, List<String>> parameters; // GET 参数
    private final HttpHeaders headers;
    private final Set<Cookie> cookies;
    private final String clientIp; // 客户端 IP (这步先预留，后面通过 Handler 获取)

    // 字符集，默认为 UTF-8
    private final Charset charset;

    public GatewayRequest(String clientIp, FullHttpRequest fullHttpRequest) {
        this.clientIp = clientIp;
        this.fullHttpRequest = fullHttpRequest;
        this.uri = fullHttpRequest.uri();
        this.method = fullHttpRequest.method();
        this.headers = fullHttpRequest.headers();
        this.charset = HttpUtil.getCharset(fullHttpRequest, CharsetUtil.UTF_8);

        // 解析 URL 参数 (QueryStringDecoder 是 Netty 提供的神器)
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri, charset);
        this.path = queryStringDecoder.path();
        this.parameters = queryStringDecoder.parameters();

        // 解析 Cookie
        String cookieString = headers.get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            this.cookies = ServerCookieDecoder.STRICT.decode(cookieString);
        } else {
            this.cookies = new java.util.HashSet<>();
        }
    }

    /**
     * 获取请求体内容（JSON字符串）
     * 注意：POST 请求通常需要读取 Body
     */
    public String getBody() {
        ByteBuf content = fullHttpRequest.content();
        return content.toString(charset);
    }
}
