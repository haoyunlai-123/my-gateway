package com.my.gateway.context;

import io.netty.handler.codec.http.*;
import lombok.Data;

@Data
public class GatewayResponse {

    // 响应头
    private HttpHeaders headers = new DefaultHttpHeaders();

    // 响应状态码，默认 200 OK
    private HttpResponseStatus status = HttpResponseStatus.OK;

    // 响应内容 (JSON 字符串等)
    private String content;

    /**
     * 设置 Content-Type 为 JSON
     */
    public void setJsonContent(String content) {
        this.content = content;
        this.headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    }

    /**
     * 设置 Content-Type 为 Plain Text
     */
    public void setHtmlContent(String content) {
        this.content = content;
        this.headers.set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
    }
}
