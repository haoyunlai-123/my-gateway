package com.my.gateway.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpstreamInstance {
    private String url;
    private int weight = 1; // 默认权重 1
}
