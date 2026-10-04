package com.zifang.z.wf.core.converter;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LogicFlowGraphConverter} 测试。
 *
 * <p>本模块此前没有任何测试依赖, 也就是没有任何会跑的测试。这个类如果写成 main()
 * 自测就会和 z-ctc 的 {@code AcDeptTreeServiceTest} 一样 —— 挂在 src/test 下、
 * 名字叫 ...Test, 但 surefire 报 {@code Tests run: 0}, 构建照样成功。
 * 所以这里用真 JUnit 5, 并且必须用 {@code mvn test} 实跑确认它真的执行了。
 */
class LogicFlowGraphConverterTest {

    /** start → approval(指定人员) → end, 最常见的单级审批图。 */
    private static final String SINGLE_APPROVAL_GRAPH = "{"
            + "\"nodes\":["
            + "  {\"id\":\"s\",\"type\":\"start\",\"text\":{\"value\":\"开始\"}},"
            + "  {\"id\":\"a\",\"type\":\"approval\",\"text\":{\"value\":\"主管审批\"},"
            + "   \"properties\":{\"approverType\":\"specify_user\",\"approver\":[\"zhangsan\"]}},"
            + "  {\"id\":\"e\",\"type\":\"end\",\"text\":{\"value\":\"结束\"}}"
            + "],"
            + "\"edges\":["
            + "  {\"id\":\"e1\",\"source\":{\"id\":\"s\"},\"target\":{\"id\":\"a\"}},"
            + "  {\"id\":\"e2\",\"source\":{\"id\":\"a\"},\"target\":{\"id\":\"e\"}}"
            + "]}";

    @Test
    @DisplayName("单级审批图: start/approval/end 各转成一个 BPMN 元素, 两条边转成 sequenceFlow")
    void singleApprovalGraph() {
        BpmnModelInstance model = LogicFlowGraphConverter.toBpmnModel("leave", "请假审批", SINGLE_APPROVAL_GRAPH);
        assertNotNull(model);

        Process process = (Process) model.getModelElementById("leave");
        assertNotNull(process, "应能按 id 取到 process");
        assertEquals("请假审批", process.getName());
        assertTrue(process.getFlowElements().size() >= 5,
                "3 个节点 + 2 条边 = 至少 5 个 FlowElement, 实际 " + process.getFlowElements().size());

        String xml = LogicFlowGraphConverter.toBpmnXml("leave", "请假审批", SINGLE_APPROVAL_GRAPH);
        // 注意: Camunda 序列化时把子元素写成默认命名空间(xmlns="...BPMN..."), 不是 bpmn: 前缀,
        // 所以这里按「无前缀的元素名」断言 —— 带前缀断言会在换序列化器时假失败。
        assertTrue(xml.contains("<startEvent"), "应含 startEvent, 实际:\n" + xml);
        assertTrue(xml.contains("<userTask"), "应含 userTask, 实际:\n" + xml);
        assertTrue(xml.contains("<endEvent"), "应含 endEvent, 实际:\n" + xml);
        assertTrue(xml.contains("sequenceFlow"), "边应转成 sequenceFlow");
        assertTrue(xml.contains("camunda:assignee"), "指定人员的审批节点应写出 camunda:assignee");
        assertTrue(xml.contains("zhangsan"), "assignee 应含指定的用户");
    }

    @Test
    @DisplayName("五种节点类型都能映射: start/approval/condition/copy/end")
    void allNodeTypesMapped() {
        String graph = "{"
                + "\"nodes\":["
                + "  {\"id\":\"s\",\"type\":\"start\"},"
                + "  {\"id\":\"c\",\"type\":\"condition\"},"
                + "  {\"id\":\"t1\",\"type\":\"approval\"},"
                + "  {\"id\":\"cp\",\"type\":\"copy\"},"
                + "  {\"id\":\"e\",\"type\":\"end\"}"
                + "],"
                + "\"edges\":["
                + "  {\"id\":\"x1\",\"source\":{\"id\":\"s\"},\"target\":{\"id\":\"c\"}},"
                + "  {\"id\":\"x2\",\"source\":{\"id\":\"c\"},\"target\":{\"id\":\"t1\"}},"
                + "  {\"id\":\"x3\",\"source\":{\"id\":\"t1\"},\"target\":{\"id\":\"cp\"}},"
                + "  {\"id\":\"x4\",\"source\":{\"id\":\"cp\"},\"target\":{\"id\":\"e\"}}"
                + "]}";
        String xml = LogicFlowGraphConverter.toBpmnXml("multi", "多节点", graph);
        assertTrue(xml.contains("<exclusiveGateway"), "condition → exclusiveGateway, 实际:\n" + xml);
        assertTrue(xml.contains("<serviceTask"), "copy → serviceTask");
        assertTrue(xml.contains("wf:copy-not-implemented"),
                "抄送节点要显式标注未实现, 而不是配个假 implementation 让人以为能跑");
    }

    @Test
    @DisplayName("非指定人员的审批节点不硬塞 assignee, 交回 Camunda 走认领")
    void nonStaticAssigneeLeftEmpty() {
        String graph = "{"
                + "\"nodes\":["
                + "  {\"id\":\"s\",\"type\":\"start\"},"
                + "  {\"id\":\"a\",\"type\":\"approval\",\"text\":{\"value\":\"主管审批\"},"
                + "   \"properties\":{\"approverType\":\"direct_leader\"}},"
                + "  {\"id\":\"e\",\"type\":\"end\"}"
                + "],"
                + "\"edges\":["
                + "  {\"id\":\"e1\",\"source\":{\"id\":\"s\"},\"target\":{\"id\":\"a\"}},"
                + "  {\"id\":\"e2\",\"source\":{\"id\":\"a\"},\"target\":{\"id\":\"e\"}}"
                + "]}";
        String xml = LogicFlowGraphConverter.toBpmnXml("leader", "上级审批", graph);
        assertTrue(xml.contains("<userTask"), "审批节点仍应转成 userTask, 实际:\n" + xml);
        assertTrue(!xml.contains("camunda:assignee"),
                "direct_leader 需要运行时求解, 转换期不该编一个 assignee 出来");
    }

    @Test
    @DisplayName("没有 start 节点要报错, 不能生成一个跑不起来的流程")
    void missingStartRejected() {
        String graph = "{\"nodes\":[{\"id\":\"a\",\"type\":\"approval\"}],\"edges\":[]}";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> LogicFlowGraphConverter.toBpmnModel("nostart", "无开始", graph));
        assertTrue(e.getMessage().contains("start"), "报错要说清是缺 start 节点, 实际: " + e.getMessage());
    }

    @Test
    @DisplayName("processKey 为空要报错")
    void emptyKeyRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LogicFlowGraphConverter.toBpmnModel("  ", "空key", SINGLE_APPROVAL_GRAPH));
    }

    @Test
    @DisplayName("流程名里的 XML 特殊字符必须转义, 否则生成非法 XML")
    void xmlSpecialCharsEscaped() {
        String graph = "{\"nodes\":["
                + "{\"id\":\"s\",\"type\":\"start\",\"text\":{\"value\":\"A&B\"}}"
                + "],\"edges\":[]}";
        String xml = LogicFlowGraphConverter.toBpmnXml("esc", "带\"引号\"和<尖括号>", graph);
        assertTrue(xml.contains("&quot;") || xml.contains("&lt;"),
                "流程名里的引号/尖括号应被转义, 实际: " + xml);
        // 转义后仍应能被 Camunda 解析回来
        assertDoesNotThrow(() -> LogicFlowGraphConverter.toBpmnModel("esc", "带\"引号\"和<尖括号>", graph));
    }

    @Test
    @DisplayName("指向未知节点的边要跳过, 不能让 BPMN 引用不存在的元素")
    void danglingEdgeSkipped() {
        String graph = "{\"nodes\":["
                + "{\"id\":\"s\",\"type\":\"start\"},"
                + "{\"id\":\"e\",\"type\":\"end\"}"
                + "],\"edges\":["
                + "{\"id\":\"ok\",\"source\":{\"id\":\"s\"},\"target\":{\"id\":\"e\"}},"
                + "{\"id\":\"bad\",\"source\":{\"id\":\"s\"},\"target\":{\"id\":\"ghost\"}}"
                + "]}";
        String xml = LogicFlowGraphConverter.toBpmnXml("dangle", "断边", graph);
        assertTrue(!xml.contains("ghost"), "不存在的节点不该出现在 BPMN 里");
    }

    @Test
    @DisplayName("validateGraph: 空/无节点/无 start 分别给出对应原因")
    void validateGraphMessages() {
        assertEquals("流程图数据为空", LogicFlowGraphConverter.validateGraph(null));
        assertEquals("流程图数据为空", LogicFlowGraphConverter.validateGraph("  "));
        assertEquals("流程图数据不是合法 JSON: ", LogicFlowGraphConverter.validateGraph("{不是json")
                .substring(0, "流程图数据不是合法 JSON: ".length()));
        assertEquals("流程图没有节点", LogicFlowGraphConverter.validateGraph("{\"nodes\":[]}"));
        assertEquals("流程图没有 start 节点",
                LogicFlowGraphConverter.validateGraph("{\"nodes\":[{\"id\":\"a\",\"type\":\"approval\"}]}"));
        assertTrue(LogicFlowGraphConverter.validateGraph(SINGLE_APPROVAL_GRAPH) == null, "正常图应通过校验");
    }

    @Test
    @DisplayName("newStarterGraph 生成的骨架图能直接被转换器接受")
    void starterGraphIsConvertible() {
        String graph = LogicFlowGraphConverter.newStarterGraph("fresh");
        assertTrue(LogicFlowGraphConverter.validateGraph(graph) == null, "骨架图应通过自检");
        assertDoesNotThrow(() -> LogicFlowGraphConverter.toBpmnModel("fresh", "新流程", graph),
                "骨架图应能被转换, 否则用户新建流程会卡死在第一步");
    }
}
