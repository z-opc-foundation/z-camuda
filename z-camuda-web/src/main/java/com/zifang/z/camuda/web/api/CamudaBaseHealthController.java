package com.zifang.z.camuda.web.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * z-camuda 健康检查 Controller.
 * <p>
 * API 基础路径: /api/wf
 * 所属模块: z-camuda-web
 * 鉴权: 无 (运维探活使用)
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /api/wf/health — 返回固定字符串 {@code "UP"}, 探活用</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/wf")
@Tag(name = "10000_监控端点")
public class CamudaBaseHealthController {

    /**
     * 健康检查端点, 返回固定字符串 {@code "UP"}.
     *
     * @return 状态字符串 "UP"
     */
    @GetMapping("/health")
    @Operation(summary = "10000_健康检查")
    public String health() {
        return "UP";
    }
}
