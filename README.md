# z-camuda

> 基于 **Camunda 7** 的工作流引擎 —— 流程定义 + 任务管理 + 审批中心，通过 SPI 扩展点与钩子接口把审批链路的控制权交回业务方。

一人公司基座里的"审批/流程"中枢：把 Camunda 的 BPMN 引擎封装成一组 REST API 和一个 Spring Boot
Starter，业务方引一条依赖即可嵌入自己的进程；独立部署时由 `z-camuda-admin` 提供自带管理前端。
真正的差异化在**扩展模型**：3 个生命周期钩子接口 + 22 个从 ace 平台蒸馏下来的 SPI 扩展接口
（表单 / 审批 / 流程 / Apex 四大类），业务方实现接口并打上 `@CamudaSpi`，`CamudaSpiRegistry` 启动时扫描、
按 `order` 排序，在流程关键节点串行回调。服务治理侧可一行接入 `z-config` 注册中心、经 `z-rpc` 暴露流程服务。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-camuda`（Workflow Engine） |
| **Maven 坐标** | `io.github.yuku123:z-camuda:${revision}`（聚合 POM，`packaging=pom`） |
| **当前版本** | `1.0.6`（根 POM `<revision>`，CI-friendly versions + `flatten-maven-plugin` `oss` 模式） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘；1.0.19 起全组织统一走该 parent，本仓不再是自包含根 POM） |
| **Maven Central** | 已发布：`z-camuda` / `z-camuda-core` / `z-camuda-web` / `z-camuda-starter` 的 `1.0.6` 均可从 repo1 拉取；`z-camuda-admin:1.0.6` 返回 404（设计上不发 Central） |
| **默认端口** | `8080`（`SERVER_PORT`），`server.servlet.context-path` 默认 `/`（`SERVER_CONTEXT_PATH`）；`z-rpc` 端口默认 `20880` |
| **运行口径** | Java 8 · Spring Boot 2.7.18（库模块经父链地板；`z-camuda-admin` 自带 `spring-boot-starter-parent` 钉 2.7.12） · Camunda BPM 7.18.0 |
| **最近更新** | 2026-09-30 |

> 版本口径由父链下发：第三方地板来自 `z-boot-dependencies`，兄弟仓（`z-config` / `z-rpc` / `z-util`）面值来自
> `z-boot-fleet` 权威表。`camunda.version=7.18.0` 是本仓**刻意保留**的自有版本键（父链不供 `org.camunda.*`）。

---

## 🎯 能力清单

能力全部对应到代码里的 Controller / Service / SPI 接口，不做无实现的承诺：

| 能力 | 入口 | 说明 |
|------|------|------|
| 审批中心 | `ApprovalCenterController` (`/api/approval-center`) | Dashboard 统计、待办 / 已办 / 我的发起 / 任务详情 / 流程详情、发起与删除流程、流程定义检索与版本 / 挂起 / 激活 |
| 流程操作 | `ProcessOperationController` (`/api/wf/process`) | 挂起 / 激活实例、加签评论、审批轨迹（trail）、流程总览 |
| 任务操作 | `TaskOperationController` (`/api/wf/task`) | 转办（transfer）、委派（delegate）、认领（claim）、撤回（withdraw）、跳转（jump）、强制完成 |
| 流程分组 | `GroupController` (`/api/wf/group`) | 基于 Camunda Category 的流程分组增删查 |
| 请假流程 | `LeaveProcessController` (`/api/leave`) | 示例流程：发起 / 待办 / 审批任务 / 完成 |
| 健康检查 | `CamudaBaseHealthController` (`/api/wf`) | `GET /api/wf/health` |
| 生命周期钩子 | `CamudaProcessHook` / `CamudaTaskHook` / `CamudaNotificationHook` | 流程启动前后 / 完成、任务创建变更完成、通知（分配 / 结果 / 超时）回调接口 |
| SPI 扩展点 | `com.zifang.z.camuda.core.spi.*`（22 个接口）+ `CamudaSpiRegistry` | 表单、审批、流程、Apex 四大类扩展点，`@CamudaSpi` 标注、按 `order` 串行执行 |
| RPC 暴露 | `CamudaProcessRpcServiceImpl`（`@ZRpcService`） | 经 z-rpc 对外暴露 `CamudaProcessRpcService`：发起 / 待办 / 完成 |
| 服务注册 | `CamudaServerLifecycle` | 上下文就绪后把 `z-camuda` 实例（ip + rpc 端口）注册进 z-config |

> 说明：钩子接口是**本地 Bean** 扩展模型（业务方 `@Bean` 提供实现）；旧 README 里"HTTP / RPC 适配器类
> `HttpWfProcessHook` / `RpcWfProcessHook`"在当前代码中并不存在，故不再列为可用能力。跨进程调用走下面的 z-rpc。

---

## 🏗️ 项目结构

4 个 Maven 模块（根 POM `<modules>` 实测清单），不再有历史里的 `z-camuda-client`：

```
z-camuda/
├── pom.xml                # 根聚合 POM：继承 z-boot-parent:1.0.21，<revision> 统一版本，camunda-bom 在本仓 import
├── z-camuda-core/             # Camunda 封装 + 3 个钩子接口 + 22 个 SPI 扩展接口 + @CamudaSpi + CamudaSpiRegistry + LeaveProcessService
├── z-camuda-web/              # REST API：6 个 Controller + DTO/VO（依赖 core、z-util-core、swagger-annotations provided）
├── z-camuda-starter/          # 自动装配 + z-config/z-rpc 可选集成 + RPC 暴露 + audit SPI 夹具 + 示例流程
│   └── src/main/resources/
│       ├── META-INF/spring.factories          # 指向 CamudaStarterAutoConfiguration
│       └── processes/
│           ├── leaveProcess.bpmn              # 请假流程示例
│           └── fiveLookEvaluation.bpmn        # 五看评估流程示例（多步评分 + 排他网关）
├── z-camuda-admin/            # 独立可启动应用（不发 Central）+ 自带管理前端 + 端到端测试
│   └── src/main/resources/
│       ├── application.properties             # 端口 / 数据源 / camunda / z-config / z-rpc 口径
│       ├── application-h2-test.properties     # h2-test profile：H2 内存库 + 关外部依赖
│       └── static/                            # 打包进来的管理前端（LogicFlow 设计器 + 审批页）
├── Dockerfile             # 多阶段：maven:3.9.9-eclipse-temurin-8 → eclipse-temurin:8-jre，EXPOSE 8080，healthcheck /api/wf/health
├── docker-compose.yml     # 三服务编排：mysql + z-config + z-camuda
├── LICENSE                # MIT
└── _doc/                  # 文档收口，见文末「文档目录」
```

`z-camuda-admin` 留在 reactor 里是为了享受统一构建，但其 POM 设了 `maven.deploy.skip=true`，且根 POM 的
`central-publishing-maven-plugin` 把它放进 `excludeArtifacts`——产物只作 Docker 镜像来源或本地
`java -jar` 演示，**永远不会上 Maven Central**。`z-camuda-starter` 里的 `z-config` / `z-rpc` 是 `optional`，
独立部署形态（admin）会显式补上这两条依赖，否则 `CamudaServerLifecycle` 的服务注册不生效。

---

## 🔧 技术栈

均来自各 POM 实测：

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | Java 8（父链 `pluginManagement` 下发 source/target 8 + `-parameters`，class-file major 52） |
| 框架 | Spring Boot 2.7.18（库模块由 `z-boot-dependencies` 地板供）；`z-camuda-admin` 自带 parent `spring-boot-starter-parent:2.7.12` |
| 工作流引擎 | Camunda BPM `7.18.0`（`camunda-bpm-spring-boot-starter` + `camunda-bom` import，版本键留本仓） |
| 通用类型 | `z-util-core`（`Result` / `PageResult` / `StatusCode`），版本随 `z-boot-fleet` 权威表 |
| 数据库 | 生产 MySQL 8（`mysql-connector-java 8.0.33` 旧坐标）+ Druid `1.2.18`（admin）；H2（`h2-test` profile） |
| 日志 | log4j2（`log4j-api`/`core` 走地板 2.25.4；`log4j-slf4j2-impl 2.26.1` 本仓留格） |
| 接口文档 | Knife4j / OpenAPI3（`knife4j-openapi3-spring-boot-starter 4.1.0` 在 admin；`swagger-annotations 2.2.8` 在 web，provided） |
| 服务治理 | `z-config`（注册 + 配置）· `z-rpc`（Netty RPC），starter 里 optional |
| 前端 | `z-camuda-admin` 内置静态管理前端（LogicFlow 流程设计 + 审批页） |
| 构建 | Maven；`flatten-maven-plugin 1.7.3`（`oss` 模式 + `updatePomFile`，发布态把 `${revision}` / `${project.version}` 落成字面量，产出自包含 pom） |

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

第三方与兄弟仓版本由 `z-boot-parent` → `z-boot-dependencies`（地板）+ `z-boot-fleet`（权威表）供给，
模块 POM 不再出现字面版本钉；若构建报找不到版本，先确认能解析到 `io.github.yuku123:z-boot-parent:1.0.21`。

### 作为依赖嵌入业务方（Starter）

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-camuda-starter</artifactId>
    <version>1.0.6</version>
</dependency>
```

Starter 的 `CamudaStarterAutoConfiguration` 会扫描 `com.zifang.z.camuda.web.api` / `.core` / `.starter`，拉起
Controller、Camunda 引擎装配与 SPI 注册表；`z-config` / `z-rpc` 集成是可选的——classpath 不含就不加载。

### 独立跑起来

```bash
# 1) 无外部依赖（H2 内存库，关闭 z-config/z-rpc）——最快看到 /doc.html 与管理前端
mvn -pl z-camuda-admin -am package -DskipTests
java -jar z-camuda-admin/target/z-camuda-admin-1.0.6-exec.jar --spring.profiles.active=h2-test

# 2) 连真实 MySQL：所有凭据一律经环境变量注入，禁止写进 yml/jar/镜像层
SERVER_PORT=8080 \
SPRING_DATASOURCE_URL='jdbc:mysql://<host>:3306/z_camuda?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai' \
SPRING_DATASOURCE_USERNAME=*** SPRING_DATASOURCE_PASSWORD=*** \
CAMUNDA_ADMIN_USER=*** CAMUNDA_ADMIN_PASSWORD=*** \
Z_CONFIG_ENABLED=true Z_CONFIG_SERVER_ADDR=127.0.0.1:8848 \
Z_RPC_ENABLED=true Z_RPC_SERVER_PORT=20880 \
  java -jar z-camuda-admin/target/z-camuda-admin-1.0.6-exec.jar
```

启动后 REST 基址 `http://localhost:8080/api/...`，Knife4j 文档在 `http://localhost:8080/doc.html`
（Knife4j 仅在 `z-camuda-admin` 引入）。配置前缀：`spring.datasource.*` / `camunda.bpm.*` / `z.config.*` / `z.rpc.*`，
对应环境变量名见 `z-camuda-admin/src/main/resources/application.properties`。`h2-test` profile（见同名 properties）
自动部署 `classpath*:processes/*.bpmn` 并把 `z.config.enabled` / `z.rpc.enabled` 关掉。

---

## 🔌 API 一览

服务前缀由 `server.port` + `server.servlet.context-path` 决定（默认 `8080` + `/`），下表为各 Controller 的类级映射：

| 路径前缀 | Controller | 关键端点 |
|----------|------------|----------|
| `/api/approval-center` | `ApprovalCenterController` | `GET /dashboard`、`GET /tasks/todo`、`GET /tasks/done`、`GET /tasks/get`、`POST /tasks/complete`、`GET /my-processes`、`GET /processes/get`、`POST /processes/start`、`GET /processes/definitions`、`GET /processes/search`、`GET /processes/versions`、`POST /processes/definitions/suspend`、`POST /processes/definitions/activate`、`DELETE /processes` |
| `/api/wf/process` | `ProcessOperationController` | `POST /suspend`、`POST /activate`、`POST /comment`、`GET /comments`、`GET /trail`、`GET /overview` |
| `/api/wf/task` | `TaskOperationController` | `POST /transfer`、`POST /delegate`、`POST /claim`、`POST /withdraw`、`POST /jump`、`POST /force-complete` |
| `/api/wf/group` | `GroupController` | `GET /list`、`POST /set`、`GET /processes`、`DELETE` |
| `/api/leave` | `LeaveProcessController` | `POST /start`、`GET /todo`、`GET /getApprovalTasks`、`POST /complete` |
| `/api/wf` | `CamudaBaseHealthController` | `GET /health` |

分页统一返回 `z-util` 的 `PageResult`（近期从各自拼装的页结构收敛过来）。鉴权由 `z-ctc` 统一拦截，本仓不做。

---

## 🧩 扩展模型（钩子 + SPI）

**钩子（3 个接口，`com.zifang.z.camuda.core.hook`）** —— 本地 Bean 方式在生命周期节点插逻辑：
`CamudaProcessHook`（`onBeforeStart` / `onAfterStart` / `onComplete`）、
`CamudaTaskHook`（`onBeforeCreate` / `onAssigneeChanged` / `onBeforeComplete` / `onAfterComplete`）、
`CamudaNotificationHook`（`notifyTaskAssigned` / `notifyApprovalResult` / `notifyOverdue`）。全部 `default` 方法，按需覆盖。

**SPI 扩展点（22 个接口，`com.zifang.z.camuda.core.spi`，蒸馏自 ace-platform-sdk）** —— 分四大类：
表单（初始化 / 校验 / 暂存 / 提交 / 查询 / 修改 / 删除的前后置与校验）、
审批（`AgreePre` / `AgreePost` / `AgreeValidate` / `RejectValidate`）、
流程（`WorkflowContextInject` / `WorkflowLogicAssigneeInject` / `WorkflowLogicAssigneeCall`）、
Apex（`ApexListStaff` / `ApexListDept`）。

**⚠ 22 个接口里只有 4 个真正接入了派发点**，其余 18 个是**预留扩展点**：

| | code | 触发点 |
| --- | --- | --- |
| **已接入（4）** | `FormDataSubmitPreHandlerService` | 发起流程实例前 |
| | `FormDataSubmitPostHandlerService` | 发起流程实例后 |
| | `AgreePreService` | 审批任务 complete 前 |
| | `AgreePostService` | 审批任务 complete 后 |
| **未接入（18）** | 其余全部（表单的 init/lifecycle/validate/query/remove/temp/modify、审批的 validate、流程的三类、Apex 的两类） | **无**——见下 |

`CamudaSpiRegistry` 启动时扫描 Spring 容器里实现了这 22 个接口且带 `@CamudaSpi` 的 Bean，按 `code` 归组、按 `order`
升序排列。但注册表**只提供 `register` / `getByCode` / `stats`，没有 dispatch 方法**；派发逻辑是
`LeaveProcessService`（示例流程）里手工写的 4 个 `getByCode` 循环。

所以对那 18 个接口：**实现它不会报错、也一定会被扫描进注册表，但方法永远不会被执行。**
判断"有没有被调用"不能看 `spiRegistry.getByCode(code)` 是否非空——**注册成功 ≠ 被调用**，
它只证明扫描到了 Bean。

**SPI 失败不中断流程**：已接入的 4 个在 `Result.isSuccess() == false` 时只记一条 warn 日志，
**流程照常推进**（pre 类是"本次返回值被丢弃、沿用调用方传入的变量"），返回 `null` 则静默跳过。
接口 Javadoc 原先写的是"引擎中断流程"，与实现不符，已全部订正。

```java
@Component
@CamudaSpi(name = "示例-表单发起前处理", code = "FormDataSubmitPreHandlerService", group = "表单", order = 10)
public class MyPreHandler implements CamudaFormDataSubmitPreHandlerService {
    @Override
    public Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data) {
        // 校验 / 补默认值 / 转换后返回；返回失败只记 warn，不会中断流程
        return Result.success(data);
    }
}
```

`z-camuda-starter` 的 `audit` 包（`ComprehensiveSpiBundle` / `AuditAggregator`）提供了覆盖 22 个 SPI 的计数夹具，
但**端到端测试只断言了其中 4 个计数器**（`submitPre` / `submitPost` / `agreePre` / `agreePost`），
其余 18 个计数恒为 0 —— 它验证的不是"全链路"，只是"这 4 个 code 通了"。

`SpiDispatchDocumentationTest` 把「Javadoc 必须与实际派发一致」钉成护栏：它**从 main 源码树里
提取真实的 `getByCode` 派发集合**再逐个比对 22 份 Javadoc（新增/删除派发点而不改文档，判据立刻红）。

### 服务注册与远程调用（可选）

`CamudaServerLifecycle` 在 `ContextRefreshedEvent` 时，若 classpath 含 z-config 客户端，就调
`ZNamingService.registerInstance(serviceName, ip, rpcPort, "DEFAULT")` 把实例注册进 z-config
（`serviceName` 取 `spring.application.name`，默认 `z-camuda`；`rpcPort` 默认 `20880`）。注册失败不阻塞启动。
`CamudaProcessRpcServiceImpl` 用 `@ZRpcService(interfaceClass = CamudaProcessRpcService.class, version = "1.0.0")`
暴露流程服务，外部方经 `@ZRpcReference(CamudaProcessRpcService.class)` 从 z-config 发现实例后远程调用。
配置前缀：`z.config.enabled` / `z.config.server-addr` / `z.config.namespace`，`z.rpc.enabled` / `z.rpc.server.port` /
`z.rpc.registry.type=z-config` / `z.rpc.registry.address` / `z.rpc.service`（对应环境变量见 `application.properties`）。

---

## 🧪 测试

真正的测试在 `z-camuda-admin/src/test/java`，是两个基于 Camunda 引擎的端到端用例，跑在 H2 内存库上、
不依赖任何外部服务（`@SpringBootTest(classes = ZCamudaAdminApplication.class, RANDOM_PORT)` + `@ActiveProfiles("h2-test")`）：

| 测试 | 覆盖 |
|------|------|
| `LeaveProcessEndToEndTest` | `leaveProcess.bpmn` 全流程：发起 → SubmitPre/Post SPI → 审批查询 → 完成 → AgreePre/Post SPI → approved/rejected 分支流转 → 历史审计 |
| `FiveLookEndToEndTest` | `fiveLookEvaluation.bpmn` 多步评分 → 决策网关（加权分阈值）→ CEO 终审 → 3 路终态；22 个 SPI 接口全覆盖 + 调用顺序一致性 |

```bash
# 端到端测试依赖 reactor 里的 core/web/starter，用 -am 一起构建
mvn -pl z-camuda-admin -am test
# 或从根跑全量（其余模块当前无单测）
mvn test
```

> 发布件是否真的可从 Maven Central 拉取，用本仓自带的闸门验（它在真实 Central 上跑，
> 校验 jar/sources/javadoc/签名/POM metadata/sources.jar 内容）：
>
> ```bash
> bash _doc/003_script/verify_central.sh
> ```
>
> 原本承载这件事的 `z-opc/z-middleware-integration-test` 已删除 —— 它把各中间件的发布件
> 验证集中放在 z-opc 里，位置不对（各仓该验自己的构件），已改为各仓自验。

---

## 🐳 部署

```bash
# 本地一键三服务编排（mysql + z-config 注册中心 + z-camuda）
docker compose up -d
docker compose ps
curl http://localhost:8080/api/wf/health     # z-camuda REST
```

`docker-compose.yml` 拉起三个服务：`mysql:8.0`（z-config 与 Camunda 共用，生产建议拆开）、
`z-config`（context `../z-config`，端口 8848）、`z-camuda`（本仓 `Dockerfile`，端口 8080）。compose 里
数据库口令与 z-config 地址是本地编排用演示值，生产通过替换 service 的 `SPRING_DATASOURCE_*` /
`Z_CONFIG_SERVER_ADDR` 等环境变量注入，切勿提交真实凭证。

`Dockerfile` 是多阶段构建：build 阶段 `maven:3.9.9-eclipse-temurin-8` 先 `mvn -N install` 父 POM、再
`-pl z-camuda-admin -am clean package` 打出 fat jar；runtime 阶段 `eclipse-temurin:8-jre`、非 root 用户（uid 10001）、
`EXPOSE 8080`，`HEALTHCHECK` 打 `${SERVER_CONTEXT_PATH}/api/wf/health`，入口类 `com.zifang.z.camuda.admin.ZCamudaAdminApplication`，
JVM 参数经 `JVM_OPTS` 覆盖。

发布到 Maven Central 用 `_doc/003_script/deploy_maven_center.sh`（`publish` / `verify`），admin 因
`excludeArtifacts` 不会被发布。

---

## 📄 License

许可证见仓库根 [`LICENSE`](LICENSE)，为 **MIT License**。

> 提示：根 POM 与各模块 POM 的 `<licenses>` 仍声明 "Apache License, Version 2.0"，与 LICENSE 文件不一致；
> 以 LICENSE 文件为准。

---

## 文档目录

本项目文档统一收口在 `_doc/` 下：

- [`_doc/003_script/`](_doc/003_script/) — 运维与工具脚本（本仓 `_doc/` 目前唯一有内容的子目录）：
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — 编译并发布到 Maven Central（`publish` / `verify`）
  - [`install-settings.sh`](_doc/003_script/install-settings.sh) — 写入 Maven Central 凭证到 `~/.m2/settings.xml`（用 `CENTRAL_USERNAME` / `CENTRAL_TOKEN` 环境变量占位，不落明文）
  - [`gen_wf_spi.py`](_doc/003_script/gen_wf_spi.py) — 批量生成器，把 ace-platform-sdk 的 22 个 SPI 接口蒸馏到 `z-camuda-core`
  - [`wf-demo.sh`](_doc/003_script/wf-demo.sh) — 端到端演示脚本（健康检查 / 流程定义 / 启动请假流程）

> `_doc/001_arch/`、`_doc/002_deploy/`、`_doc/004_skill/` 目前均为空目录，尚无归档文档；
> 架构与部署说明现内联在本 README 上文，脚本归 [`_doc/003_script/`](_doc/003_script/)。

- `_doc/001_arch/` — 架构文档（目前为空目录，暂无内容）
- `_doc/002_deploy/` — 部署 SQL（目前为空目录，建表由 Camunda `schema-update` 与脚本自理）
- `_doc/004_skill/` — AI skill 定义（目前为空目录，暂无 skill）

_Maintained by the z-opc-foundation organization._
