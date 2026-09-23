# z-wf

> **基于 Camunda 7 的工作流引擎**, 支持流程定义 + 任务管理 + 审批中心。
> 通过 **z-config** 注册到服务注册中心, 通过 **z-rpc** 对外暴露流程服务接口。
> 业务方一行 Spring Boot Starter 集成, 钩子接口 (WfProcessHook / WfTaskHook / WfNotificationHook) 支持本地/HTTP/RPC 多模式扩展。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-6DB33F)](https://spring.io)
[![Camunda](https://img.shields.io/badge/Camunda-7.18-FF6F00)](https://camunda.com)

---

## 🚀 5 分钟接入

### 方式一：作为 Spring Boot 应用 (推荐)

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-wf-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

`application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/camunda?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
camunda:
  bpm:
    database:
      schema-update: true   # 首次启动自动建表
    auto-deployment-enabled: true
```

启动类:
```java
@SpringBootApplication
public class WfApplication {
    public static void main(String[] args) {
        SpringApplication.run(WfApplication.class, args);
    }
}
```

启动后:
- REST API: `http://localhost:8080/api/...`
- Knife4j: `http://localhost:8080/doc.html`

> 独立部署形态 (不嵌入业务方) 见 `z-wf-admin` 模块: 自带管理前端, `mvn -pl z-wf-admin spring-boot:run` 或 Docker 启动。

### 方式二：嵌入 z-config (服务注册) + z-rpc (远程调用)

```xml
<!-- 工作流引擎 -->
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-wf-starter</artifactId>
    <version>1.0.4</version>
</dependency>

<!-- 配置中心 + 注册中心 -->
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-config-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- RPC 框架 -->
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-rpc-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

```yaml
z:
  config:
    enabled: true
    server-addr: localhost:8848       # z-config server 地址
    namespace: production
  rpc:
    enabled: true
    protocol: zrpc
    registry:
      type: z-config                  # 用 z-config 当注册中心
      address: localhost:8848
    service: z-wf                     # 注册到 z-config 的服务名
```

启动后:
- z-wf 自动以服务名 `z-wf` 注册到 z-config (ip + port)
- 暴露 RPC 接口 `WfProcessService` / `WfTaskService` / `WfNotificationService`
- 其他服务通过 `@ZRpcReference` 引用即可远程调用

### 方式三：通过钩子接口扩展 (本地 / HTTP / RPC)

```java
// 1. 本地模式: 直接调用 service
@Bean
public WfProcessHook localWfHook() {
    return new WfProcessHook() {
        @Override
        public boolean onBeforeStart(String processKey, Map<String, Object> vars) {
            log.info("流程 {} 启动前校验", processKey);
            return true;
        }
    };
}

// 2. HTTP 模式: 调 REST API
@Bean
public WfProcessHook httpWfHook(@Value("${z-wf.url}") String baseUrl) {
    return new HttpWfProcessHook(baseUrl);
}

// 3. RPC 模式: 通过 z-rpc 调用
@Bean
public WfProcessHook rpcWfHook(@Autowired WfProcessService rpcService) {
    return new RpcWfProcessHook(rpcService);
}
```

---

## 📦 模块结构

> groupId: `io.github.yuku123` · version: **1.0.4**

| 模块 | 说明 | 何时该引入 |
|---|---|---|
| `z-wf-core` | Camunda 封装 + 钩子接口 (WfProcessHook / WfTaskHook / WfNotificationHook) | 客户端调用 / 嵌入使用 |
| `z-wf-web` | REST API + DTO + Controller (Process / Task / Approval) | Web 层 |
| `z-wf-starter` | 自动装配 starter (core + web + 可选 z-config/z-rpc 集成) | Spring Boot 应用嵌入 |
| `z-wf-admin` | 独立可启动应用 (自带管理前端, 不发 Central) | 独立部署 / Docker 镜像 |

---

## ✨ 核心能力

- ✅ **流程定义** — BPMN 2.0 (Camunda Modeler 可视化编辑)
- ✅ **任务管理** — 启动流程 / 完成任务 / 委派 / 跳转 / 回退
- ✅ **审批中心** — 待办 / 已办 / 我的发起 / Dashboard 统计
- ✅ **钩子扩展** — 3 种钩子 (流程 / 任务 / 通知), 3 种调用模式 (本地 / HTTP / RPC)
- ✅ **REST API** — 9 个 Controller, 完整覆盖流程生命周期
- ✅ **自带管理前端** — LogicFlow 流程设计 + 审批页面 (z-wf-admin, 业务方可替换为自己的页面)
- ✅ **API 文档** — Knife4j (Swagger 3) 集成
- ✅ **服务注册** — 一行接入 z-config, 自动注册 `z-wf` 实例
- ✅ **RPC 暴露** — 通过 z-rpc 注解 `@ZRpcService` 把 service 暴露给远程调用方

---

## 🏗️ 项目结构

```
z-wf/
├── pom.xml                          # 自给自足 parent (1.0.4)
├── z-wf-core/                       # 核心模块 + 钩子接口 ✅
├── z-wf-web/                        # REST API + DTO + Controller ✅
├── z-wf-starter/                    # 自动装配 starter (可选 z-config/z-rpc 集成) ✅
│   └── src/main/resources/
│       └── processes/
│           ├── leaveProcess.bpmn          # 请假流程示例
│           └── fiveLookEvaluation.bpmn    # 五看评估流程示例
├── z-wf-admin/                      # 独立可启动应用 (不发 Maven Central) ✅
│   └── src/main/resources/
│       └── static/                       # Z-WF 自带管理前端
├── Dockerfile                       # Docker 镜像构建 (Eclipse Temurin 8 JRE)
├── docker-compose.yml               # 三服务编排 (z-config + z-wf + z-wf-client)
├── start.sh                         # 本地一键启停
├── install-settings.sh              # 写入 Maven Central 凭证到 ~/.m2
├── deploy_maven_center.sh           # 发布到 Maven Central
└── README.md
```

---

## 🐳 Docker 部署 (三服务编排)

```bash
cd z-wf
docker compose up -d

# 验证三服务都启动
docker compose ps

# 外部访问 z-wf REST API
curl http://localhost:8080/api/wf/health

# z-config 注册中心
curl http://localhost:8848/api/health

# 通过 z-wf-client 调用 RPC (需要先注册到 z-config)
curl http://localhost:8081/wf-client/api/call/start?processKey=leaveProcess
```

### 三服务架构

```
┌────────────────────────────────────────────────────────────┐
│  z-config-server (port 8848)                                │
│  ─ Netty + MySQL + namespace 隔离                           │
│  ─ 服务注册中心 + 配置中心                                  │
└────────────────────────────────────────────────────────────┘
          ▲              ▲              ▲
          │ register     │ discover     │
          │              │              │
┌────────────────────────────────────────────────────────────┐
│  z-wf-server (port 8080)                                    │
│  ─ z-wf-admin (Camunda 引擎 + REST API + 管理前端)            │
│  ─ 启动时 ZNamingService.registerInstance("z-wf", ...)     │
│  ─ @ZRpcService 暴露 WfProcessService                       │
└────────────────────────────────────────────────────────────┘
          ▲
          │ RPC call (Netty + Hessian2)
          │
┌────────────────────────────────────────────────────────────┐
│  z-wf-client (port 8081)                                    │
│  ─ z-config-spring-boot-starter (订阅 z-wf 实例)            │
│  ─ z-rpc-spring-boot-starter (RPC 客户端)                  │
│  ─ @ZRpcReference(WfProcessService.class) 远程调用          │
└────────────────────────────────────────────────────────────┘
```

---

## 🧪 集成测试

集成测试位于 [`z-opc/z-middleware-integration-test`](../z-opc/z-middleware-integration-test/)：

| 测试 | 说明 | 状态 |
|---|---|---|
| `ZWfMavenCentralPullIT` | z-wf 3 个模块从 Maven Central 拉取 + POM 元信息校验 | ✅ |
| `ZWfConfigRpcIntegrationIT` | z-wf + z-config + z-rpc 三方联调 (启动 + 注册 + RPC 调用) | ✅ |

```bash
cd z-opc/z-middleware-integration-test

# 1. 仅验证 z-wf artifact 拉取 (不依赖服务)
RUN_ZWF_CENTRAL_IT=true mvn test -Dtest=ZWfMavenCentralPullIT

# 2. 完整三方联调 (需要 z-config server 跑起来)
RUN_ZWF_RPC_IT=true ZCONFIG_SERVER=127.0.0.1:8848 \
  mvn test -Dtest=ZWfConfigRpcIntegrationIT
```

---

## 🔧 高级配置

### 自定义 BPMN 流程

把你的 BPMN 文件放到 `src/main/resources/processes/` 目录, Camunda 启动时会自动部署。
或者在 application.yml 里指定:

```yaml
camunda.bpm:
  deployment-resources:
    - classpath:processes/leaveProcess.bpmn
    - classpath:processes/customFlow.bpmn
```

### 与 z-rpc 暴露流程服务

```java
@ZRpcService(interfaceClass = WfProcessService.class, group = "default", version = "1.0.0")
public class WfProcessServiceImpl implements WfProcessService {
    @Autowired private RuntimeService runtimeService;

    @Override
    public String startProcess(String processKey, Map<String, Object> vars) {
        return runtimeService.startProcessInstanceByKey(processKey, vars).getId();
    }
}
```

### 与 z-config 做服务发现

```java
@Autowired private ZNamingService znaming;

// 业务方拿 z-wf 实例
List<ZNamingInstance> instances = znaming.getAllInstances("z-wf");
ZNamingInstance one = znaming.selectOneHealthyInstance("z-wf");
```

---

## 🤝 贡献

```bash
mvn clean install                              # 安装到本地 .m2
mvn -pl z-wf-starter package -DskipTests       # 单独打 starter 包 (纯库)
mvn -pl z-wf-admin -am package -DskipTests     # 打可执行 fat jar (z-wf-admin-*-exec.jar)
bash deploy_maven_center.sh publish            # 发布到 Maven Central (本地终端跑, admin 不会发布)
```

---

## 📄 许可证

[Apache License 2.0](LICENSE)

---

## 🔗 相关项目

| 项目 | 关系 |
|---|---|
| [z-config](https://github.com/yuku123/z-opc-foundation) | **z-wf 的官方注册中心**, 一行接入做服务发现 |
| [z-rpc](https://github.com/yuku123/z-opc-foundation) | **z-wf 的官方 RPC 框架**, 远程调用流程服务 |
| [z-boot](https://github.com/yuku123/z-opc-foundation) | Spring Boot Starter 聚合 + BOM |
| [z-cache](https://github.com/yuku123/z-opc-foundation) | 同系列 — 分布式缓存 |
| [z-mq](https://github.com/yuku123/z-opc-foundation) | 同系列 — 分布式消息队列 |

---

## 📮 联系

- GitHub: [yuku123/z-opc-foundation](https://github.com/yuku123/z-opc-foundation)
- Email: yuku123@users.noreply.github.com


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)
  - [`install-settings.sh`](_doc/003_script/install-settings.sh)
  - [`z-wf-client.sh`](_doc/003_script/z-wf-client.sh)

各文档详细说明见各子目录。
