package com.zifang.z.wf.starter;

import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.rpc.starter.properties.ZRpcProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * z-wf 服务端生命周期.
 *
 * <p>当 z-wf-starter 启动 + classpath 含 z-config-spring-boot-starter 时,
 * Spring 上下文启动完成后, 自动把 z-wf 实例注册到 z-config 服务注册中心.</p>
 *
 * <p>注册内容:</p>
 * <ul>
 *   <li>serviceName = {@code ${spring.application.name:z-wf}}</li>
 *   <li>ip = 本机 IP</li>
 *   <li>port = z-rpc 端口 (默认 20880, 来自 z-rpc-spring-boot-starter)</li>
 * </ul>
 *
 * <p>外部服务通过 {@code ZNamingService.selectOneHealthyInstance("z-wf")}
 * 拿到这个实例, 然后通过 {@code @ZRpcReference(WfProcessService.class)} 远程调用.</p>
 *
 * @author zifang
 * @since 1.0.0
 */
@Component
@ConditionalOnClass(name = "com.zifang.z.rpc.starter.properties.ZRpcProperties")
public class WfServerLifecycle {

    private static final Logger log = LogManager.getLogger(WfServerLifecycle.class);

    @Autowired(required = false)
    private ZNamingService znaming;

    @Autowired(required = false)
    private ZRpcProperties zRpcProperties;

    @Value("${spring.application.name:z-wf}")
    private String serviceName;

    @Value("${server.port:8080}")
    private int httpPort;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (znaming == null) {
            log.warn("[z-wf] ZNamingService bean 未找到, 跳过服务注册. 请在 classpath 加 z-config-spring-boot-starter.");
            return;
        }

        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            int rpcPort = (zRpcProperties != null && zRpcProperties.getServer() != null)
                    ? zRpcProperties.getServer().getPort()
                    : 20880;

            // 优先注册 RPC 端口 (外部通过 z-rpc 调 z-wf 用), HTTP 端口写到 cluster 名作为元信息
            znaming.registerInstance(serviceName, ip, rpcPort, "DEFAULT");
            log.info("[z-wf] ✓ 服务注册成功: service={} ip={} rpcPort={} httpPort={}",
                    serviceName, ip, rpcPort, httpPort);
        } catch (Exception e) {
            // 不让 z-config 不可用阻塞整个 z-wf 启动
            log.warn("[z-wf] ✗ 服务注册失败 (z-config 注册中心不可达? 仍可对外提供 HTTP API + Netty RPC): {}",
                    e.getMessage());
            log.debug("[z-wf] 注册失败详情", e);
        }
    }
}
