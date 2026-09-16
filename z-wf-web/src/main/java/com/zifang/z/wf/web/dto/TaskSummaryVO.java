package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 任务摘要VO（用于列表展示）
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "任务摘要")
public class TaskSummaryVO {

    @Schema(description = "任务ID")
    private String taskId; // 任务唯一标识

    @Schema(description = "任务名称")
    private String taskName; // 任务名称

    @Schema(description = "流程实例ID")
    private String processInstanceId; // 流程实例唯一标识

    @Schema(description = "流程定义名称")
    private String processName; // 流程定义名称

    @Schema(description = "发起人")
    private String initiator; // 流程发起人

    @Schema(description = "审批人/当前处理人")
    private String assignee; // 任务处理人

    @Schema(description = "任务创建时间")
    private String createTime; // 任务创建时间

    @Schema(description = "优先级")
    private Integer priority; // 任务优先级

    @Schema(description = "任务状态: todo-待办, done-已办")
    private String status; // 任务状态

    public TaskSummaryVO() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        TaskSummaryVO that = (TaskSummaryVO) o;
        return Objects.equals(taskId, that.taskId) && Objects.equals(taskName, that.taskName) && Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processName, that.processName) && Objects.equals(initiator, that.initiator) && Objects.equals(assignee, that.assignee) && Objects.equals(createTime, that.createTime) && Objects.equals(priority, that.priority) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, taskName, processInstanceId, processName, initiator, assignee, createTime, priority, status);
    }

    @Override
    public String toString() {
        return "TaskSummaryVO{" +
                "taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", processInstanceId='" + processInstanceId + '\'' +
                ", processName='" + processName + '\'' +
                ", initiator='" + initiator + '\'' +
                ", assignee='" + assignee + '\'' +
                ", createTime='" + createTime + '\'' +
                ", priority=" + priority +
                ", status='" + status + '\'' +
                '}';
    }
}
