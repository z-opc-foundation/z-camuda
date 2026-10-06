package com.zifang.z.camuda.core.spi;

import com.zifang.util.core.meta.Result;


import java.util.List;
import java.util.Map;

/**
 * 审批后处理 — 蒸馏自 ace-platform-sdk {@code AgreePostService}
 * ({@code com.c2f.ace.sdk}，字段语义完全对齐.
 *
 * <p>SPI 元信息（来自 ace {@code @InterfaceMapping}）:
 * <ul>
 *   <li>中文名 — 审批后处理</li>
 *   <li>英文 code — AgreePostService</li>
 *   <li>分组 — 审批</li>
 * </ul>
 *
 * <p>业务方实现本接口，并标注 {@link CamudaSpi @CamudaSpi} 注解，
 * z-camuda 引擎会在对应生命周期点通过 {@link CamudaSpiRegistry} 调用所有实现。
 *
 * <p><b>失败语义以 {@code @return} 为准</b>：本引擎<b>不会</b>因 SPI 返回失败而中断流程。
 *
 * @author zifang
 */
public interface CamudaAgreePostService {

    /**
     * 审批后处理.
     *
     * @param context SPI 调用上下文（应用/模型/流程定义/实例 ID 等）
     * @return 处理结果 — {@link Result#isSuccess()} 为 {@code false} 时只记一条 warn
     *         日志，<b>流程照常推进</b>；返回 {@code null} 时静默跳过。引擎不会因此中断流程（本行原写“引擎中断流程”，与实现不符，已订正）
     */
    Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data);
}
