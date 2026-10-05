package com.zifang.z.camuda.web.api;

import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 高级任务操作 Controller — 转办/委派/签收/撤回/跳转/强制推进.
 * <p>
 * API 基础路径: /api/wf/task
 * 所属模块: z-camuda-web
 * 鉴权: 由 z-ctc 统一拦截
 */
@RestController
@RequestMapping("/api/wf/task")
@Tag(name = "002_高级任务操作")
public class TaskOperationController {

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    /**
     * 转办任务: 将任务转交给其他人处理.
     *
     * @param taskId      任务ID
     * @param targetUser  目标用户
     * @param reason      转办原因
     * @return 成功信息
     */
    @PostMapping("/transfer")
    @Operation(summary = "001_转办任务")
    public Result<String> transferTask(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "目标用户") @RequestParam String targetUser,
            @Parameter(description = "转办原因") @RequestParam(required = false) String reason) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 设置任务负责人
        taskService.setAssignee(taskId, targetUser);

        // 添加评论
        if (reason != null && !reason.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(),
                    "转办给 " + targetUser + ": " + reason);
        }

        return Result.success("转办成功");
    }

    /**
     * 委派任务: 将任务委派给其他人处理，完成后返回给原处理人.
     *
     * @param taskId      任务ID
     * @param targetUser  目标用户
     * @param reason      委派原因
     * @return 成功信息
     */
    @PostMapping("/delegate")
    @Operation(summary = "002_委派任务")
    public Result<String> delegateTask(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "目标用户") @RequestParam String targetUser,
            @Parameter(description = "委派原因") @RequestParam(required = false) String reason) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 委派任务
        taskService.delegateTask(taskId, targetUser);

        // 添加评论
        if (reason != null && !reason.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(),
                    "委派给 " + targetUser + ": " + reason);
        }

        return Result.success("委派成功");
    }

    /**
     * 签收任务: 将任务签收给自己.
     *
     * @param taskId  任务ID
     * @param userId  用户ID
     * @return 成功信息
     */
    @PostMapping("/claim")
    @Operation(summary = "003_签收任务")
    public Result<String> claimTask(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "用户ID") @RequestParam String userId) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return Result.fail("任务不存在");
        }

        taskService.claim(taskId, userId);

        return Result.success("签收成功");
    }

    /**
     * 撤回任务: 发起人可以撤回尚未处理的任务.
     *
     * @param processInstanceId  流程实例ID
     * @param reason            撤回原因
     * @return 成功信息
     */
    @PostMapping("/withdraw")
    @Operation(summary = "004_撤回流程")
    public Result<String> withdrawProcess(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId,
            @Parameter(description = "撤回原因") @RequestParam(required = false) String reason) {

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (process == null) {
            return Result.fail("流程实例不存在或已结束");
        }

        // 终止流程实例
        runtimeService.deleteProcessInstance(processInstanceId,
                reason != null ? reason : "发起人撤回");

        return Result.success("撤回成功");
    }

    /**
     * 跳转任务: 强制跳转到指定节点.
     *
     * @param processInstanceId  流程实例ID
     * @param targetActivityId  目标活动节点ID
     * @param variables         变量
     * @return 成功信息
     */
    @PostMapping("/jump")
    @Operation(summary = "005_跳转到指定节点")
    public Result<String> jumpToNode(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId,
            @Parameter(description = "目标活动节点ID") @RequestParam String targetActivityId,
            @RequestBody(required = false) Map<String, Object> variables) {

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (process == null) {
            return Result.fail("流程实例不存在或已结束");
        }

        try {
            // 获取当前执行中的活动
            Execution execution = runtimeService.createExecutionQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (execution == null) {
                return Result.fail("未找到执行中的流程实例");
            }

            // 强制移动到目标活动
            runtimeService.createModification(processInstanceId)
                    .startBeforeActivity(targetActivityId)
                    .execute();

            return Result.success("跳转成功");
        } catch (Exception e) {
            return Result.fail("跳转失败: " + e.getMessage());
        }
    }

    /**
     * 强制推进: 跳过当前节点，直接完成.
     *
     * @param processInstanceId  流程实例ID
     * @param variables         变量
     * @return 成功信息
     */
    @PostMapping("/force-complete")
    @Operation(summary = "006_强制推进(跳过当前节点)")
    public Result<String> forceComplete(
            @Parameter(description = "流程实例ID") @RequestParam String processInstanceId,
            @RequestBody(required = false) Map<String, Object> variables) {

        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (process == null) {
            return Result.fail("流程实例不存在或已结束");
        }

        try {
            // 查找当前活动的任务
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (task != null) {
                // 完成当前任务
                taskService.complete(task.getId(), variables);
                return Result.success("强制推进成功");
            } else {
                return Result.fail("未找到待处理的任务");
            }
        } catch (Exception e) {
            return Result.fail("强制推进失败: " + e.getMessage());
        }
    }
}
