package com.zifang.z.wf.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-wf 独立启动类.
 *
 * <p>嵌入式使用 (如 z-opc) 请依赖 z-wf-starter 自动装配, 无需启动此类.</p>
 *
 * @author zifang
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.zifang.z.wf")
public class ZWfAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZWfAdminApplication.class, args);
    }
}
