package com.zifang.z.wf.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-wf-starter 自动配置.
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
                "com.zifang.z.wf.web.api",
                "com.zifang.z.wf.core",
                "com.zifang.z.wf.starter.rpc",
                "com.zifang.z.wf.starter",
                // 显式扫 z-rpc 包, 确保 ZRpcServiceExporter (BeanPostProcessor) 能被 Spring 容器创建
                // (它的 @Component 注解需要 ComponentScan 触发才能生效)
                "com.zifang.z.rpc.starter.config"
        }
)
public class WfStarterAutoConfiguration {
}
