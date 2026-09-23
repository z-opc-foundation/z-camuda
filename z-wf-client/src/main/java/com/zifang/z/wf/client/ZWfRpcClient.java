package com.zifang.z.wf.client;

import com.zifang.z.rpc.remoting.RpcClient;
import com.zifang.z.rpc.remoting.RpcRequest;
import com.zifang.z.rpc.remoting.RpcResponse;
import com.zifang.z.wf.starter.rpc.WfProcessRpcService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * z-wf 外部客户端 demo.
 *
 * <p>通过 z-rpc Netty 直连 z-wf-server 的 20880 端口, 远程调用流程服务.</p>
 *
 * <p>本客户端模拟"外部应用"角色, 它可以是:</p>
 * <ul>
 *   <li>一个独立的 Java 程序 (本例)</li>
 *   <li>另一个 Spring Boot 应用通过 {@code @ZRpcReference} 注入 (推荐)</li>
 *   <li>任何能调 Netty + Hessian2 的客户端</li>
 * </ul>
 *
 * <p>实际使用方式 (在另一个 Spring Boot 应用里):</p>
 * <pre>{@code
 * @RestController
 * public class LeaveWorkflowController {
 *     @ZRpcReference(version = "1.0.0", registry = "127.0.0.1:8848")
 *     private WfProcessRpcService wfRpc;
 *
 *     @PostMapping("/leave/start")
 *     public String start(@RequestBody LeaveRequest req) {
 *         return wfRpc.startLeaveProcess(req.applicant, req.approver,
 *             Map.of("days", req.days, "reason", req.reason));
 *     }
 * }
 * }</pre>
 *
 * @author zifang
 * @since 1.0.0
 */
public class ZWfRpcClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法: ZWfRpcClient <rpc-host> <rpc-port> start [applicant] [approver] [days] [reason]");
            System.err.println("示例: ZWfRpcClient 127.0.0.1 20880 start alice bob 3 sick-leave");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String action = args.length > 2 ? args[2] : "start";

        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("  z-wf 外部客户端 — 通过 z-rpc Netty 调用 z-wf-server");
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("  RPC target : " + host + ":" + port);
        System.out.println("  Action     : " + action);
        System.out.println();

        RpcClient client = new RpcClient(host, port);
        try {
            client.setTimeout(10_000);

            if ("start".equals(action)) {
                String applicant = args.length > 3 ? args[3] : "alice";
                String approver = args.length > 4 ? args[4] : "bob";
                int days = args.length > 5 ? Integer.parseInt(args[5]) : 3;
                String reason = args.length > 6 ? args[6] : "test-from-zwf-client";

                System.out.println("→ 调用 WfProcessRpcService.startLeaveProcess(" +
                        applicant + ", " + approver + ", days=" + days + ", reason=\"" + reason + "\")");

                // 构造流程变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("days", days);
                variables.put("reason", reason);

                // 构造 RpcRequest
                RpcRequest request = new RpcRequest();
                request.setRequestId(UUID.randomUUID().toString());
                request.setInterfaceName(WfProcessRpcService.class.getName());
                request.setMethodName("startLeaveProcess");
                request.setParameterTypes(new Class<?>[]{
                        String.class, String.class, Map.class
                });
                request.setArguments(new Object[]{applicant, approver, variables});
                Map<String, String> attachments = new HashMap<>();
                attachments.put("version", "1.0.0");
                request.setAttachments(attachments);

                RpcResponse response = client.sendRequest(request);

                System.out.println();
                System.out.println("✓ RPC 调用成功!");
                System.out.println("  processInstanceId = " + response.getResult());
                System.out.println();
                System.out.println("说明:");
                System.out.println("  - 流程已通过 z-rpc 在 z-wf-server 启动");
                System.out.println("  - 现在可以通过 REST API 查询待办: GET /api/approval-center/tasks/get?taskId=");
                System.out.println("  - 或在管理前端查看: http://localhost:18080/ (z-wf-admin 自带管理前端)");
            } else {
                System.err.println("未知 action: " + action);
            }
        } finally {
            client.close();
        }
    }
}
