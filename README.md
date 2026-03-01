
------

# My-Gateway

> 基于 Netty + AsyncHttpClient 实现的高性能可扩展 HTTP 网关系统
> 支持动态路由、平滑加权轮询、熔断、重试、指标统计与热更新

------

# 项目简介

My-Gateway 是一个从零实现的自研 HTTP 网关系统，核心目标是：

- 理解网关底层实现原理
- 实践中间件级架构设计
- 实现可扩展、可观测、可热更新的网关系统

该项目采用 **Netty NIO 模型 + 异步 HTTP 转发**，并设计了完整的：

- 路由系统
- 负载均衡系统
- 熔断机制
- 自动重试
- 指标统计
- 热更新机制

------

# 整体架构

```
                 ┌──────────────────────┐
                 │     Netty Server     │
                 └─────────┬────────────┘
                           │
                    GatewayContext
                           │
                DefaultGatewayFilterChain
                           │
   ┌─────────────┬──────────────┬──────────────┬────────────┐
   │ Monitor     │ RouteSetup   │ LoadBalance  │ RouteProxy │
   │ Filter      │ Filter       │ Filter       │ Filter     │
   └─────────────┴──────────────┴──────────────┴────────────┘
                           │
                    AsyncHttpClient
                           │
                     Downstream Service
```

------

# 模块结构

```
my-gateway
 ├── gateway-admin
 ├── gateway-client
 ├── gateway-common
 └── gateway-core
      ├── bootstrap      启动 & 热更新
      ├── config         配置解析
      ├── container      路由快照管理
      ├── context        请求上下文模型
      ├── filter         责任链过滤器
      ├── health         熔断与健康管理
      ├── loadbalance    负载均衡算法
      ├── metrics        指标统计
      └── netty          网络层实现
```

------

#  核心功能

------

## 动态路由系统

- 支持 YAML 配置路由
- 支持 path 前缀匹配
- 支持多 upstream 节点
- 支持权重配置

采用：

```
AtomicReference<List<Route>>
```

实现 **Copy-On-Write 路由快照模型**

优势：

- 读操作无锁
- 更新原子替换
- 热更新不影响正在执行的请求

------

## 过滤器责任链

采用责任链模式：

```
MonitorFilter
RouteSetupFilter
LoadBalanceFilter
RouteFilter
```

特点：

- 可扩展
- 可插拔
- 易于添加限流 / 鉴权 / 日志 / tracing

------

## 负载均衡算法

已实现：

- Random
- RoundRobin
- ConsistentHash
- SmoothWeightedRoundRobin（平滑加权轮询）

### 平滑加权轮询算法（SWRR）

核心逻辑：

```
current += weight
选 current 最大
selected.current -= totalWeight
```

优势：

- 流量平滑
- 无突刺
- 接近 Nginx 行为

------

## 被动健康检查 + 熔断

状态机：

```
CLOSED → OPEN → HALF_OPEN → CLOSED
```

触发条件：

- 连接异常
- 5xx 响应
- 连续失败超过阈值

特性：

- 自动摘除异常节点
- 半开自动恢复
- 无需主动探活

------

## 自动重试机制

支持：

- 最大重试次数
- 指定可重试状态码（如 502/503/504）
- 切换其他健康节点
- 与熔断联动

流程：

```
请求失败
  ↓
记录健康状态
  ↓
选择新 upstream
  ↓
延迟重试
```

------

## 指标统计系统

统计维度：

- route 级别
- upstream 级别

统计内容：

- 请求总数
- 成功数 / 失败数
- 4xx / 5xx
- 平均耗时
- 最大耗时
- p50 / p90 / p99 近似分位

实现方式：

- ConcurrentHashMap
- LongAdder
- 轻量级直方图

访问：

```
GET /metrics
```

------

## 路由热更新

实现方式：

- JDK WatchService
- 或文件轮询模式
- ConfigLoader.reload()
- 原子替换路由表

特性：

- 无需重启
- 无需注册中心
- 更新失败保持旧配置

启动方式：

```
-Dgateway.config=E:\...\gateway.yaml
```

------

#  启动方式

### 指定外部配置文件

```
java -Dgateway.config=E:\path\gateway.yaml -jar gateway-core.jar
```

###  访问示例

```
http://localhost:9000/json
http://localhost:9000/metrics
http://localhost:9000/routes
```

------

#  设计亮点

###  Copy-On-Write 路由发布模型

- 原子替换
- 无锁读
- 热更新安全

------

###  平滑加权轮询实现

- 接近 Nginx 行为
- 权重流量平滑

------

###  被动熔断机制

- 自动摘除异常节点
- 半开自动恢复
- 不依赖注册中心

------

### 重试与熔断联动

- 异常节点不再参与负载
- 自动切换健康节点

------

###  原子指标统计

- 不阻塞主链路
- 支持高并发

------

#  性能特点

- Netty NIO 模型
- AsyncHttpClient 异步转发
- 非阻塞 I/O
- 低锁争用
- 高吞吐

------

# 后续可扩展方向

- 限流（令牌桶 / 漏桶）
- 鉴权模块（JWT）
- 灰度发布
- 熔断统计窗口化
- Prometheus 集成
- OpenTelemetry 接入

------

#  简历写法参考

> 自研高性能 HTTP 网关系统（Netty + AsyncHttpClient）
>
> - 实现动态路由匹配与 Copy-On-Write 路由快照模型
> - 实现平滑加权轮询与一致性哈希负载均衡算法
> - 实现被动健康检查与熔断机制（CLOSED/OPEN/HALF_OPEN）
> - 支持自动重试与多节点容错
> - 实现运行时 YAML 热更新（WatchService + 原子替换）
> - 实现指标统计与可观测接口（QPS、失败率、RT）

------

# 技术栈

- Java 17
- Netty
- AsyncHttpClient
- Jackson (YAML)
- Maven

------

#  项目定位

该项目定位为：

> 中间件级工程实践项目

重点体现：

- 并发控制能力
- 架构设计能力
- 算法实现能力
- 可观测设计能力
- 高可用设计能力

------

# 作者

自研项目，用于深入理解网关系统底层原理与架构设计。


