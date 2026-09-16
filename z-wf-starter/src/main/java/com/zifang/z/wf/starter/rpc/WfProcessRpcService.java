package com.zifang.z.wf.starter.rpc;

import java.util.List;
import java.util.Map;

/**
 * z-wf 流程服务 RPC 接口 (供外部通过 z-rpc 远程调用).
 *
 * <p>由 {@code WfProcessRpcServiceImpl} 实现并通过 {@code @ZRpcService} 暴露.</p>
 *
 * <p>典型调用流程:</p>
 * <ol>
 *   <li>{@link #startLeaveProcess(String, String, Map)} - 启动请假流程, 返回 processInstanceId</li>
 *   <li>{@link #getTodoTasks(String)} - 审批人查询待办任务</li>
 *   <li>{@link #completeTask(String, Map)} - 完成任务 (审批结果通过 variables 传)</li>
 * </ol>
 *
 * @author zifang
 * @since 1.0.0
 */
public interface WfProcessRpcService {

    /**
     * 启动请假流程.
     *
     * @param applicant 申请人
     * @param approver  审批人
     * @param variables 流程变量 (请假天数 / 原因等)
     * @return 流程实例 ID
     */
    String startLeaveProcess(String applicant, String approver, Map<String, Object> variables);

    /**
     * 查询审批人待办任务.
     *
     * @param approver 审批人
     * @return 待办任务列表 (id / name / assignee / createTime / processInstanceId)
     */
    List<Map<String, Object>> getTodoTasks(String approver);

    /**
     * 完成任务.
     *
     * @param taskId    任务 ID
     * @param variables 审批变量 (approvalResult: approved / rejected)
     */
    void completeTask(String taskId, Map<String, Object> variables);
}
