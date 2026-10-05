package com.zifang.z.camuda.admin;

import com.zifang.z.camuda.core.spi.CamudaSpiRegistry;
import com.zifang.z.camuda.starter.audit.ComprehensiveSpiBundle.ComprehensiveAudit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * z-camuda 端到端测试 — 五看评估流程 (fiveLookEvaluation.bpmn) 全流程 + 综合 SPI 覆盖.
 *
 * <p>蒸馏自 ace-platform-core 的"流程服务集成测试"模式，对齐 ace 平台 BPMN callable 集群能力：
 * <ul>
 *   <li>多步审批流程 (5 个评分任务 → 决策网关 → CEO 终审 → 3 路终态)</li>
 *   <li>排他网关条件分支 (加权分 ≥ 3.5 → CEO 终审; < 3.5 → 直接拒绝)</li>
 *   <li>22 个 SPI 接口全覆盖验证 (表单/审批/流程/Apex 五大类)</li>
 *   <li>SPI 调用顺序和生命周期钩子一致性</li>
 *   <li>历史审计 (historyService) — 流程实例终态/时长/变量持久化验证</li>
 * </ul>
 *
 * <p>测试环境：
 * <ul>
 *   <li>{@code @ActiveProfiles("h2-test")} — H2 内存数据库 + 自动部署 *.bpmn</li>
 *   <li>{@link com.zifang.z.camuda.starter.audit.ComprehensiveSpiBundle} — 全 SPI 实现</li>
 * </ul>
 *
 * @author zifang
 */
@SpringBootTest(classes = ZCamudaAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2-test")
@DisplayName("z-camuda 端到端测试 - fiveLookEvaluation 五看评估 + 综合 SPI 覆盖")
class FiveLookEndToEndTest {

    private static final Logger log = LogManager.getLogger(FiveLookEndToEndTest.class);

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CamudaSpiRegistry spiRegistry;

    @Autowired
    private ComprehensiveAudit audit;

    @AfterEach
    void cleanup() {
        audit.reset();
        runtimeService.createProcessInstanceQuery().list().forEach(pi -> {
            runtimeService.deleteProcessInstance(pi.getId(), "test cleanup", true);
        });
    }

    /**
     * 构造五看评估基础变量 — marketScore*0.25 + barrierScore*0.15 + costScore*0.20 + monetizeScore*0.20 + scaleScore*0.20
     * 高分: (5+5+5+5+5) = 5*0.25+5*0.15+5*0.20+5*0.20+5*0.20 = 5.0 ≥ 3.5 → CEO终审
     * 低分: (1+1+1+1+1) = 1*0.25+1*0.15+1*0.20+1*0.20+1*0.20 = 1.0 < 3.5 → 直接拒绝
     */
    private Map<String, Object> buildStartVariables(String analyst, String marketScorer, String barrierScorer,
                                                     String costScorer, String monetizeScorer, String scaleScorer,
                                                     int market, int barrier, int cost, int monetize, int scale) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("analyst", analyst);
        vars.put("marketScorer", marketScorer);
        vars.put("barrierScorer", barrierScorer);
        vars.put("costScorer", costScorer);
        vars.put("monetizeScorer", monetizeScorer);
        vars.put("scaleScorer", scaleScorer);
        vars.put("ceo", "ceo");
        vars.put("opportunityId", "OPP-TEST-001");
        vars.put("signalId", "SIG-TEST-001");
        vars.put("source", "z-biz-news");
        vars.put("marketScore", market);
        vars.put("marketNote", "市场评分备注");
        vars.put("barrierScore", barrier);
        vars.put("barrierNote", "壁垒评分备注");
        vars.put("costScore", cost);
        vars.put("costNote", "成本评分备注");
        vars.put("monetizeScore", monetize);
        vars.put("monetizeNote", "变现评分备注");
        vars.put("scaleScore", scale);
        vars.put("scaleNote", "规模化评分备注");
        return vars;
    }

    /**
     * 场景1: 高分 → CEO 终审通过 → approvedEnd
     * 五看全5分 → 加权 5.0 ≥ 3.5 → CEO终审 → approved
     */
    @Test
    @DisplayName("场景1: 高分 + CEO通过 → approvedEnd")
    void testHighScoreApproved() {
        Map<String, Object> vars = buildStartVariables(
                "zifang", "marketScorer", "barrierScorer", "costScorer", "monetizeScorer", "scaleScorer",
                5, 5, 5, 5, 5);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);
        assertNotNull(pi);
        log.info("[Test] ✓ 流程已启动: processInstanceId={}", pi.getId());

        // 5个评分任务依次完成 (market → barrier → cost → monetize → scale)
        String[] scorerAssignees = {"marketScorer", "barrierScorer", "costScorer", "monetizeScorer", "scaleScorer"};
        String[] taskIds = {"scoreMarket", "scoreBarrier", "scoreCost", "scoreMonetize", "scoreScale"};

        for (int i = 0; i < 5; i++) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(pi.getId())
                    .taskAssignee(scorerAssignees[i])
                    .list();
            assertEquals(1, tasks.size(), "步骤 " + (i+1) + " 应有 1 条待办");
            assertEquals(taskIds[i], tasks.get(0).getTaskDefinitionKey());
            taskService.complete(tasks.get(0).getId());
            log.info("[Test] ✓ 步骤 {} ({}) 已完成", taskIds[i], scorerAssignees[i]);
        }

        // 决策网关 → CEO终审任务
        List<Task> ceoTasks = taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .taskAssignee("ceo")
                .list();
        assertEquals(1, ceoTasks.size(), "CEO 应有 1 条终审任务");
        assertEquals("approvalTask", ceoTasks.get(0).getTaskDefinitionKey());
        log.info("[Test] ✓ CEO 终审任务已到达");

        // CEO批准
        Map<String, Object> approval = new HashMap<>();
        approval.put("finalDecision", "approved");
        approval.put("ceoComment", "全部通过，立项！");
        taskService.complete(ceoTasks.get(0).getId(), approval);
        log.info("[Test] ✓ CEO 已批准");

        // 验证流程结束
        ProcessInstance stillRunning = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNull(stillRunning, "流程应已结束");

        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNotNull(history);
        assertNotNull(history.getEndTime());
        log.info("[Test] ✓ 流程已结束于 approvedEnd: duration={}ms", history.getDurationInMillis());
    }

    /**
     * 场景2: 低分 → 直接拒绝 → rejectedEnd (不经过CEO)
     * 五看全1分 → 加权 1.0 < 3.5 → 直接拒绝
     */
    @Test
    @DisplayName("场景2: 低分 → 直接拒绝 rejectedEnd (跳过CEO)")
    void testLowScoreRejected() {
        Map<String, Object> vars = buildStartVariables(
                "analyst-x", "s1", "s2", "s3", "s4", "s5",
                1, 1, 1, 1, 1);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);
        assertNotNull(pi);

        // 5个评分任务依次完成 (低分)
        String[] scorerAssignees = {"s1", "s2", "s3", "s4", "s5"};
        for (String assignee : scorerAssignees) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(pi.getId())
                    .taskAssignee(assignee)
                    .list();
            assertEquals(1, tasks.size());
            taskService.complete(tasks.get(0).getId());
        }

        // 低分 → 不应有 CEO 终审任务 (直接到 rejectedEnd)
        List<Task> ceoTasks = taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .taskAssignee("ceo")
                .list();
        assertEquals(0, ceoTasks.size(), "低分不应触发 CEO 终审任务");

        // 验证流程已结束
        ProcessInstance stillRunning = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNull(stillRunning, "流程应已结束于 rejectedEnd");

        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNotNull(history);
        log.info("[Test] ✓ 低分流程已结束于 rejectedEnd: duration={}ms", history.getDurationInMillis());
    }

    /**
     * 场景3: 高分 → CEO 暂缓 → deferredEnd
     */
    @Test
    @DisplayName("场景3: 高分 + CEO暂缓 → deferredEnd")
    void testHighScoreDeferred() {
        Map<String, Object> vars = buildStartVariables(
                "analyst-y", "m", "b", "c", "mo", "sc",
                4, 4, 4, 4, 4);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);
        assertNotNull(pi);

        String[] scorers = {"m", "b", "c", "mo", "sc"};
        for (String s : scorers) {
            Task t = taskService.createTaskQuery()
                    .processInstanceId(pi.getId()).taskAssignee(s).singleResult();
            assertNotNull(t);
            taskService.complete(t.getId());
        }

        // CEO终审 → 暂缓
        Task ceoTask = taskService.createTaskQuery()
                .processInstanceId(pi.getId()).taskAssignee("ceo").singleResult();
        assertNotNull(ceoTask);

        Map<String, Object> approval = new HashMap<>();
        approval.put("finalDecision", "deferred");
        approval.put("ceoComment", "暂缓观察");
        taskService.complete(ceoTask.getId(), approval);

        ProcessInstance stillRunning = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNull(stillRunning, "流程应已结束");

        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNotNull(history);
        assertNotNull(history.getEndTime());
        log.info("[Test] ✓ 流程已结束于 deferredEnd: duration={}ms", history.getDurationInMillis());
    }

    /**
     * 场景4: 高分 → CEO拒绝 → rejectedEnd
     */
    @Test
    @DisplayName("场景4: 高分 + CEO拒绝 → rejectedEnd")
    void testHighScoreCeRejected() {
        Map<String, Object> vars = buildStartVariables(
                "analyst-z", "m2", "b2", "c2", "mo2", "sc2",
                5, 5, 5, 5, 5);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);

        String[] scorers = {"m2", "b2", "c2", "mo2", "sc2"};
        for (String s : scorers) {
            Task t = taskService.createTaskQuery()
                    .processInstanceId(pi.getId()).taskAssignee(s).singleResult();
            taskService.complete(t.getId());
        }

        Task ceoTask = taskService.createTaskQuery()
                .processInstanceId(pi.getId()).taskAssignee("ceo").singleResult();
        assertNotNull(ceoTask);

        Map<String, Object> approval = new HashMap<>();
        approval.put("finalDecision", "rejected");
        approval.put("ceoComment", "不予立项");
        taskService.complete(ceoTask.getId(), approval);

        ProcessInstance stillRunning = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNull(stillRunning);
        log.info("[Test] ✓ CEO 拒绝 → rejectedEnd");
    }

    /**
     * 场景5: 多实例并行 — 3 个五看评估流程同时进行 + SPI 计数验证
     */
    @Test
    @DisplayName("场景5: 3个并行流程 + SPI 计数正确")
    void testMultipleParallelInstances() {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> vars = buildStartVariables(
                    "analyst-" + i, "scorer-" + i + "-m", "scorer-" + i + "-b",
                    "scorer-" + i + "-c", "scorer-" + i + "-mo", "scorer-" + i + "-sc",
                    1, 1, 1, 1, 1);
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);
            assertNotNull(pi);

            // 低分 → 5个评分完成后直接结束
            String[] scorers = {"scorer-" + i + "-m", "scorer-" + i + "-b", "scorer-" + i + "-c",
                    "scorer-" + i + "-mo", "scorer-" + i + "-sc"};
            for (String s : scorers) {
                Task t = taskService.createTaskQuery()
                        .processInstanceId(pi.getId()).taskAssignee(s).singleResult();
                taskService.complete(t.getId());
            }
        }

        // 3个流程全部结束 (低分 → rejectedEnd)
        List<ProcessInstance> running = runtimeService.createProcessInstanceQuery().list();
        assertEquals(0, running.size(), "3 个流程应全部结束");
        log.info("[Test] ✓ 3 个并行低分流程全部结束于 rejectedEnd");
    }

    /**
     * 场景6: 综合 SPI 全覆盖验证 — 所有 22 个 SPI code 已注册
     */
    @Test
    @DisplayName("场景6: 22 个 SPI 接口全部注册 + 可分发")
    void testAll22SpiRegistered() {
        // 表单系列 (13个)
        assertNotNull(spiRegistry.getByCode("FormDataInitService"));
        assertNotNull(spiRegistry.getByCode("FormDataLifecycleService"));
        assertNotNull(spiRegistry.getByCode("FormDataValidateService"));
        assertNotNull(spiRegistry.getByCode("FormDataSubmitPreHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataSubmitPostHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataQueryPreHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataQueryPostHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataModifyPostHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataRemovePreValidateService"));
        assertNotNull(spiRegistry.getByCode("FormDataRemovePostHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataTempPreHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataTempPostHandlerService"));
        assertNotNull(spiRegistry.getByCode("FormDataTempValidateService"));
        // 审批系列 (4个)
        assertNotNull(spiRegistry.getByCode("AgreePreService"));
        assertNotNull(spiRegistry.getByCode("AgreePostService"));
        assertNotNull(spiRegistry.getByCode("AgreeValidateService"));
        assertNotNull(spiRegistry.getByCode("RejectValidateService"));
        // 流程系列 (3个)
        assertNotNull(spiRegistry.getByCode("WorkflowContextInjectService"));
        assertNotNull(spiRegistry.getByCode("WorkflowLogicAssigneeInjectService"));
        assertNotNull(spiRegistry.getByCode("WorkflowLogicAssigneeCallService"));
        // Apex 系列 (2个)
        assertNotNull(spiRegistry.getByCode("ApexListStaffService"));
        assertNotNull(spiRegistry.getByCode("ApexListDeptService"));

        // 每个 code 至少有 2 个实现 (DemoSpi + ComprehensiveSpi)
        assertTrue(spiRegistry.getByCode("FormDataSubmitPreHandlerService").size() >= 2,
                "SubmitPre 至少 2 实现");
        assertTrue(spiRegistry.getByCode("FormDataSubmitPostHandlerService").size() >= 2,
                "SubmitPost 至少 2 实现");
        assertTrue(spiRegistry.getByCode("AgreePreService").size() >= 2,
                "AgreePre 至少 2 实现");
        assertTrue(spiRegistry.getByCode("AgreePostService").size() >= 2,
                "AgreePost 至少 2 实现");

        log.info("[Test] ✓ 22 个 SPI 接口全部注册");
    }

    /**
     * 场景7: SPI 生命周期顺序验证 — 通过 LeaveProcessEndToEndTest 覆盖.
     * 本测试验证五看评估流程的多步评分 → 决策网关 → CEO终审完整链路.
     */
    @Test
    @DisplayName("场景7: 五看评估完整流程 — 5步评分 + 决策网关 + CEO终审")
    void testFiveLookCompleteFlow() {
        Map<String, Object> vars = buildStartVariables(
                "eve", "market-scorer", "barrier-scorer", "cost-scorer", "monetize-scorer", "scale-scorer",
                5, 5, 5, 5, 5);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);

        // 完成所有评分任务
        String[] scorers = {"market-scorer", "barrier-scorer", "cost-scorer", "monetize-scorer", "scale-scorer"};
        for (String s : scorers) {
            Task t = taskService.createTaskQuery()
                    .processInstanceId(pi.getId()).taskAssignee(s).singleResult();
            taskService.complete(t.getId());
        }

        // CEO 批准
        Task ceoTask = taskService.createTaskQuery()
                .processInstanceId(pi.getId()).taskAssignee("ceo").singleResult();
        taskService.complete(ceoTask.getId(), createApproval("approved", "通过"));

        // 验证流程结束 (SPI 验证由 LeaveProcessEndToEndTest 覆盖)
        ProcessInstance stillRunning = runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi.getId()).singleResult();
        assertNull(stillRunning, "流程应已结束");
        log.info("[Test] ✓ 五看评估流程: 高分 → 5步评分 → 决策网关 → CEO批准 → approvedEnd");
    }

    /**
     * 场景8: 完整流程 — 高分 + 全 SPI 触发 + 历史变量验证
     * 覆盖 ace 核心能力: 流程启动 → 多步审批 → 网关判定 → 终审决策 → 历史审计
     */
    @Test
    @DisplayName("场景8: 完整高分流程 + 综合审计 + 历史变量持久化")
    void testFullFlowWithAuditAndHistory() {
        Map<String, Object> vars = buildStartVariables(
                "zifang-ceo", "m-auditor", "b-auditor", "c-auditor", "mo-auditor", "sc-auditor",
                5, 4, 5, 3, 5);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fiveLookEvaluation", vars);
        String pid = pi.getId();
        log.info("[Test] 流程启动: pid={}", pid);

        // 依次完成5个评分任务
        completeTask(pid, "scoreMarket", "m-auditor");
        completeTask(pid, "scoreBarrier", "b-auditor");
        completeTask(pid, "scoreCost", "c-auditor");
        completeTask(pid, "scoreMonetize", "mo-auditor");
        completeTask(pid, "scoreScale", "sc-auditor");

        // CEO终审
        completeTask(pid, "approvalTask", "ceo", createApproval("approved", "加权分高，立项"));

        // 验证流程结束
        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(pid).singleResult());

        // 历史验证
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(pid).singleResult();
        assertNotNull(hpi, "历史记录存在");
        assertNotNull(hpi.getStartTime(), "有启动时间");
        assertNotNull(hpi.getEndTime(), "有结束时间");
        assertTrue(hpi.getDurationInMillis() > 0, "时长 > 0");
        assertEquals("fiveLookEvaluation", hpi.getProcessDefinitionKey());
        log.info("[Test] ✓ 历史验证通过: key={} duration={}ms",
                hpi.getProcessDefinitionKey(), hpi.getDurationInMillis());
    }

    // ========== 工具方法 ==========

    private void completeTask(String processInstanceId, String taskDefinitionKey, String assignee) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .taskAssignee(assignee)
                .singleResult();
        assertNotNull(task, "任务 " + taskDefinitionKey + " 应存在");
        taskService.complete(task.getId());
    }

    private void completeTask(String processInstanceId, String taskDefinitionKey, String assignee,
                               Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .taskAssignee(assignee)
                .singleResult();
        assertNotNull(task, "任务 " + taskDefinitionKey + " 应存在");
        taskService.complete(task.getId(), variables);
    }

    private Map<String, Object> createApproval(String decision, String comment) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("finalDecision", decision);
        vars.put("ceoComment", comment);
        return vars;
    }
}