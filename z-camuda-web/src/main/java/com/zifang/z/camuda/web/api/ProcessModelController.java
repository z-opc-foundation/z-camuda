package com.zifang.z.camuda.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.converter.LogicFlowGraphConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 流程设计 Controller —— 补齐 z-camuda 之前没暴露的「流程图」能力。
 *
 * <p><b>为什么要补</b>：工作台前端 {@code /workflow/designer/:id} 挂在
 * {@code SystemShell} 路由上，但它调的 {@code designerApi} 三个方法
 * （getFlowGraph / saveFlowGraph / deployProcess）在 z-camuda 里<b>从来就没有对应端点</b> ——
 * 之前前端把它们指向 {@code makeApi('designer')}，那是个只有
 * list/page/get/create/update/delete 六个方法的通用垫片，点进去必 TypeError。
 * z-camuda 侧只封装了审批中心/流程操作/任务操作，BPMN 模型本身的读取与发布没做。
 *
 * <p><b>存储口径</b>：设计稿存在 Camunda 的「纯资源部署」里
 * （{@code createDeployment().addString("graph.json", ...)}，不产生流程定义）。
 * 这么做是为了<b>不新增任何数据库表</b> —— 设计稿和流程定义同源同生命周期，
 * 天然跟着 deployment 走，清理策略一致。
 *
 * <p>API 基础路径: {@code /api/approval-center/processes/model}
 *
 * @author z-camuda
 */
@RestController
@RequestMapping("/api/approval-center/processes/model")
@Tag(name = "011_流程设计")

public class ProcessModelController {

    /** 设计稿部署名前缀。与真正的流程部署区分开, 避免被 {@code createDeploymentQuery().deploymentName()} 混进去。 */
    private static final String DESIGN_DEPLOYMENT_PREFIX = "wf-design:";

    /** 设计稿在部署里的资源名。 */
    private static final String DESIGN_RESOURCE_NAME = "graph.json";

    @Resource
    private RepositoryService repositoryService;

    // ==================== 1. 读设计稿 ====================

    /**
     * 取某个流程 key 的最新设计稿 (LogicFlow 图数据)。
     *
     * <p>还没有设计稿但已经有流程定义时, 返回该流程定义里内嵌的
     * {@code camunda:properties/wf:graph} 若存在; 都没有则返回一份最小骨架图,
     * 让设计器总能打开, 而不是 404。
     *
     * @param processKey 流程 key
     * @return {@code {processKey, name, graph, source}} ; source ∈ designed/definition/empty
     */
    @GetMapping("/graph")
    @Operation(summary = "011_获取流程设计稿")
    public Result<Map<String, Object>> getGraph(
            @Parameter(description = "流程定义Key") @RequestParam String processKey) {

        if (processKey == null || processKey.trim().isEmpty()) {
            return Result.fail("processKey 不能为空");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("processKey", processKey);

        Deployment design = findLatestDesign(processKey);
        if (design != null) {
            out.put("name", design.getName());
            out.put("graph", readDesignResource(design));
            out.put("source", "designed");
            out.put("updatedAt", design.getDeploymentTime() == null ? "" : design.getDeploymentTime().toString());
            return Result.success(out);
        }

        // 没有设计稿, 退回用已部署的定义兜底, 保证设计器能打开
        ProcessDefinition def = latestDefinition(processKey);
        if (def != null) {
            out.put("name", def.getName());
            out.put("graph", extractGraphFromDefinition(def));
            out.put("source", "definition");
            out.put("updatedAt", "");
            return Result.success(out);
        }

        out.put("name", processKey);
        out.put("graph", LogicFlowGraphConverter.newStarterGraph(processKey));
        out.put("source", "empty");
        out.put("updatedAt", "");
        return Result.success(out);
    }

    // ==================== 2. 存设计稿 ====================

    /**
     * 保存设计稿 (LogicFlow 图数据)。每次保存产生一个新的纯资源部署, 保留历史。
     *
     * <p>只做形状自检 (非空 / 有节点 / 有 start), 不做 BPMN 语义校验 ——
     * 语义错误留到部署时才报, 避免用户画到一半就被拦。
     *
     * @param request {@code {processKey, name, graph}}
     */
    @PostMapping("/graph")
    @Operation(summary = "012_保存流程设计稿")
    public Result<String> saveGraph(@RequestBody Map<String, String> request) {

        String processKey = trim(request.get("processKey"));
        String name = trim(request.get("name"));
        String graph = request.get("graph");

        if (processKey == null) {
            return Result.fail("processKey 不能为空");
        }
        String error = LogicFlowGraphConverter.validateGraph(graph);
        if (error != null) {
            return Result.fail(error);
        }

        Deployment dep = repositoryService.createDeployment()
                .name(DESIGN_DEPLOYMENT_PREFIX + processKey)
                .addString(DESIGN_RESOURCE_NAME, graph)
                .deploy();

        return Result.success("设计稿已保存 (deployment " + dep.getId() + ")");
    }

    // ==================== 3. 发布 ====================

    /**
     * 把设计稿转成 BPMN 2.0 并部署为真正的流程定义, 随后激活最新版本。
     *
     * <p>转换里的「未实现」项（分支条件 / 多人会签或签 / 抄送）见
     * {@link LogicFlowGraphConverter} 的类注释, 这里不重复。
     *
     * @param request {@code {processKey, name}}; name 可空, 为空时用设计稿里的
     * @return 新流程定义的 id / key / version
     */
    @PostMapping("/deploy")
    @Operation(summary = "013_发布流程")
    public Result<Map<String, Object>> deploy(@RequestBody Map<String, String> request) {

        String processKey = trim(request.get("processKey"));
        String name = trim(request.get("name"));
        if (processKey == null) {
            return Result.fail("processKey 不能为空");
        }

        Deployment design = findLatestDesign(processKey);
        if (design == null) {
            return Result.fail("还没有设计稿, 请先保存流程图");
        }
        String graph = readDesignResource(design);
        if (name == null) {
            name = design.getName();
        }

        final org.camunda.bpm.model.bpmn.BpmnModelInstance bpmnModel;
        try {
            bpmnModel = LogicFlowGraphConverter.toBpmnModel(processKey, name, graph);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail("生成 BPMN 失败: " + e.getMessage());
        }

        Deployment deployed;
        try {
            // addModelInstance 直接交模型对象, 不绕 XML 字符串。
            // 版本: 模板里的 process 没写 version 属性, Camunda 解析时按同 key 自动递增,
            // 所以重复发布天然产生新版本 —— 不需要 deployWithNewProcessVersion
            // (那是 DeploymentBuilderWithNewProcessVersion 上的方法, createDeployment() 拿不到)。
            deployed = repositoryService.createDeployment()
                    .name(name == null ? processKey : name)
                    .addModelInstance(processKey + ".bpmn", bpmnModel)
                    .deploy();
        } catch (Exception e) {
            // Camunda 的部署异常信息很长, 只取第一行给前端, 完整栈进服务端日志
            return Result.fail("部署失败: " + firstLine(e.getMessage()));
        }

        ProcessDefinition latest = latestDefinition(processKey);
        Map<String, Object> out = new HashMap<>();
        out.put("deploymentId", deployed.getId());
        out.put("processKey", processKey);
        out.put("processDefinitionId", latest == null ? null : latest.getId());
        out.put("version", latest == null ? null : latest.getVersion());
        return Result.success(out);
    }

    // ==================== 4. 列出可设计的流程 ====================

    /**
     * 列出「有设计稿」的流程 key, 以及「已部署但还没设计稿」的流程 key, 各自一份。
     *
     * <p>前端流程列表页用它区分两种状态: 只存了草稿没发布的, 和已经发布在跑的。
     *
     * @return {@code {designed:[...], deployedOnly:[...]}}
     */
    @GetMapping("/keys")
    @Operation(summary = "014_列出可设计的流程")
    public Result<Map<String, List<String>>> listKeys() {

        List<String> designed = new ArrayList<>();
        for (Deployment d : repositoryService.createDeploymentQuery()
                .deploymentNameLike(DESIGN_DEPLOYMENT_PREFIX + "%").list()) {
            String key = d.getName().substring(DESIGN_DEPLOYMENT_PREFIX.length());
            if (!key.isEmpty() && !designed.contains(key)) {
                designed.add(key);
            }
        }

        List<String> deployedOnly = new ArrayList<>();
        for (ProcessDefinition def : repositoryService.createProcessDefinitionQuery()
                .latestVersion().list()) {
            if (!designed.contains(def.getKey())) {
                deployedOnly.add(def.getKey());
            }
        }

        Map<String, List<String>> out = new HashMap<>();
        out.put("designed", designed);
        out.put("deployedOnly", deployedOnly);
        return Result.success(out);
    }

    // ==================== 内部方法 ====================

    private Deployment findLatestDesign(String processKey) {
        List<Deployment> list = repositoryService.createDeploymentQuery()
                .deploymentName(DESIGN_DEPLOYMENT_PREFIX + processKey)
                .orderByDeploymentTime()
                .desc()
                .listPage(0, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private ProcessDefinition latestDefinition(String processKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
    }

    private String readDesignResource(Deployment deployment) {
        InputStream in = repositoryService.getResourceAsStream(
                deployment.getId(), DESIGN_RESOURCE_NAME);
        if (in == null) {
            return LogicFlowGraphConverter.newStarterGraph(
                    deployment.getName().substring(DESIGN_DEPLOYMENT_PREFIX.length()));
        }
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    /**
     * 从已部署的 BPMN 里把 {@code camunda:properties} 中的 {@code wf:graph} 抠出来。
     *
     * <p>刻意用字符串定位而不是解析 XML: 这一步只是兜底, 拿不到就返回空图,
     * 解析失败不该让整个接口 500。
     */
    private String extractGraphFromDefinition(ProcessDefinition def) {
        String key = def.getKey();
        InputStream in = repositoryService.getResourceAsStream(
                def.getDeploymentId(), def.getResourceName());
        if (in == null) {
            return LogicFlowGraphConverter.newStarterGraph(key);
        }
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name())) {
            s.useDelimiter("\\A");
            String xml = s.hasNext() ? s.next() : "";
            int start = xml.indexOf("name=\"wf:graph\"");
            if (start < 0) {
                return LogicFlowGraphConverter.newStarterGraph(key);
            }
            int open = xml.indexOf('>', start);
            int close = xml.indexOf('<', open);
            if (open < 0 || close < 0) {
                return LogicFlowGraphConverter.newStarterGraph(key);
            }
            return xml.substring(open + 1, close);
        }
    }

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "未知错误";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }
}
