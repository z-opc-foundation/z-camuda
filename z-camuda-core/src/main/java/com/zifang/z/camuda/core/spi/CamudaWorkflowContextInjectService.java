package com.zifang.z.camuda.core.spi;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.spi.dto.CamudaWorkflowContextParam;

import java.util.List;
import java.util.Map;

/**
 * 流程上下文坑位注册 — 蒸馏自 ace-platform-sdk {@code WorkflowContextKeyInjectService}
 * ({@code com.c2f.ace.sdk}，字段语义完全对齐.
 *
 * <p>SPI 元信息（来自 ace {@code @InterfaceMapping}）:
 * <ul>
 *   <li>中文名 — 流程上下文坑位注册</li>
 *   <li>英文 code — WorkflowContextKeyInjectService</li>
 *   <li>分组 — 流程</li>
 * </ul>
 *
 * <p>业务方实现本接口，并标注 {@link CamudaSpi @CamudaSpi} 注解，
 * z-camuda 引擎会在对应生命周期点通过 {@link CamudaSpiRegistry} 调用所有实现.
 *
 * @author zifang
 */
public interface CamudaWorkflowContextInjectService {

    /**
     * 流程上下文坑位注册.
     *
     * @param context SPI 调用上下文（应用/模型/流程定义/实例 ID 等）
     * @return 处理结果 — {@link Result#isSuccess()} 为 {@code false} 时引擎中断流程
     */
    Result<List<CamudaWorkflowContextParam>> context(CamudaExtensionContext context);
}
