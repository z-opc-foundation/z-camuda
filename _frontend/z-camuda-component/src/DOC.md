# z-camuda-component

z-camuda（流程引擎）跨仓唯一前端面：把「请假流程演示 / 引擎分组」这两个流程引擎控制台页面
收敛成一个可复用的 React 组件包。本包是 z-wf 的**引擎侧**（提供端点与分组管理），
z-wf 是**业务侧**（工作台 / 待办 / 已办 / 流程设计器），两者配合使用。

- **npm 包名**：`@yuku123/z-camuda-component`
- **宿主接入**：`@yuku123/z-camuda-component/pages`（manifest + 页面 + API），主壳可只取 `ComponentDocPage`
- **后端**：流程引擎适配层 `/api/leave/**`（请假演示流程）+ `/api/wf/group/**`、`/api/wf/health`
- **技术栈**：React 19 + antd 6 + react-router-dom 7 + vite（library mode，双入口 `.` / `./pages`）

---

## 一、能力概览

路由前缀 `/z-camuda`，菜单与路由在 `src/pages-manifest.jsx` 里声明。

| 页面 | 路由（相对 `/z-camuda/*`） | 源文件 | 能力 |
|------|--------------------------|--------|------|
| 登录 | `login` | `pages/LoginPage.jsx` | 登录页（suit 侧单独挂这条路由） |
| 首页 | `home` | `pages/HomePage.jsx` | 入口卡片页 |
| 请假流程 | `leave` | `pages/LeaveDemo.jsx` | 发起请假流程 + 待审批任务列表 + 审批通过 / 驳回 |
| 引擎分组 | `groups` | `pages/Groups.jsx` | 引擎分组列表 / 新建 / 修改 + 分组下的流程清单 |
| 组件文档 | `docs` | `pages/ComponentDoc.jsx` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

本包的 `routes` **没有**兜底通配（`*`）项，未列出的路径由宿主的 404 兜底。

## 二、导出面

```js
// 主入口（. 桶）
import {camudaApi, configureCamuda, ComponentDocPage} from '@yuku123/z-camuda-component'

// 宿主组装面（./pages 桶）
import {menuItems, routes, HomePage, LoginPage, LeaveDemo, Groups} from '@yuku123/z-camuda-component/pages'
```

`./pages` 桶 = `export * from './pages-manifest.jsx'` 的整份转发 + `configureCamuda` 再导出。

## 三、API 端点

请求实例由基座 `createRequest` 工厂创建（`src/services/api.js`），`baseURL` 默认空串
（路径自带 `/api` 前缀），token 键 `zcamuda_token`。下表是 `camudaApi` 里**实际调用**的路径。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/wf/health` | 引擎健康检查 |
| POST | `/api/leave/start` | 发起请假流程（body `applicant` / `approver`） |
| GET | `/api/leave/todo` | 我的待办（query `approver`） |
| GET | `/api/leave/getApprovalTasks` | 待审批任务（query `approver`） |
| POST | `/api/leave/complete` | 完成任务（body `taskId` / `approvalResult`） |
| GET | `/api/wf/group/list` | 引擎分组列表 |
| POST | `/api/wf/group/set` | 新建 / 修改分组（body 带 `id` 即为修改） |
| GET | `/api/wf/group/processes` | 分组下的流程清单 |

## 四、页面说明

### 4.1 请假流程

三个动作串成一条链：

1. **发起**：填申请人 + 审批人 → `leaveStart(applicant, approver)`；
2. **待办**：按审批人查待审批任务（`leaveApprovalTasks`），空态「暂无待审批任务」；
3. **审批**：对任务调用 `leaveComplete(taskId, approvalResult)`，`approvalResult` 区分通过 / 驳回。

整页加载失败用基座 `ErrorState`（标题「后端未连接」）+ 重试。

### 4.2 引擎分组

并行拉 `groupList` 与 `groupProcesses`（各自 `.catch(() => [])`，一块失败不拖垮另一块），
渲染分组列表与分组下的流程清单；新建 / 修改都走 `groupSet`（带 `id` 即为修改，不带即新建）。

### 4.3 组件文档

本文件。内容以 `?raw` 在编译期内联，零额外请求；左侧 240px TOC 随滚动联动。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `configureCamuda({apiBase})` | `''` | 改写请求实例的 `baseURL`，宿主换挂载前缀时用 |
| token 存储键 | `zcamuda_token` | 独立命名，避免与其他套件串号 |
| `unwrap(promise)` | — | 后端部分端点直接回 Map（非 Result 包裹），统一解一层 `data`；`success=false` 或 `code>=400` 抛错 |

## 六、已知限制

- 本包**只提供引擎侧能力**（分组管理 + 请假演示 + 健康检查）。
  业务侧的待办 / 已办 / 流程设计器在 **z-wf** 包里，两包要一起挂才能看到完整流程。
- z-wf 的流程设计器依赖本包提供的 `/api/approval-center/processes/model/**` 端点 ——
  **只挂 z-wf 而不挂 z-camuda 时，流程设计页会 404**。
- `unwrap` 只解一层 `data`；后端改成多层信封时要同步改这里。
- `routes` 里没有 `*` 兜底 —— 宿主必须自己提供 404 路由。
- 页面统一把后端异常渲染成基座 `ErrorState`（标题「后端未连接」）—— 真因要看浏览器控制台。
- 组件包必须 external 基座；`vite.config.js` 的 `external` 是精确字符串匹配，
  `@yuku123/z-frontend-common/ui` 与 `/ui/echarts` 两个子路径必须逐个列出，漏了基座会被复制进 dist。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.0 | 新增组件文档页 `ComponentDocPage`（lead 008 §7）：`src/DOC.md` + 基座 `ComponentDoc` 薄包装 |
| 0.1.0 | 首版：请假流程演示 + 引擎分组页面集 + manifest + `camudaApi` |
