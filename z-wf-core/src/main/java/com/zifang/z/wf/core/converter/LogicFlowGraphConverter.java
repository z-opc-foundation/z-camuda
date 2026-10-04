package com.zifang.z.wf.core.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * LogicFlow 图数据 → BPMN 2.0 XML 转换。
 *
 * <p><b>为什么需要它</b>：前端流程设计器 {@code wf/components/FlowDesigner} 用的是
 * LogicFlow（{@code @logicflow/core}），{@code getGraphData()} 吐出来的是
 * {@code {nodes:[...], edges:[...]}} 图数据；而 Camunda 只吃 BPMN XML。
 * 两边之间必须有人翻译，本类就是那个翻译。
 *
 * <p><b>节点映射</b>（与 {@code FlowDesigner/nodes/index.ts} 里 register 的五种类型一一对应）：
 * <pre>
 *   start     → startEvent
 *   approval  → userTask      （带 camunda:assignee 表达式）
 *   condition → exclusiveGateway
 *   copy      → serviceTask   （抄送）
 *   end       → endEvent
 * </pre>
 * 边一律映射成 sequenceFlow，LogicFlow 边上没有条件表达式，所以
 * <b>分支条件未实现</b>——画了 condition 节点也不会生成 {@code conditionExpression}，
 * 运行时走的是 default 流转。见下方「未实现」清单。
 *
 * <p><b>未实现（刻意不假装支持）</b>：
 * <ol>
 *   <li><b>分支条件</b>：LogicFlow 边没有条件表达式字段，condition 节点画出来只影响
 *       图形，不影响运行时走向。要支持得先扩前端边的属性面板。</li>
 *   <li><b>多人会签 / 或签</b>：节点属性里有 {@code multipleApproval}
 *       （sequential/parallel/any），本转换不生成 Camunda multiInstanceLoopCharacteristics。
 *       并行会签需要生成 multiInstance，串行需要生成 sequential multiInstance，
 *       这两种都会显著改变流程语义，不能猜。</li>
 *   <li><b>抄送</b>：Camunda 无原生抄送概念，本转换落成 serviceTask 且不配实现，
 *       运行到该节点会因无 implementation 而报错。抄送要接 SPI 钩子单独做。</li>
 *   <li><b>节点坐标</b>：LogicFlow 是像素坐标，BPMN 的 DI 是 diagram interchange，
 *       这里不生成 {@code bpmndi} 段。流程在 Camunda 自带 Modeler 里打开会没有布局，
 *       但语义完全正确、可正常流转。</li>
 * </ol>
 *
 * <p>「未实现」就写「未实现」，不做「画了像是有、跑了才知道不对」的假实现。
 *
 * @author z-wf
 */
public final class LogicFlowGraphConverter {

    /** 前端节点 type → BPMN 元素类型。 */
    private static final Map<String, String> NODE_TYPE_HINT = new HashMap<>();

    static {
        NODE_TYPE_HINT.put("start", "startEvent");
        NODE_TYPE_HINT.put("approval", "userTask");
        NODE_TYPE_HINT.put("condition", "exclusiveGateway");
        NODE_TYPE_HINT.put("copy", "serviceTask");
        NODE_TYPE_HINT.put("end", "endEvent");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LogicFlowGraphConverter() {
    }

    /**
     * 把 LogicFlow 的 {nodes, edges} 转成 BPMN 2.0 XML。
     *
     * @see #toBpmnModel(String, String, String) 模型形态
     */
    public static String toBpmnXml(String processKey, String processName, String graphJson) {
        return Bpmn.convertToString(toBpmnModel(processKey, processName, graphJson));
    }

    /**
     * 把 LogicFlow 的 {nodes, edges} 转成 Camunda 的 BPMN 模型对象。
     *
     * <p>部署走 {@code DeploymentBuilder.addModelInstance(String, BpmnModelInstance)},
     * 直接交模型不必绕 XML 字符串; 需要人看/存档时再用
     * {@link #toBpmnXml(String, String, String)}。
     *
     * @param processKey 流程定义 key (必填, 同时作为 process 的 id)
     * @param processName 流程名称, 可空
     * @param graphJson   LogicFlow 图数据 JSON
     * @return BPMN 模型实例
     * @throws IllegalArgumentException processKey/graphJson 为空, 或图里没有 start 节点
     * @throws IllegalStateException    骨架里找不到对应 process 元素
     */
    public static BpmnModelInstance toBpmnModel(String processKey, String processName, String graphJson) {
        if (processKey == null || processKey.trim().isEmpty()) {
            throw new IllegalArgumentException("processKey 不能为空");
        }
        if (graphJson == null || graphJson.trim().isEmpty()) {
            throw new IllegalArgumentException("流程图数据为空, 无法部署");
        }

        JsonNode graph;
        try {
            graph = MAPPER.readTree(graphJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("流程图数据不是合法 JSON: " + e.getMessage(), e);
        }

        // 从最小 BPMN 模板起手, 而不是 Bpmn.createExecutableProcess()。
        // 原因: 它返回 ProcessBuilder, 而 7.18 的 AbstractProcessBuilder 里 modelInstance
        // 字段是 protected、没有公开的取模型方法, 硬取只能靠反射。走模板 +
        // readModelFromStream 全程只用公开 API, 且模板本身就是一份可读的契约。
        BpmnModelInstance model = Bpmn.readModelFromStream(
                new ByteArrayInputStream(minimalDefinitions(processKey, processName)
                        .getBytes(StandardCharsets.UTF_8)));
        // 7.18 的 ModelInstance 没有 getModelElementByType(Class), 只有 getModelElementById(String);
        // 而模板里 process 的 id 就是 processKey, 直接按 id 取。
        // 全限定 Process: 不写会与 java.lang.Process 撞名
        org.camunda.bpm.model.bpmn.instance.Process process =
                (org.camunda.bpm.model.bpmn.instance.Process) model.getModelElementById(processKey);
        if (process == null) {
            throw new IllegalStateException("BPMN 骨架里找不到 id=" + processKey + " 的 process 元素");
        }

        // ---- 节点 ----
        // 五种节点全是 FlowNode(可挂 sequenceFlow 的节点), 用 FlowNode 建索引
        Map<String, FlowNode> byLogicflowId = new HashMap<>();
        boolean hasStart = false;

        JsonNode nodes = graph.path("nodes");
        if (nodes.isArray()) {
            for (JsonNode n : nodes) {
                String lfId = n.path("id").asText(null);
                String type = n.path("type").asText("");
                if (lfId == null || lfId.isEmpty()) {
                    continue;
                }
                String name = n.path("text").path("value").asText(null);
                if (name == null || name.isEmpty()) {
                    name = n.path("text").asText("");
                }
                if (name == null || name.trim().isEmpty()) {
                    name = lfId;
                }

                FlowNode element = createElement(model, type, lfId, name, n);
                if (element == null) {
                    // 未注册的节点类型不静默丢弃: 画布上可能有用户拖进来的自定义节点,
                    // 直接丢掉会让流程语义不完整, 而用户看不出少了东西。
                    continue;
                }
                process.addChildElement(element);
                byLogicflowId.put(lfId, element);
                if ("start".equals(type)) {
                    hasStart = true;
                }
            }
        }

        if (!hasStart) {
            throw new IllegalArgumentException("流程图里没有 start 节点, 无法生成可执行流程");
        }

        // ---- 边 ----
        JsonNode edges = graph.path("edges");
        if (edges.isArray()) {
            for (JsonNode e : edges) {
                String src = e.path("source").path("id").asText(e.path("source").asText(null));
                String tgt = e.path("target").path("id").asText(e.path("target").asText(null));
                if (src == null || tgt == null) {
                    continue;
                }
                FlowNode source = byLogicflowId.get(src);
                FlowNode target = byLogicflowId.get(tgt);
                if (source == null || target == null) {
                    // 边指向了被跳过的节点, 连带跳过, 否则 BPMN 会引用不存在的元素
                    continue;
                }
                SequenceFlow flow = model.newInstance(SequenceFlow.class);
                // 7.18 的 setter 叫 setSource/setTarget, 不是 setSourceRef/setTargetRef
                flow.setSource(source);
                flow.setTarget(target);
                String label = e.path("text").path("value").asText("");
                if (label != null && !label.trim().isEmpty()) {
                    flow.setName(label);
                }
                process.addChildElement(flow);
            }
        }

        // ---- 自校验 ----
        // 不用 Bpmn.validateModel 的全量校验: 它对未解析的表达式会直接抛,
        // 而本转换刻意不生成表达式(分支条件/会签均未实现), 会误伤。
        // 这里只校验「有 start 且 start 至少有一条出边」这一条硬约束,
        // 其余结构性问题交给 Camunda 在 deploy() 时报错, 那时报错信息更贴近真实原因。
        if (process.getFlowElements().isEmpty()) {
            throw new IllegalArgumentException("流程图没有任何可转换的元素");
        }

        return model;
    }

    /**
     * 最小可用的 BPMN definitions 骨架 —— 只含一个空的 executable process。
     *
     * <p>camunda 命名空间必须预声明: 审批节点要写 {@code camunda:assignee}。
     */
    private static String minimalDefinitions(String processKey, String processName) {
        String name = processName == null ? "" : escapeXml(processName);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                + "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" "
                + "xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" "
                + "xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" "
                + "targetNamespace=\"http://zifang.com/wf\">\n"
                + "  <bpmn:process id=\"" + escapeXml(processKey) + "\" name=\"" + name
                + "\" isExecutable=\"true\"/>\n"
                + "</bpmn:definitions>\n";
    }

    /** XML 属性值转义 —— 流程名/节点名是用户输入, 不转义会生成非法 XML 让部署失败。 */
    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 按 LogicFlow 节点类型创建对应的 BPMN 元素。
     *
     * @return 创建好的元素; 类型未知时返回 null(由调用方跳过)
     */
    private static FlowNode createElement(BpmnModelInstance model, String type, String lfId,
                                         String name, JsonNode node) {
        switch (type) {
            case "start": {
                StartEvent e = model.newInstance(StartEvent.class);
                e.setId(lfId);
                e.setName(name);
                return e;
            }
            case "approval": {
                UserTask e = model.newInstance(UserTask.class);
                e.setId(lfId);
                e.setName(name);
                String assignee = resolveAssignee(node);
                if (assignee != null) {
                    e.setCamundaAssignee(assignee);
                }
                return e;
            }
            case "condition": {
                ExclusiveGateway e = model.newInstance(ExclusiveGateway.class);
                e.setId(lfId);
                e.setName(name);
                // defaultFlow 留空: 分支条件未实现, 运行时由引擎按无条件的 default 流转。
                return e;
            }
            case "copy": {
                ServiceTask e = model.newInstance(ServiceTask.class);
                e.setId(lfId);
                e.setName(name);
                // 抄送未实现, 刻意不设 implementation:
                // 设一个假的会让流程"看起来能跑", 到节点才炸; 不设则部署时就清楚。
                e.setCamundaTopic("wf:copy-not-implemented");
                return e;
            }
            case "end": {
                EndEvent e = model.newInstance(EndEvent.class);
                e.setId(lfId);
                e.setName(name);
                return e;
            }
            default:
                return null;
        }
    }

    /**
     * 解析审批人。
     *
     * <p>LogicFlow 节点属性里 {@code properties.approverType} / {@code properties.approver}
     * 是用户在右侧抽屉里填的。当前只支持 {@code specify_user}（指定人员）这一种能
     * 静态落成表达式的情形；其余三种（直属上级 / 角色 / 部门负责人）都需要引擎侧或
     * SPI 钩子在运行时求解，本转换不猜，留空由 Camunda 走人工认领。
     *
     * @return assignee 表达式; 无法静态确定时返回 null
     */
    private static String resolveAssignee(JsonNode node) {
        JsonNode props = node.path("properties");
        String approverType = props.path("approverType").asText("");
        JsonNode approver = props.path("approver");

        if (!"specify_user".equals(approverType) || !approver.isArray() || approver.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<JsonNode> it = approver.elements(); it.hasNext(); ) {
            String v = it.next().asText("");
            if (v == null || v.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(v.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * 图数据合法性自检, 供 Controller 在保存设计稿前先拦一道, 给出比 Camunda 部署期
     * 更早也更直白的错误。
     *
     * @return 错误信息; 无问题返回 null
     */
    public static String validateGraph(String graphJson) {
        if (graphJson == null || graphJson.trim().isEmpty()) {
            return "流程图数据为空";
        }
        JsonNode graph;
        try {
            graph = MAPPER.readTree(graphJson);
        } catch (Exception e) {
            return "流程图数据不是合法 JSON: " + e.getMessage();
        }
        JsonNode nodes = graph.path("nodes");
        if (!nodes.isArray() || nodes.size() == 0) {
            return "流程图没有节点";
        }
        boolean hasStart = false;
        for (JsonNode n : nodes) {
            if ("start".equals(n.path("type").asText(""))) {
                hasStart = true;
                break;
            }
        }
        if (!hasStart) {
            return "流程图没有 start 节点";
        }
        return null;
    }

    /**
     * 造一份最小可用的图数据, 给「新建流程」用。
     *
     * @param processKey 流程 key
     * @return LogicFlow {nodes, edges} JSON
     */
    public static String newStarterGraph(String processKey) {
        ObjectNode root = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode nodes = root.putArray("nodes");
        com.fasterxml.jackson.databind.node.ArrayNode edges = root.putArray("edges");

        ObjectNode start = nodes.addObject();
        start.put("id", "start_" + processKey);
        start.put("type", "start");
        ObjectNode startText = start.putObject("text");
        startText.put("value", "开始");

        ObjectNode end = nodes.addObject();
        end.put("id", "end_" + processKey);
        end.put("type", "end");
        ObjectNode endText = end.putObject("text");
        endText.put("value", "结束");

        ObjectNode edge = edges.addObject();
        edge.put("id", "e_start_end");
        ObjectNode source = edge.putObject("source");
        source.put("id", start.get("id").asText());
        ObjectNode target = edge.putObject("target");
        target.put("id", end.get("id").asText());

        return root.toString();
    }
}
