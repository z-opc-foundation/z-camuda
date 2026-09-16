package com.zifang.z.wf.web.api;

import com.zifang.z.wf.core.service.LeaveProcessService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假流程接口 (Camunda 引擎).
 * <p>
 * API 基础路径: /api/leave
 * 所属模块: z-wf-starter
 * 鉴权: 由 z-ctc 统一拦截
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/leave/start         — 启动请假流程</li>
 *   <li>GET  /api/leave/todo          — 查询审批人待办任务 (Camunda Task 列表)</li>
 *   <li>GET  /api/leave/getApprovalTasks — 查询审批人待办任务 (DTO 形式)</li>
 *   <li>POST /api/leave/complete      — 完成审批任务</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/leave")
public class LeaveProcessController {

    @Resource
    private LeaveProcessService leaveProcessService;

    @Resource
    private TaskService taskService;

    /**
     * 启动请假流程, 申请人 / 审批人作为流程变量, days / reason 作为业务变量.
     * <p>
     * 请求体示例:
     * <pre>{@code
     * {
     *   "applicant": "zhangsan",
     *   "approver":  "lisi",
     *   "days":      3,
     *   "reason":    "生病请假"
     * }
     * }</pre>
     *
     * @param params 启动参数 (含 applicant / approver / days / reason)
     * @return 包含 code / message / processInstanceId 的结果 Map
     */
    @PostMapping("/start")
    public Map<String, String> startLeaveProcess(@RequestBody Map<String, Object> params) {
        String applicant = (String) params.get("applicant");
        String approver = (String) params.get("approver");

        // 封装流程变量（请假天数、原因）
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", params.get("days"));
        variables.put("reason", params.get("reason"));

        // 启动流程
        String processInstanceId = leaveProcessService.startLeaveProcess(applicant, approver, variables);

        Map<String, String> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "流程启动成功");
        result.put("processInstanceId", processInstanceId);
        return result;
    }

    /**
     * 查询指定审批人的待办任务 (Camunda Task 原始实体列表).
     * <p>
     * 示例: {@code GET /api/leave/todo?approver=lisi}
     *
     * @param approver 审批人 ID
     * @return 包含 code / todoTasks / count 的结果 Map
     */
    @GetMapping("/todo")
    public Map<String, Object> getTodoTasks(@RequestParam String approver) {
        List<Task> todoTasks = leaveProcessService.getTodoTasksByApprover(approver);

        // 将 Camunda Task 对象转换为 DTO，避免序列化问题
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<TodoTaskDTO> todoTaskDTOS = new ArrayList<>();
        for (Task task : todoTasks) {
            TodoTaskDTO dto = new TodoTaskDTO();
            dto.setTaskId(task.getId());
            dto.setTaskName(task.getName());
            dto.setAssignee(task.getAssignee());
            dto.setProcessInstanceId(task.getProcessInstanceId());
            dto.setProcessDefinitionId(task.getProcessDefinitionId());
            dto.setCreateTime(sdf.format(task.getCreateTime()));
            todoTaskDTOS.add(dto);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "200");
        result.put("todoTasks", todoTaskDTOS);
        result.put("count", todoTaskDTOS.size());
        return result;
    }

    /**
     * 查询指定审批人在请假流程 (processDefinitionKey = leaveProcess) 下的待办任务, 以 {@link TodoTaskDTO} 形式返回.
     *
     * @param approver 审批人 ID
     * @return 包含 code / msg / todoTasks 的结果 Map
     */
    @GetMapping("/getApprovalTasks")
    public Map<String, Object> getApprovalTasks(@RequestParam String approver) {
        Map<String, Object> result = new HashMap<>();
        List<TodoTaskDTO> todoTaskDTOS = new ArrayList<>();

        // 1. 查询指定审批人的待办任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(approver)
                .processDefinitionKey("leaveProcess")
                .list();

        // 2. 将Camunda的Task实体转换为自定义DTO（仅保留业务字段）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Task task : tasks) {
            TodoTaskDTO dto = new TodoTaskDTO();
            dto.setTaskId(task.getId());
            dto.setTaskName(task.getName());
            dto.setAssignee(task.getAssignee());
            dto.setProcessInstanceId(task.getProcessInstanceId());
            dto.setProcessDefinitionId(task.getProcessDefinitionId());
//            dto.setProcessDefinitionName(task.getProcessDefinitionName());
            dto.setCreateTime(sdf.format(task.getCreateTime()));

            todoTaskDTOS.add(dto);
        }

        // 3. 返回封装结果（避免直接返回List，统一返回格式）
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("todoTasks", todoTaskDTOS);
        return result;
    }


    /**
     * 完成审批任务, 将审批结果 (approved / rejected) 作为流程变量写入.
     * <p>
     * 请求体示例:
     * <pre>{@code
     * {
     *   "taskId":         "任务ID (从待办接口获取)",
     *   "approvalResult": "approved"
     * }
     * }</pre>
     *
     * @param params 审批参数 (含 taskId / approvalResult)
     * @return 包含 code / message 的结果 Map
     */
    @PostMapping("/complete")
    public Map<String, String> completeApproval(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String approvalResult = (String) params.get("approvalResult");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", approvalResult);

        leaveProcessService.completeApprovalTask(taskId, variables);

        Map<String, String> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "审批完成");
        return result;
    }
}