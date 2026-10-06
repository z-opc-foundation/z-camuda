package com.zifang.z.camuda.core.spi;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.spi.dto.CamudaApexDeptDTO;
import com.zifang.z.camuda.core.spi.dto.CamudaDeptListRequest;

import java.util.List;
import java.util.Map;

/**
 * Apex查询部门服务 — 蒸馏自 ace-platform-sdk {@code ApexListDeptService}
 * ({@code com.c2f.ace.sdk}，字段语义完全对齐.
 *
 * <p>SPI 元信息（来自 ace {@code @InterfaceMapping}）:
 * <ul>
 *   <li>中文名 — Apex查询部门服务</li>
 *   <li>英文 code — ApexListDeptService</li>
 *   <li>分组 — Apex</li>
 * </ul>
 *
 * <p>业务方实现本接口并标注 {@link CamudaSpi @CamudaSpi} 注解后，Bean 会被
 * {@link CamudaSpiRegistry} 扫描并按 {@code code} 注册。
 *
 * <p><b>⚠ 当前版本没有派发点会调用本接口。</b>引擎只在 4 个 code 上分发 SPI
 * （{@code FormDataSubmitPreHandlerService} / {@code FormDataSubmitPostHandlerService} /
 * {@code AgreePreService} / {@code AgreePostService}），本接口属<b>预留扩展点</b>：
 * 实现它不会报错、也一定会被扫描进注册表，但它的方法<b>永远不会被执行</b>。
 *
 * <p><b>注册成功 ≠ 被调用</b>：{@code spiRegistry.getByCode(code)} 返回非空只说明
 * 扫描到了 Bean，不说明有派发点会触发它。
 *
 * @author zifang
 */
public interface CamudaApexListDeptService {

    /**
     * Apex查询部门服务.
     *
     * @param context SPI 调用上下文（应用/模型/流程定义/实例 ID 等）
     * @return 处理结果 — <b>当前没有派发点会消费这个返回值</b>（见类级说明），
     *         实现里的返回值语义不会对流程产生任何影响
     */
    Result<List<CamudaApexDeptDTO>> listDept(CamudaExtensionContext context, CamudaDeptListRequest deptListRequest);
}
