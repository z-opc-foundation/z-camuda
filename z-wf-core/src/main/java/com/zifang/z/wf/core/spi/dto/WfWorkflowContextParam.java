package com.zifang.z.wf.core.spi.dto;

import java.io.Serializable;

/**
 * 流程上下文参数 — 蒸馏自 ace-platform-sdk {@code WorkflowContextParam}
 * （{@code com.c2f.ace.sdk.dto}），字段语义完全对齐.
 *
 * <p>业务方实现 {@link com.zifang.z.wf.core.spi.WfWorkflowContextInjectService} 时，
 * 注册到流程平台的「上下文坑位」 — 流程表单/审批人解析时可读取这些变量.
 *
 * @author zifang
 */
public class WfWorkflowContextParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数名称. */
    private String name;

    /** 参数默认值. */
    private String value;

    /** 参数类型（如 {@code String} / {@code Long} / {@code Boolean}） */
    private String type;

    /** 参数含义描述. */
    private String desc;

    /** 是否可以被取消. */
    private Boolean cancelable;

    public WfWorkflowContextParam() {
    }

    public WfWorkflowContextParam(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public WfWorkflowContextParam(String name, String value, String type, String desc, Boolean cancelable) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.desc = desc;
        this.cancelable = cancelable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Boolean getCancelable() {
        return cancelable;
    }

    public void setCancelable(Boolean cancelable) {
        this.cancelable = cancelable;
    }
}
