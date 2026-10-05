package com.zifang.z.camuda.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程详情VO
 */
@Schema(description = "流程详情")
public class ProcessDetailVO {

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程定义ID")
    private String processDefinitionId;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程Key")
    private String processKey;

    @Schema(description = "发起人")
    private String initiator;

    @Schema(description = "流程状态: running-运行中, completed-已完成, suspended-已挂起, terminated-已终止")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "总耗时")
    private String duration;

    @Schema(description = "当前节点/任务名称")
    private String currentTaskName;

    @Schema(description = "当前处理人")
    private String currentAssignee;

    @Schema(description = "业务数据/表单数据")
    private Map<String, Object> formData;

    @Schema(description = "流程审批历史")
    private List<ApprovalNodeVO> historyNodes;

    public ProcessDetailVO() {
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

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
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

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public List<ApprovalNodeVO> getHistoryNodes() {
        return historyNodes;
    }

    public void setHistoryNodes(List<ApprovalNodeVO> historyNodes) {
        this.historyNodes = historyNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        ProcessDetailVO that = (ProcessDetailVO) o;
        return Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(processName, that.processName) && Objects.equals(processKey, that.processKey) && Objects.equals(initiator, that.initiator) && Objects.equals(status, that.status) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime) && Objects.equals(duration, that.duration) && Objects.equals(currentTaskName, that.currentTaskName) && Objects.equals(currentAssignee, that.currentAssignee) && Objects.equals(formData, that.formData) && Objects.equals(historyNodes, that.historyNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processInstanceId, processDefinitionId, processName, processKey, initiator, status, startTime, endTime, duration, currentTaskName, currentAssignee, formData, historyNodes);
    }

    @Override
    public String toString() {
        return "ProcessDetailVO{" +
                "processInstanceId='" + processInstanceId + '\'' +
                ", processDefinitionId='" + processDefinitionId + '\'' +
                ", processName='" + processName + '\'' +
                ", processKey='" + processKey + '\'' +
                ", initiator='" + initiator + '\'' +
                ", status='" + status + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", duration='" + duration + '\'' +
                ", currentTaskName='" + currentTaskName + '\'' +
                ", currentAssignee='" + currentAssignee + '\'' +
                ", formData=" + formData +
                ", historyNodes=" + historyNodes +
                '}';
    }

    /**
     * 审批节点VO
     */
    @Schema(description = "审批节点")
    public static class ApprovalNodeVO {
        @Schema(description = "节点ID")
        private String nodeId;

        @Schema(description = "节点名称")
        private String nodeName;

        @Schema(description = "节点类型: startEvent-开始, userTask-用户任务, exclusiveGateway-排他网关, endEvent-结束")
        private String nodeType;

        @Schema(description = "处理人")
        private String assignee;

        @Schema(description = "开始时间")
        private String startTime;

        @Schema(description = "结束时间")
        private String endTime;

        @Schema(description = "耗时")
        private String duration;

        @Schema(description = "审批结果: approved-通过, rejected-拒绝, transfer-转办, delegate-委托")
        private String result;

        @Schema(description = "审批意见/备注")
        private String comment;

        public ApprovalNodeVO() {
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
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

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
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

            ApprovalNodeVO that = (ApprovalNodeVO) o;
            return Objects.equals(nodeId, that.nodeId) && Objects.equals(nodeName, that.nodeName) && Objects.equals(nodeType, that.nodeType) && Objects.equals(assignee, that.assignee) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime) && Objects.equals(duration, that.duration) && Objects.equals(result, that.result) && Objects.equals(comment, that.comment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, nodeName, nodeType, assignee, startTime, endTime, duration, result, comment);
        }

        @Override
        public String toString() {
            return "ApprovalNodeVO{" +
                    "nodeId='" + nodeId + '\'' +
                    ", nodeName='" + nodeName + '\'' +
                    ", nodeType='" + nodeType + '\'' +
                    ", assignee='" + assignee + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", endTime='" + endTime + '\'' +
                    ", duration='" + duration + '\'' +
                    ", result='" + result + '\'' +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
}
