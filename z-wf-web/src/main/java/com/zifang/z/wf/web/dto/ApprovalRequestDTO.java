package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 审批请求DTO
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "审批请求")
public class ApprovalRequestDTO {

    @Schema(description = "任务ID")
    private String taskId; // 任务唯一标识

    @Schema(description = "审批结果：approved-通过，rejected-拒绝")
    private String approvalResult; // 审批结果

    @Schema(description = "审批意见")
    private String comment; // 审批意见

    @Schema(description = "其他流程变量")
    private Map<String, Object> variables; // 其他流程变量

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getApprovalResult() {
        return approvalResult;
    }

    public void setApprovalResult(String approvalResult) {
        this.approvalResult = approvalResult;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
