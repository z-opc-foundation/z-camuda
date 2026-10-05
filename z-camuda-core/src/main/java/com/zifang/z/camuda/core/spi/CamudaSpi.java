package com.zifang.z.camuda.core.spi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * z-camuda 流程 SPI 扩展点元信息注解 — 蒸馏自 ace-platform-sdk
 * {@code @InterfaceMapping} （{@code com.c2f.ace.sdk.annoation}），字段语义完全对齐.
 *
 * <p>每个 SPI 接口实现类都应使用本注解标明：
 * <ul>
 *   <li>{@link #name()} — 中文名（用于低代码平台 UI 展示）</li>
 *   <li>{@link #code()} — 英文标识（用于 SPI 注册表查找）</li>
 *   <li>{@link #group()} — 分组（表单/流程/审批/Apex 等）</li>
 *   <li>{@link #order()} — 同分组内执行顺序（数字越小越靠前，默认 0）</li>
 * </ul>
 *
 * <p>示例：
 * <pre>{@code
 * @CamudaSpi(name = "表单发起前处理", code = "FormDataSubmitPreHandlerService", group = "表单", order = 1)
 * public class MyPreHandler implements CamudaFormDataSubmitPreHandlerService { ... }
 * }</pre>
 *
 * @author zifang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CamudaSpi {

    /**
     * SPI 中文名（用于低代码平台 UI 展示）.
     */
    String name() default "";

    /**
     * SPI 英文标识（用于 SPI 注册表查找，与 ace 的 {@code code} 同义）.
     */
    String code() default "";

    /**
     * SPI 分组：{@code 表单} / {@code 流程} / {@code 审批} / {@code Apex}.
     */
    String group() default "";

    /**
     * 同分组内执行顺序 — 数字越小越靠前（默认 0）.
     * <p>业务方可以通过在 SPI 注册表（{@link CamudaSpiRegistry}）中按 {@code order} 排序，
     * 让多个 SPI 按预期顺序串行执行（如：校验 → 转换 → 通知）.
     */
    int order() default 0;
}
