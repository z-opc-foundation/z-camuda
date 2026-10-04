package com.zifang.z.wf.starter.host;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 *
 * <p><b>2026-10-04 补上 @ConditionalOnProperty —— 在此之前这两个 bean 是无条件注册的,
 * 而它读的三个属性全是静默空默认值</b>：
 * <pre>
 *   host     默认 ""     ⇒ URL 拼成 jdbc:mysql://:3306/oc   (空主机!)
 *   database 默认 "oc"
 *   username 默认 "zifang" / password 默认 ""
 * </pre>
 * 由此有两个后果，都实测到了：
 * <ol>
 *   <li><b>z-wf-admin 的 h2-test profile 彻底失效</b>。那份 profile 把
 *       {@code spring.datasource.*} 指向 H2、{@code camunda.bpm.database.type=h2}，
 *       而本类完全不看 spring.datasource.*，自己拼一条 MySQL 串；加上它还标了
 *       {@code @Primary}，连主数据源都被它压过。两个端到端用例
 *       （FiveLookEndToEndTest / LeaveProcessEndToEndTest）就永远对着
 *       {@code jdbc:mysql://:3306/oc} 反复重连 —— 实测一份构建日志里 461 次
 *       create connection SQLException，测试根本跑不完。</li>
 *   <li><b>部署漏配 {@code z.base.db.default.host} 时，报错完全指不到真因</b>：
 *       拿到的是 MySQL 的 {@code CommunicationsException}（"Communications link failure"），
 *       而真实原因是"这台部署根本没配 DB 坐标"。空默认值把配置缺失伪装成了网络故障。</li>
 * </ol>
 *
 * <p>⇒ 只有在运维**真的**配了 {@code z.base.db.default.host} 时才由本类接管数据源；
 * 没配就整个不注册，camunda-bpm-spring-boot-starter 于是回落到容器的主数据源
 * （也就是 {@code spring.datasource.*} 说的那个，h2-test 走 H2 就是靠这条回落生效）。
 * 配了的行为与改前逐字相同，不配的则从"连空主机"变成"用应用自己的数据源"。
 */
@Configuration
@ConditionalOnProperty(name = "z.base.db.default.host")
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
