package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Objects;

/**
 * 启动流程请求DTO
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "启动流程请求")
public class StartProcessRequestDTO {

    @Schema(description = "流程定义Key")
    private String processKey; // 流程定义Key

    @Schema(description = "业务Key")
    private String businessKey; // 业务唯一标识

    @Schema(description = "发起人/申请人")
    private String initiator; // 流程发起人

    @Schema(description = "流程标题/摘要")
    private String title; // 流程标题

    @Schema(description = "流程变量/表单数据")
    private Map<String, Object> variables; // 流程变量

    public StartProcessRequestDTO() {
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        StartProcessRequestDTO that = (StartProcessRequestDTO) o;
        return Objects.equals(processKey, that.processKey) && Objects.equals(businessKey, that.businessKey) && Objects.equals(initiator, that.initiator) && Objects.equals(title, that.title) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processKey, businessKey, initiator, title, variables);
    }

    @Override
    public String toString() {
        return "StartProcessRequestDTO{" +
                "processKey='" + processKey + '\'' +
                ", businessKey='" + businessKey + '\'' +
                ", initiator='" + initiator + '\'' +
                ", title='" + title + '\'' +
                ", variables=" + variables +
                '}';
    }
}
