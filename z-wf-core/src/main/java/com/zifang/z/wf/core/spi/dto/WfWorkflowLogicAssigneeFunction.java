package com.zifang.z.wf.core.spi.dto;

import java.io.Serializable;

/**
 * 流程逻辑审批人函数定义 — 蒸馏自 ace-platform-sdk {@code WorkflowLogicAssigneeFunction}
 * （{@code com.c2f.ace.sdk.dto}），字段语义完全对齐.
 *
 * <p>业务方实现 {@link com.zifang.z.wf.core.spi.WfWorkflowLogicAssigneeInjectService} 时，
 * 注册一个「逻辑审批人选取方式」到流程平台 — 流程引擎解析 BPMN 中的审批人占位符
 * （如 {@code ${myApprover}}）时，会调用对应的逻辑计算 SPI.
 *
 * @author zifang
 */
public class WfWorkflowLogicAssigneeFunction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 英文名 / 逻辑标识符（BPMN 中通过此名引用，如 {@code ${myApprover}}）.
     */
    private String name;

    /**
     * 逻辑描述（中文 — 低代码 UI 展示用）.
     */
    private String desc;

    /**
     * 逻辑详细说明（使用文档 / 调用示例）.
     */
    private String explain;

    public WfWorkflowLogicAssigneeFunction() {
    }

    public WfWorkflowLogicAssigneeFunction(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public WfWorkflowLogicAssigneeFunction(String name, String desc, String explain) {
        this.name = name;
        this.desc = desc;
        this.explain = explain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }
}
