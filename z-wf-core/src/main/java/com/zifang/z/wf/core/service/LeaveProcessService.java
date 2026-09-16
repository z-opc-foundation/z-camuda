package com.zifang.z.wf.core.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 请假流程服务类，封装Camunda核心操作
 *
 * @author zifang
 * @since 1.0.0
 */
@Service
public class LeaveProcessService {

    @Resource
    private RuntimeService runtimeService; // Camunda 核心API：启动流程实例

    @Resource
    private TaskService taskService; // Camunda 核心API：处理任务（查询、完成）

    /**
     * 启动请假流程实例
     *
     * @param applicant 申请人
     * @param approver  审批人
     * @param variables 流程变量（请假天数、原因等）
     * @return 流程实例ID
     */
    public String startLeaveProcess(String applicant, String approver, Map<String, Object> variables) {
        // 设置流程变量（必须包含BPMN中定义的变量）
        variables.put("applicant", applicant);
        variables.put("approver", approver);

        // 启动流程实例（processId对应BPMN中的process id：leaveProcess）
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "leaveProcess",
                variables
        );
        return processInstance.getId();
    }

    /**
     * 根据审批人查询待办任务
     *
     * @param approver 审批人
     * @return 待办任务列表
     */
    public List<Task> getTodoTasksByApprover(String approver) {
        return taskService.createTaskQuery()
                .taskAssignee(approver)  // 按审批人筛选
                .active()                // 只查激活的任务
                .orderByTaskCreateTime()
                .desc()
                .list();
    }

    /**
     * 完成审批任务
     *
     * @param taskId    任务ID
     * @param variables 审批结果（approvalResult: approved/rejected）
     */
    public void completeApprovalTask(String taskId, Map<String, Object> variables) {
        // 完成任务并传递审批结果变量
        taskService.complete(taskId, variables);
    }
}