package com.zifang.z.wf.starter.host;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Camunda BPM 集成配置.
 * <p>
 * FEATURE012: 提供 camundaBpmDataSource + camundaBpmTransactionManager,
 * 复用 z.base.db.default 配置 (与主应用同库, 表前缀 act_).
 * Camunda spring-boot-starter 自动注入这两个 bean.
 * 2026-10-02: 连接串/凭据改由 z.base.db.default.* 属性注入(值经环境变量供给,
 * 明文只住 lead 仓 004_重要秘钥), 原硬编码字面量按"全仓零明文"清除.
 */
@Configuration
public class CamundaConfig {

    @Bean(name = "camundaBpmDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.camunda")
    public DataSource camundaBpmDataSource(Environment env) {
        String host = env.getProperty("z.base.db.default.host", "");
        String port = env.getProperty("z.base.db.default.port", "3306");
        String database = env.getProperty("z.base.db.default.database", "oc");
        return DataSourceBuilder.create()
                .type(DruidDataSource.class)
                .url("jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8")
                .username(env.getProperty("z.base.db.default.username", "zifang"))
                .password(env.getProperty("z.base.db.default.password", ""))
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean(name = "camundaBpmTransactionManager")
    public PlatformTransactionManager camundaBpmTransactionManager(
            @Qualifier("camundaBpmDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
