package com.zifang.z.wf.starter.host;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 宿主侧胶水装配 (2026-10-03 自 z-opc main-starter 平移):
 * CamundaConfig (camundaBpmDataSource + camundaBpmTransactionManager).
 *
 * <p>跟随 z.wf.host.enabled 开关 (默认关): 寄生 all-in-one 模式由宿主打开,
 * standalone 模式不开 (Camunda 跑在独立容器里).
 */
@Configuration
@ConditionalOnProperty(prefix = "z.wf.host", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.zifang.z.wf.starter.host")
public class WfHostAutoConfiguration {
}