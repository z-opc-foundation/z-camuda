package com.zifang.z.camuda.core.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * z-camuda 流程扩展上下文 — 蒸馏自 ace-platform-sdk {@code ExtensionServiceContext}
 * （{@code com.c2f.ace.sdk.dto}），字段语义完全对齐.
 *
 * <p>在 SPI 调用链中作为「业务上下文」参数传入 — 业务方实现 SPI 时可据此：
 * <ul>
 *   <li>判断当前所属应用/模型/流程定义，路由到对应的业务逻辑分支</li>
 *   <li>把上下文透传到下游 RPC（z-rpc）调用，保留链路追踪</li>
 *   <li>读取流程实例 ID，做历史/审计查询</li>
 * </ul>
 *
 * @author zifang
 */
public class CamudaExtensionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用编码（来自 {@code appCode} 字段）.
     */
    private String appCode;

    /**
     * 模型编码.
     */
    private String modelCode;

    /**
     * 流程定义 Key（如 {@code "leaveProcess"}）.
     */
    private String workflowDefinitionKey;

    /**
     * 表单编码.
     */
    private String formCode;

    /**
     * 页面编码.
     */
    private String pageCode;

    /**
     * 表单附带信息（已弃用，保留兼容）.
     */
    @Deprecated
    private String bizTag;

    /**
     * 表单附带信息 — 推荐使用字段.
     */
    private List<String> customTags;

    /**
     * 流程实例 ID（流程启动后填充）.
     */
    private String processInstanceId;

    public CamudaExtensionContext() {
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    @Deprecated
    public String getBizTag() {
        return bizTag;
    }

    @Deprecated
    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    public List<String> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(List<String> customTags) {
        this.customTags = customTags;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * 链式 setter — 业务方构造 context 时可读性更高.
     */
    public CamudaExtensionContext withAppCode(String appCode) {
        this.appCode = appCode;
        return this;
    }

    public CamudaExtensionContext withModelCode(String modelCode) {
        this.modelCode = modelCode;
        return this;
    }

    public CamudaExtensionContext withWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
        return this;
    }

    public CamudaExtensionContext withFormCode(String formCode) {
        this.formCode = formCode;
        return this;
    }

    public CamudaExtensionContext withProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public CamudaExtensionContext addCustomTag(String tag) {
        if (this.customTags == null) {
            this.customTags = new ArrayList<>();
        }
        this.customTags.add(tag);
        return this;
    }
}
