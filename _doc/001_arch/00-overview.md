# 项目总览

## 1. 项目简介

z-camuda 是基于 **Camunda 7** 的工作流引擎：把 Camunda BPMN 引擎封装成一组 REST API 和一个 Spring Boot Starter，业务方引一条依赖即可把流程能力嵌进自己的进程；独立部署时由 `z-camuda-admin` 提供自带管理前端。

它在一人公司基座里的定位是「审批 / 流程」中枢。真正的差异化不在功能清单，而在**扩展模型**：3 个生命周期钩子接口 + **22 个 SPI 扩展接口**（表单 / 审批 / 流程 / Apex 四大类，蒸馏自 ace-platform-sdk），业务方实现接口并打上 `@CamudaSpi`，`CamudaSpiRegistry` 启动时扫描、按 `order` 排序，在流程关键节点串行回调。

**技术栈：** Java 8 · Spring Boot 2.7.18（`z-camuda-admin` 自带 `spring-boot-starter-parent:2.7.12`）· Camunda BPM 7.18.0（`camunda-bom` import，版本键 `camunda.version` 留在本仓）· `z-util-core`（`Result` / `PageResult` / `StatusCode`）· `z-config` 注册中心 + `z-rpc`（Netty RPC）· MySQL 8 + Druid（生产）/ H2（`h2-test` profile）

## 2. 模块架构

```
z-camuda/
├── pom.xml                    # 根聚合 POM：z-camuda:${revision}=1.0.6，parent = z-boot-parent:1.1.0（relativePath 留空）
│                              #   4 个 <module>，camunda-bom 在本仓 import
├── z-camuda-core/             # 37 个类：Camunda 封装 + 3 个钩子 + 22 个 SPI 接口 + 注册表 + 示例流程服务
│   └── com.zifang.z.camuda.core.{spi,spi.dto,hook,service,converter,demo}
├── z-camuda-web/              # 15 个类：7 个 REST Controller + DTO / VO（依赖 core、z-util-core、swagger-annotations provided）
│   └── com.zifang.z.camuda.web.{api,dto}
├── z-camuda-starter/          # 12 个类：自动装配 + z-config/z-rpc 可选集成 + RPC 暴露 + audit SPI 夹具 + 示例流程
│   ├── src/main/resources/
│   │   ├── META-INF/spring.factories   # 注册 CamudaStarterAutoConfiguration + host.CamudaHostAutoConfiguration
│   │   └── processes/
│   │       ├── leaveProcess.bpmn             # 请假流程示例
│   │       └── fiveLookEvaluation.bpmn       # 五看评估流程示例（多步评分 + 排他网关）
│   └── com.zifang.z.camuda.starter{,.audit,.host,.rpc}
├── z-camuda-admin/            # 2 个类：ZCamudaAdminApplication + ZCamudaWebMvcConfig（独立可启动应用，不发 Central）
│   └── src/main/resources/
│       ├── application.properties           # 端口 / 数据源 / camunda / z-config / z-rpc 口径
│       ├── application-h2-test.properties   # h2-test profile：H2 内存库 + 关外部依赖
│       └── static/                          # 打包进来的管理前端（LogicFlow 设计器 + 审批页）
├── Dockerfile                 # 多阶段：maven:3.9.9-eclipse-temurin-8 → eclipse-temurin:8-jre，EXPOSE 8080
├── docker-compose.yml         # 三服务编排：mysql:8.0 + z-config + z-camuda
├── _frontend/                 # z-camuda-component（页面）+ z-camuda-suit（壳）两套 Vite 工程
└── _doc/003_script/           # 7 个脚本（发布 / SPI 生成 / 端到端演示 / 校验清单）
```

## 3. 核心功能

### 3.1 REST API（7 个 Controller）

服务前缀由 `server.port` + `server.servlet.context-path` 决定（默认 `8080` + `/`）：

| 路径前缀 | Controller | 关键端点 |
|---|---|---|
| `/api/approval-center` | `ApprovalCenterController` | `GET /dashboard`、`GET /tasks/todo`、`GET /tasks/done`、`GET /tasks/get`、`POST /tasks/complete`、`GET /my-processes`、`GET /processes/get`、`POST /processes/start`、`GET /processes/definitions`、`GET /processes/search`、`GET /processes/versions`、`POST /processes/definitions/suspend`、`POST /processes/definitions/activate`、`DELETE /processes` |
| `/api/approval-center/processes/model` | `ProcessModelController` | 流程模型（模板）相关端点 |
| `/api/wf/process` | `ProcessOperationController` | `POST /suspend`、`POST /activate`、`POST /comment`、`GET /comments`、`GET /trail`、`GET /overview` |
| `/api/wf/task` | `TaskOperationController` | `POST /transfer`、`POST /delegate`、`POST /claim`、`POST /withdraw`、`POST /jump`、`POST /force-complete` |
| `/api/wf/group` | `GroupController` | `GET /list`、`POST /set`、`GET /processes`、`DELETE`（基于 Camunda Category） |
| `/api/leave` | `LeaveProcessController` | `POST /start`、`GET /todo`、`GET /getApprovalTasks`、`POST /complete`（示例流程） |
| `/api/wf` | `CamudaBaseHealthController` | `GET /health` |

分页统一返回 `z-util` 的 `PageResult`。鉴权由 `z-ctc` 统一拦截，本仓不做。

### 3.2 生命周期钩子（3 个接口）

`com.zifang.z.camuda.core.hook` 下的 `CamudaProcessHook`（`onBeforeStart` / `onAfterStart` / `onComplete`）、`CamudaTaskHook`（`onBeforeCreate` / `onAssigneeChanged` / `onBeforeComplete` / `onAfterComplete`）、`CamudaNotificationHook`（`notifyTaskAssigned` / `notifyApprovalResult` / `notifyOverdue`）。全部是 `default` 方法，按需覆盖；扩展方式是**本地 Bean**（业务方 `@Bean` 提供实现）。

### 3.3 SPI 扩展点（22 个接口）

`com.zifang.z.camuda.core.spi` 下 22 个 `public interface`（另有 `CamudaSpi` 注解、`CamudaSpiRegistry` 注册表、`CamudaExtensionContext` 上下文对象与 `spi/dto` 下 6 个 DTO），分四大类：

- **表单** —— 初始化 / 校验 / 暂存 / 提交 / 查询 / 修改 / 删除的前置、后置与校验
- **审批** —— `AgreePre` / `AgreePost` / `AgreeValidate` / `RejectValidate`
- **流程** —— `WorkflowContextInject` / `WorkflowLogicAssigneeInject` / `WorkflowLogicAssigneeCall`
- **Apex** —— `ApexListStaff` / `ApexListDept`

`CamudaSpiRegistry` 启动时扫描 Spring 容器里实现了这 22 个接口且带 `@CamudaSpi` 的 Bean，按 `code` 归组、按 `order` 升序排列。

⚠ **22 个接口里只有 4 个真正接入了派发点**，其余 **18 个是预留扩展点**：

| 状态 | 接口 | 触发点 |
|---|---|---|
| 已接入 | `FormDataSubmitPreHandlerService` | 发起流程实例前 |
| 已接入 | `FormDataSubmitPostHandlerService` | 发起流程实例后 |
| 已接入 | `AgreePreService` | 审批任务 complete 前 |
| 已接入 | `AgreePostService` | 审批任务 complete 后 |
| 未接入 | 其余 18 个（表单的 init/lifecycle/validate/query/remove/temp/modify、审批的 validate、流程的三类、Apex 的两类） | **无** |

注册表只提供 `register` / `getByCode` / `stats`，**没有 dispatch 方法**；派发逻辑是 `LeaveProcessService`（示例流程）里手工写的 4 个 `getByCode` 循环。所以对那 18 个接口：实现它不会报错、也一定会被扫描进注册表，但方法永远不会被执行 —— 判断"有没有被调用"不能看 `spiRegistry.getByCode(code)` 是否非空，**注册成功 ≠ 被调用**。

### 3.4 自动装配与可选集成

`z-camuda-starter` 的 `META-INF/spring.factories` 注册 `CamudaStarterAutoConfiguration` 与 `host.CamudaHostAutoConfiguration`。`z-config` / `z-rpc` 在 starter 里是 **optional**：classpath 不含就不加载；独立部署形态（admin）会显式补上这两条依赖，否则 `CamudaServerLifecycle` 的服务注册不生效。

- `CamudaServerLifecycle` —— 上下文就绪后把 `z-camuda` 实例（ip + rpc 端口）注册进 z-config。
- `CamudaProcessRpcServiceImpl`（`@ZRpcService`）—— 经 z-rpc 对外暴露 `CamudaProcessRpcService`：发起 / 待办 / 完成。
- `starter/audit` 下 6 个类（`AuditAggregator`、`AuditAgreePreSpi`、`AuditAgreePostSpi`、`AuditSubmitPreSpi`、`AuditSubmitPostSpi`、`ComprehensiveSpiBundle`）是 audit 用的 SPI 夹具。
- `starter/host` 下 `CamudaHostAutoConfiguration` 负责宿主形态的装配。

### 3.5 示例流程

`z-camuda-starter/src/main/resources/processes/` 下两份 BPMN：`leaveProcess.bpmn`（请假流程）与 `fiveLookEvaluation.bpmn`（五看评估流程，多步评分 + 排他网关）。`camunda.bpm.deployment-resource-pattern=classpath*:processes/*.bpmn` 负责自动部署，`h2-test` profile 同样走这条路径。

### 3.6 管理前端

`z-camuda-admin/src/main/resources/static/` 是打包进 jar 的管理前端（LogicFlow 流程设计器 + 审批页）。仓内另有 `_frontend/` 两套 Vite 工程：`z-camuda-component`（`Groups.jsx` / `HomePage.jsx` / `LeaveDemo.jsx` / `LoginPage.jsx` + `services/api.js`）与 `z-camuda-suit`（`App.jsx` / `main.jsx`）。

## 4. 对外集成 / 上下游

**上游依赖：** `z-boot-parent:1.1.0`（`relativePath` 留空）→ 第三方地板（Spring Boot 2.7.18、log4j2 2.25.4）与兄弟仓 fleet（`z-util-core` / `z-config` / `z-rpc`）。`camunda.version=7.18.0` 是本仓**刻意保留**的自有版本键（父链不供 `org.camunda.*`），由本仓 import `camunda-bom`。`log4j-slf4j2-impl 2.26.1` 同样留格在本仓。

**下游 / 对外边界：**

| 面 | 边界 | 端口 |
|---|---|---|
| REST | Spring MVC（`server.servlet.context-path` 缺省 `/`） | **8080**（`SERVER_PORT`） |
| RPC | z-rpc 服务端（`@ZRpcService` 暴露 `CamudaProcessRpcService`） | **20880**（`Z_RPC_SERVER_PORT`） |
| 注册中心 | z-config 客户端，服务注册 + 配置拉取 | `127.0.0.1:8848`（`Z_CONFIG_SERVER_ADDR`） |
| 接口文档 | Knife4j / OpenAPI3（仅 `z-camuda-admin` 引入） | 随 8080，`/doc.html` |

**数据库：** 生产 MySQL 8（`mysql-connector-java 8.0.33` 旧坐标 + Druid 1.2.18，admin 模块）；`h2-test` profile 走 H2 内存库。Camunda 建表由 `camunda.bpm.database.schema-update` 控制（缺省 `true`）。

**发布口径：** `z-camuda` / `z-camuda-core` / `z-camuda-web` / `z-camuda-starter` 发 Maven Central；`z-camuda-admin` 的 POM 设 `maven.deploy.skip=true`，且根 POM 的 `central-publishing-maven-plugin` 把它放进 `<excludeArtifacts>` —— 因为该插件不认 `maven.deploy.skip`，必须显式排除。admin 产物只作 Docker 镜像来源或本地 `java -jar` 演示，**永远不会上 Central**。

**容器：** `Dockerfile` 多阶段构建（`maven:3.9.9-eclipse-temurin-8` → `eclipse-temurin:8-jre`），以 uid 10001 非 root 运行，`EXPOSE 8080`，`HEALTHCHECK` 打 `/api/wf/health`；`docker-compose.yml` 编排 `mysql:8.0` + `z-config` + `z-camuda` 三服务。

## 5. 技术特点

1. **扩展点数量与派发点数量被刻意分开记账**：22 个 SPI 接口、4 个真实派发点、18 个预留 —— README 与代码注释都把这三个数字摆在一起，并明说"注册成功 ≠ 被调用"，避免把骨架说成能力。
2. **SPI 是代码生成出来的**：`_doc/003_script/gen_camuda_spi.py` 把 ace-platform-sdk 的接口批量蒸馏到 `z-camuda-core`，22 个接口形制一致、不是手抄的。
3. **钩子与 SPI 两种扩展模型并存**：钩子是 3 个生命周期接口（本地 Bean 覆盖 `default` 方法），SPI 是 22 个细粒度接口（注解 + 注册表 + 排序），解决的是"我要在流程启动前插逻辑"与"我要在表单暂存后插逻辑"两种粒度。
4. **admin 走"own-parent"路线**：`z-camuda-admin` 自带 `spring-boot-starter-parent:2.7.12`，不继承 `z-boot-parent`（库模块继承），版本需与根 `<revision>` 保持一致 —— 同一仓里两套 parent 链是本仓最不寻常的结构。
5. **中央发布要双重排除**：`maven.deploy.skip=true` 拦不住 `central-publishing-maven-plugin`，必须再写 `<excludeArtifacts>`，根 POM 注释里明写了这个原因。
6. **示例流程是真实可跑的 BPMN**：不是伪代码，仓库里躺着 `leaveProcess.bpmn` 与 `fiveLookEvaluation.bpmn`，`LeaveProcessService` 又是那 4 个 SPI 派发点的唯一手工实现所在 —— 扩展模型在示例流程上是端到端跑通的。
7. **凭据一律走环境变量**：`application.properties` 里所有 datasource / camunda admin / z-config / z-rpc 值都写成 `${ENV:default}` 形式，默认值里没有真实凭据；`h2-test` profile 额外把 `z.config.enabled` / `z.rpc.enabled` 关掉，给"零外部依赖最快看到界面"留了一条路。

## 6. 目录结构

源码包树共 4 段（`com.zifang.z.camuda.*`），主源码 **66 个 `.java`**（core 37 + web 15 + starter 12 + admin 2），测试 4 个（core 2 + admin 2）：

```
z-camuda/
├── z-camuda-core/src/main/java/com/zifang/z/camuda/core/
│   ├── spi/          (22 个接口 + CamudaSpi 注解 + CamudaSpiRegistry + CamudaExtensionContext)
│   ├── spi/dto/      (6)  # CamudaApexDeptDTO / CamudaApexStaffDTO / 两个 Request / WorkflowContextParam / AssigneeFunction
│   ├── hook/         (3)  # CamudaProcessHook / CamudaTaskHook / CamudaNotificationHook
│   ├── service/         # LeaveProcessService 等：SPI 派发的唯一手工实现处
│   ├── converter/       # DTO / VO ↔ 引擎对象转换
│   └── demo/            # 示例流程相关
├── z-camuda-web/src/main/java/com/zifang/z/camuda/web/
│   ├── api/          (7)  # ApprovalCenterController / ProcessModelController / ProcessOperationController
│   │                     #   / TaskOperationController / GroupController / LeaveProcessController
│   │                     #   / CamudaBaseHealthController
│   └── dto/          (8)  # TodoTaskDTO / ApprovalRequestDTO / DashboardStatsVO / ProcessDetailVO
│                         #   / ProcessInstanceVO / StartProcessRequestDTO / TaskDetailVO / TaskSummaryVO
├── z-camuda-starter/src/main/java/com/zifang/z/camuda/starter/
│   ├── CamudaStarterAutoConfiguration.java
│   ├── CamudaServerLifecycle.java
│   ├── CamundaConfig.java
│   ├── rpc/          (2)  # CamudaProcessRpcService + Impl（@ZRpcService）
│   ├── audit/        (6)  # audit 用的 SPI 夹具与聚合器
│   └── host/         (1)  # CamudaHostAutoConfiguration
└── z-camuda-admin/src/main/java/com/zifang/z/camuda/admin/
    ├── ZCamudaAdminApplication.java
    └── config/ZCamudaWebMvcConfig.java
```

`_doc/` 侧目前只有 `003_script/` 一个桶（7 个文件：`camuda-demo.sh` 端到端演示、`deploy_maven_center.sh` 一键发 Central、`install-settings.sh` 写凭证进 `~/.m2/settings.xml`、`gen_camuda_spi.py` SPI 批量生成器、`verify_central.sh` 与两份校验清单 `verify-classes.txt` / `verify-exclude.txt`）。
