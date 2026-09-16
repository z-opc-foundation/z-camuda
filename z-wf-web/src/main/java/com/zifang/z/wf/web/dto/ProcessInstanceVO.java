package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 流程实例VO（我发起的流程）
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "流程实例")
public class ProcessInstanceVO {

    @Schema(description = "流程实例ID")
    private String processInstanceId; // 流程实例唯一标识

    @Schema(description = "流程定义ID")
    private String processDefinitionId; // 流程定义唯一标识

    @Schema(description = "流程名称")
    private String processName; // 流程名称

    @Schema(description = "流程Key")
    private String processKey; // 流程定义Key

    @Schema(description = "发起人")
    private String initiator; // 流程发起人

    @Schema(description = "当前节点/任务名称")
    private String currentTaskName; // 当前正在处理的节点名称

    @Schema(description = "当前处理人")
    private String currentAssignee; // 当前任务处理人

    @Schema(description = "流程状态: running-运行中, completed-已完成, suspended-已挂起")
    private String status; // 流程状态

    @Schema(description = "开始时间")
    private String startTime; // 流程开始时间

    @Schema(description = "结束时间")
    private String endTime; // 流程结束时间

    public ProcessInstanceVO() {
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

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getCurrentTaskName() {
        return currentTaskName;
    }

    public void setCurrentTaskName(String currentTaskName) {
        this.currentTaskName = currentTaskName;
    }

    public String getCurrentAssignee() {
        return currentAssignee;
    }

    public void setCurrentAssignee(String currentAssignee) {
        this.currentAssignee = currentAssignee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        ProcessInstanceVO that = (ProcessInstanceVO) o;
        return Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(processName, that.processName) && Objects.equals(processKey, that.processKey) && Objects.equals(initiator, that.initiator) && Objects.equals(currentTaskName, that.currentTaskName) && Objects.equals(currentAssignee, that.currentAssignee) && Objects.equals(status, that.status) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processInstanceId, processDefinitionId, processName, processKey, initiator, currentTaskName, currentAssignee, status, startTime, endTime);
    }

    @Override
    public String toString() {
        return "ProcessInstanceVO{" +
                "processInstanceId='" + processInstanceId + '\'' +
                ", processDefinitionId='" + processDefinitionId + '\'' +
                ", processName='" + processName + '\'' +
                ", processKey='" + processKey + '\'' +
                ", initiator='" + initiator + '\'' +
                ", currentTaskName='" + currentTaskName + '\'' +
                ", currentAssignee='" + currentAssignee + '\'' +
                ", status='" + status + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
