package com.zifang.z.camuda.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-camuda-starter 自动配置.
 * <p>
 * FEATURE012: 启用 ApprovalCenterController 等 controller.
 * Camunda 引擎需在 application.properties 启用 + 配 camundaBpmDataSource / camundaBpmTransactionManager.
 *
 * @author zifang
 * @since 1.0.0
 */
@Configuration
@ComponentScan(
        basePackages = {
                "com.zifang.z.camuda.web.api",
                "com.zifang.z.camuda.core",
                "com.zifang.z.camuda.starter.rpc",
                "com.zifang.z.camuda.starter"
                // 注意：原本这里显式扫 com.zifang.z.rpc.starter.config 已被移除 —
                // z-rpc 通过 spring.factories / 自动装配机制加载，
                // 显式扫会导致 z.rpc.enabled=false 时 ZRpcServerAutoConfiguration 被独立加载，
                // 找不到 ZRpcProperties 报错.
        }
)
public class CamudaStarterAutoConfiguration {
}
