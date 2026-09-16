package com.zifang.z.wf.web.api;

import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程实例操作 Controller — 挂起/激活/批注/轨迹/概览.
 * <p>
 * API 基础路径: /api/wf/process
 * 所属模块: z-wf-web
 * 鉴权: 由 z-ctc 统一拦截
 */
@RestController
@RequestMapping("/api/wf/process")
@Tag(name = "003_流程实例操作")
public class ProcessOperationController {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private RepositoryService repositoryService;

    /**
     * 挂起流程实例: 暂停流程执行.
     *
     * @param processInstanceId  流程实例ID
     * @return 成功信息
     */
    @PostMapping("/suspend")
    @Operation(summary = "001_挂起流程实例")
    public Result<String> suspendProcess(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId) {

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (process == null) {
            return Result.fail("流程实例不存在或已结束");
        }

        if (process.isSuspended()) {
            return Result.fail("流程实例已处于挂起状态");
        }

        runtimeService.suspendProcessInstanceById(processInstanceId);

        return Result.success("挂起成功");
    }

    /**
     * 激活流程实例: 恢复流程执行.
     *
     * @param processInstanceId  流程实例ID
     * @return 成功信息
     */
    @PostMapping("/activate")
    @Operation(summary = "002_激活流程实例")
    public Result<String> activateProcess(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId) {

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (process == null) {
            return Result.fail("流程实例不存在或已结束");
        }

        if (!process.isSuspended()) {
            return Result.fail("流程实例未处于挂起状态");
        }

        runtimeService.activateProcessInstanceById(processInstanceId);

        return Result.success("激活成功");
    }

    /**
     * 添加批注: 在任务上添加评论/批注.
     *
     * @param taskId            任务ID
     * @param processInstanceId 流程实例ID
     * @param message           批注内容
     * @param userId            用户ID
     * @return 成功信息
     */
    @PostMapping("/comment")
    @Operation(summary = "003_添加批注")
    public Result<String> addComment(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId,
            @Parameter(description = "批注内容") @RequestParam String message,
            @Parameter(description = "用户ID") @RequestParam String userId) {

        try {
            taskService.addComment(taskId, processInstanceId, message);
            return Result.success("添加批注成功");
        } catch (Exception e) {
            return Result.fail("添加批注失败: " + e.getMessage());
        }
    }

    /**
     * 获取批注列表: 获取流程实例的所有批注.
     *
     * @param processInstanceId  流程实例ID
     * @return 批注列表
     */
    @GetMapping("/comments")
    @Operation(summary = "004_获取批注列表")
    public Result<List<Map<String, Object>>> getComments(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId) {

        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);

        List<Map<String, Object>> result = comments.stream().map(comment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("message", comment.getFullMessage());
            map.put("userId", comment.getUserId());
            map.put("time", sdf.format(comment.getTime()));
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取执行轨迹: 获取流程实例的执行历史节点.
     *
     * @param processInstanceId  流程实例ID
     * @return 执行轨迹列表
     */
    @GetMapping("/trail")
    @Operation(summary = "005_获取执行轨迹")
    public Result<List<Map<String, Object>>> getExecutionTrail(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId) {

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<Map<String, Object>> result = activities.stream()
                .filter(act -> act.getActivityType() != null)
                .map(act -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("activityId", act.getActivityId());
                    map.put("activityName", act.getActivityName());
                    map.put("activityType", act.getActivityType());
                    map.put("assignee", act.getAssignee());
                    map.put("startTime", sdf.format(act.getStartTime()));
                    if (act.getEndTime() != null) {
                        map.put("endTime", sdf.format(act.getEndTime()));
                        map.put("duration", act.getDurationInMillis());
                    }
                    return map;
                }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 流程概览: 获取流程实例的概览信息（谁在什么时间做了什么操作）.
     *
     * @param processInstanceId  流程实例ID
     * @return 概览信息
     */
    @GetMapping("/overview")
    @Operation(summary = "006_流程概览")
    public Result<Map<String, Object>> getProcessOverview(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId) {

        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (hpi == null) {
            return Result.fail("流程实例不存在");
        }

        Map<String, Object> overview = new HashMap<>();

        // 基本信息
        overview.put("processInstanceId", processInstanceId);
        overview.put("startTime", sdf.format(hpi.getStartTime()));
        overview.put("startUserId", hpi.getStartUserId());
        if (hpi.getEndTime() != null) {
            overview.put("endTime", sdf.format(hpi.getEndTime()));
            overview.put("duration", hpi.getDurationInMillis());
        }

        // 获取流程定义名称
        org.camunda.bpm.engine.repository.ProcessDefinition pd =
                repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(hpi.getProcessDefinitionId())
                        .singleResult();
        if (pd != null) {
            overview.put("processName", pd.getName());
        }

        // 获取当前任务
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (currentTask != null) {
            overview.put("currentTask", currentTask.getName());
            overview.put("currentAssignee", currentTask.getAssignee());
            overview.put("status", "running");
        } else {
            overview.put("status", "completed");
        }

        // 获取历史任务列表
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<Map<String, Object>> taskHistory = historicTasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskName", task.getName());
            taskMap.put("assignee", task.getAssignee());
            taskMap.put("startTime", sdf.format(task.getStartTime()));
            if (task.getEndTime() != null) {
                taskMap.put("endTime", sdf.format(task.getEndTime()));
                taskMap.put("duration", task.getDurationInMillis());
            }
            taskMap.put("status", task.getEndTime() != null ? "completed" : "pending");
            return taskMap;
        }).collect(Collectors.toList());

        overview.put("taskHistory", taskHistory);

        return Result.success(overview);
    }
}
