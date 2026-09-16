package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 任务详情VO
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "任务详情")
public class TaskDetailVO {

    @Schema(description = "任务ID")
    private String taskId; // 任务唯一标识

    @Schema(description = "任务名称")
    private String taskName; // 任务名称

    @Schema(description = "任务描述")
    private String description; // 任务描述

    @Schema(description = "流程实例ID")
    private String processInstanceId; // 流程实例唯一标识

    @Schema(description = "流程定义ID")
    private String processDefinitionId; // 流程定义唯一标识

    @Schema(description = "流程定义名称")
    private String processName; // 流程定义名称

    @Schema(description = "发起人")
    private String initiator; // 流程发起人

    @Schema(description = "审批人/当前处理人")
    private String assignee; // 任务处理人

    @Schema(description = "任务创建时间")
    private String createTime; // 任务创建时间

    @Schema(description = "任务到期时间")
    private String dueDate; // 任务到期时间

    @Schema(description = "优先级")
    private Integer priority; // 任务优先级

    @Schema(description = "流程变量/表单数据")
    private Map<String, Object> formData; // 流程变量

    @Schema(description = "审批历史记录")
    private List<ApprovalHistoryVO> approvalHistory; // 审批历史记录

    public TaskDetailVO() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
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

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public List<ApprovalHistoryVO> getApprovalHistory() {
        return approvalHistory;
    }

    public void setApprovalHistory(List<ApprovalHistoryVO> approvalHistory) {
        this.approvalHistory = approvalHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        TaskDetailVO that = (TaskDetailVO) o;
        return Objects.equals(taskId, that.taskId) && Objects.equals(taskName, that.taskName) && Objects.equals(description, that.description) && Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(processName, that.processName) && Objects.equals(initiator, that.initiator) && Objects.equals(assignee, that.assignee) && Objects.equals(createTime, that.createTime) && Objects.equals(dueDate, that.dueDate) && Objects.equals(priority, that.priority) && Objects.equals(formData, that.formData) && Objects.equals(approvalHistory, that.approvalHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, taskName, description, processInstanceId, processDefinitionId, processName, initiator, assignee, createTime, dueDate, priority, formData, approvalHistory);
    }

    @Override
    public String toString() {
        return "TaskDetailVO{" +
                "taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", description='" + description + '\'' +
                ", processInstanceId='" + processInstanceId + '\'' +
                ", processDefinitionId='" + processDefinitionId + '\'' +
                ", processName='" + processName + '\'' +
                ", initiator='" + initiator + '\'' +
                ", assignee='" + assignee + '\'' +
                ", createTime='" + createTime + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", priority=" + priority +
                ", formData=" + formData +
                ", approvalHistory=" + approvalHistory +
                '}';
    }

    /**
     * 审批历史记录内部类
     */
    @Schema(description = "审批历史记录")
    public static class ApprovalHistoryVO {
        @Schema(description = "活动/节点名称")
        private String activityName; // 节点名称

        @Schema(description = "处理人")
        private String assignee; // 节点处理人

        @Schema(description = "开始时间")
        private String startTime; // 节点开始时间

        @Schema(description = "结束时间")
        private String endTime; // 节点结束时间

        @Schema(description = "耗时")
        private String duration; // 节点耗时

        @Schema(description = "审批结果/意见")
        private String comment; // 审批意见

        public ApprovalHistoryVO() {
        }

        public String getActivityName() {
            return activityName;
        }

        public void setActivityName(String activityName) {
            this.activityName = activityName;
        }

        public String getAssignee() {
            return assignee;
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }

            if (o == null || getClass() != o.getClass()) return false;

            ApprovalHistoryVO that = (ApprovalHistoryVO) o;
            return Objects.equals(activityName, that.activityName) && Objects.equals(assignee, that.assignee) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime) && Objects.equals(duration, that.duration) && Objects.equals(comment, that.comment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(activityName, assignee, startTime, endTime, duration, comment);
        }

        @Override
        public String toString() {
            return "ApprovalHistoryVO{" +
                    "activityName='" + activityName + '\'' +
                    ", assignee='" + assignee + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", endTime='" + endTime + '\'' +
                    ", duration='" + duration + '\'' +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
}
