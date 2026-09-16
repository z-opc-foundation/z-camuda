package com.zifang.z.wf.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-wf 独立启动类 (开发/测试用).
 *
 * <p>生产环境请将 z-wf-starter 作为依赖引入, 无需启动此类.
 */
@SpringBootApplication(scanBasePackages = "com.zifang.z.wf")
public class ZWfApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZWfApplication.class, args);
    }
}
