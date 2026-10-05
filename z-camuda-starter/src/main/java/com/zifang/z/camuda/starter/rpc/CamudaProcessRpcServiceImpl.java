package com.zifang.z.camuda.starter.rpc;

import com.zifang.z.rpc.annotation.ZRpcService;
import com.zifang.z.camuda.core.service.LeaveProcessService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * z-camuda 流程服务 RPC 暴露实现.
 *
 * <p>当业务方接入 z-camuda-starter + z-rpc-spring-boot-starter 时,
 * 这个类会被自动扫描并通过 {@link ZRpcService} 暴露为 RPC 服务.</p>
 *
 * <p>外部调用方:</p>
 * <pre>{@code
 * @ZRpcReference
 * private CamudaProcessRpcService wfRpc;
 *
 * String processId = wfRpc.startLeaveProcess("alice", "bob",
 *     Map.of("days", 3, "reason", "sick"));
 * List<Map<String, Object>> todos = wfRpc.getTodoTasks("bob");
 * }</pre>
 *
 * @author zifang
 * @since 1.0.0
 */
@Service
@ConditionalOnClass(name = "com.zifang.z.rpc.annotation.ZRpcService")
@ConditionalOnProperty(prefix = "z.rpc", name = "enabled", matchIfMissing = true)
@ZRpcService(interfaceClass = CamudaProcessRpcService.class, version = "1.0.0")
public class CamudaProcessRpcServiceImpl implements CamudaProcessRpcService {

    private static final Logger log = LogManager.getLogger(CamudaProcessRpcServiceImpl.class);

    @Autowired
    private LeaveProcessService leaveProcessService;

    @Override
    public String startLeaveProcess(String applicant, String approver, Map<String, Object> variables) {
        log.info("[z-camuda RPC] startLeaveProcess applicant={} approver={} vars={}", applicant, approver, variables);
        return leaveProcessService.startLeaveProcess(applicant, approver, variables);
    }

    @Override
    public List<Map<String, Object>> getTodoTasks(String approver) {
        log.info("[z-camuda RPC] getTodoTasks approver={}", approver);
        List<Task> tasks = leaveProcessService.getTodoTasksByApprover(approver);
        return tasks.stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("assignee", t.getAssignee());
            map.put("createTime", t.getCreateTime());
            map.put("processInstanceId", t.getProcessInstanceId());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        log.info("[z-camuda RPC] completeTask taskId={} vars={}", taskId, variables);
        leaveProcessService.completeApprovalTask(taskId, variables);
    }
}
