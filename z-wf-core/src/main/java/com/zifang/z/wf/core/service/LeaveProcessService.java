package com.zifang.z.wf.core.service;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.WfAgreePostService;
import com.zifang.z.wf.core.spi.WfAgreePreService;
import com.zifang.z.wf.core.spi.WfExtensionContext;
import com.zifang.z.wf.core.spi.WfFormDataSubmitPostHandlerService;
import com.zifang.z.wf.core.spi.WfFormDataSubmitPreHandlerService;
import com.zifang.z.wf.core.spi.WfSpiRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假流程服务类 — 封装 Camunda 核心操作 + 触发 z-wf SPI 钩子.
 *
 * <p>本服务在流程生命周期关键节点（启动/完成）调用 {@link WfSpiRegistry}
 * 分发的 {@link WfFormDataSubmitPreHandlerService} /
 * {@link WfFormDataSubmitPostHandlerService} /
 * {@link WfAgreePreService} /
 * {@link WfAgreePostService} SPI，
 * 业务方实现这些接口即可在表单发起前/后、审批前/后注入自定义逻辑.
 *
 * <p>蒸馏自 ace-platform-core
 * {@code C2fOpsFormDataPreServiceImpl} + {@code AgreePreServiceImpl} 的 SPI 调度模式.
 *
 * @author zifang
 * @since 1.0.0
 */
@Service
public class LeaveProcessService {

    private static final Logger log = LogManager.getLogger(LeaveProcessService.class);

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private WfSpiRegistry spiRegistry;

    /**
     * 启动请假流程实例 — 触发 SubmitPre → 引擎启动 → SubmitPost.
     *
     * @param applicant 申请人
     * @param approver  审批人
     * @param variables 流程变量（请假天数、原因等）
     * @return 流程实例ID
     */
    public String startLeaveProcess(String applicant, String approver, Map<String, Object> variables) {
        // 1. 构造 SPI 上下文
        WfExtensionContext context = new WfExtensionContext()
                .withAppCode(applicant)
                .withModelCode("leave")
                .withWorkflowDefinitionKey("leaveProcess")
                .withFormCode("leaveForm");

        // 2. 触发表单发起前 SPI（业务方可修改 variables）
        Map<String, Object> beforeData = new HashMap<>(variables);
        for (Object spi : spiRegistry.getByCode("FormDataSubmitPreHandlerService")) {
            WfFormDataSubmitPreHandlerService handler = (WfFormDataSubmitPreHandlerService) spi;
            Result<Map<String, Object>> r = handler.preHandler(context, beforeData);
            if (r != null && r.isSuccess() && r.getData() != null) {
                beforeData = r.getData();
            } else if (r != null && !r.isSuccess()) {
                log.warn("[SubmitPre] SPI returned failure: {} - skip variable update", r.getMessage());
            }
        }

        // 3. 设置流程变量（必须包含 BPMN 中定义的变量）
        beforeData.put("applicant", applicant);
        beforeData.put("approver", approver);

        // 4. 启动流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leaveProcess", beforeData);

        // 5. 触发表单发起后 SPI
        context.withProcessInstanceId(processInstance.getId());
        for (Object spi : spiRegistry.getByCode("FormDataSubmitPostHandlerService")) {
            WfFormDataSubmitPostHandlerService handler = (WfFormDataSubmitPostHandlerService) spi;
            Result<Boolean> r = handler.postHandler(context, beforeData);
            if (r != null && !r.isSuccess()) {
                log.warn("[SubmitPost] SPI returned failure: {} - continuing", r.getMessage());
            }
        }
        return processInstance.getId();
    }

    /**
     * 根据审批人查询待办任务.
     *
     * @param approver 审批人
     * @return 待办任务列表
     */
    public List<Task> getTodoTasksByApprover(String approver) {
        return taskService.createTaskQuery()
                .taskAssignee(approver)
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();
    }

    /**
     * 完成审批任务 — 触发 AgreePre → 引擎 complete → AgreePost.
     */
    public void completeApprovalTask(String taskId, Map<String, Object> variables) {
        // 1. 查询任务获取上下文信息
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        // 2. 构造 SPI 上下文（从 task processDefinitionId 提取 processDefinitionKey — 形式为 "key:version:id"）
        String processDefinitionId = task.getProcessDefinitionId();
        String processDefinitionKey = processDefinitionId == null
                ? "unknown"
                : processDefinitionId.split(":")[0];
        WfExtensionContext context = new WfExtensionContext()
                .withWorkflowDefinitionKey(processDefinitionKey)
                .withProcessInstanceId(task.getProcessInstanceId())
                .withFormCode("approvalForm");
        // 把审批人变量加入
        Map<String, Object> dataWithAssignee = new HashMap<>(variables);
        dataWithAssignee.put("approver", task.getAssignee());

        // 3. 触发审批前 SPI
        Map<String, Object> beforeData = dataWithAssignee;
        for (Object spi : spiRegistry.getByCode("AgreePreService")) {
            WfAgreePreService handler = (WfAgreePreService) spi;
            Result<Map<String, Object>> r = handler.preHandler(context, beforeData);
            if (r != null && r.isSuccess() && r.getData() != null) {
                beforeData = r.getData();
            } else if (r != null && !r.isSuccess()) {
                log.warn("[AgreePre] SPI returned failure: {} - skip variable update", r.getMessage());
            }
        }

        // 4. 完成任务
        taskService.complete(taskId, beforeData);

        // 5. 触发审批后 SPI
        for (Object spi : spiRegistry.getByCode("AgreePostService")) {
            WfAgreePostService handler = (WfAgreePostService) spi;
            Result<Boolean> r = handler.postHandler(context, beforeData);
            if (r != null && !r.isSuccess()) {
                log.warn("[AgreePost] SPI returned failure: {} - continuing", r.getMessage());
            }
        }
    }
}
