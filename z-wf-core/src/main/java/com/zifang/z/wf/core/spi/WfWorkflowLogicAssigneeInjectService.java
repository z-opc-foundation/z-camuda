package com.zifang.z.wf.core.spi;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.dto.WfWorkflowLogicAssigneeFunction;

import java.util.List;
import java.util.Map;

/**
 * 流程逻辑审批人注册 — 蒸馏自 ace-platform-sdk {@code WorkflowLogicAssigneeInjectService}
 * ({@code com.c2f.ace.sdk}，字段语义完全对齐.
 *
 * <p>SPI 元信息（来自 ace {@code @InterfaceMapping}）:
 * <ul>
 *   <li>中文名 — 流程逻辑审批人注册</li>
 *   <li>英文 code — WorkflowLogicAssigneeInjectService</li>
 *   <li>分组 — 流程</li>
 * </ul>
 *
 * <p>业务方实现本接口，并标注 {@link WfSpi @WfSpi} 注解，
 * z-wf 引擎会在对应生命周期点通过 {@link WfSpiRegistry} 调用所有实现.
 *
 * @author zifang
 */
public interface WfWorkflowLogicAssigneeInjectService {

    /**
     * 流程逻辑审批人注册.
     *
     * @param context SPI 调用上下文（应用/模型/流程定义/实例 ID 等）
     * @return 处理结果 — {@link Result#isSuccess()} 为 {@code false} 时引擎中断流程
     */
    Result<List<WfWorkflowLogicAssigneeFunction>> register(WfExtensionContext context);
}
