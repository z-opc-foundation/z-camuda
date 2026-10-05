package com.zifang.z.camuda.core.hook;

import java.util.Map;

/**
 * 工作流通知钩子.
 *
 * <p>z-camuda-core 定义接口, 调用方通过此钩子注入通知逻辑 (邮件/短信/钉钉等).
 * 支持多种实现方式 (HTTP / RPC / 本地).
 *
 * <p>使用方式:
 * <pre>{@code
 * // 通知钩子
 * @Bean
 * public CamudaNotificationHook wfNotificationHook() {
 *     return new CamudaNotificationHook() {
 *         @Override
 *         public void notifyTaskAssigned(String taskId, String assignee, String processKey) {
 *             // 发送钉钉通知
 *         }
 *     };
 * }
 * }</pre>
 */
public interface CamudaNotificationHook {

    /**
     * 任务分配通知.
     *
     * @param taskId     任务 ID
     * @param assignee   被分配人
     * @param processKey 流程定义 Key
     * @param variables  流程变量
     */
    default void notifyTaskAssigned(String taskId, String assignee, String processKey, Map<String, Object> variables) {
        // 默认空实现
    }

    /**
     * 审批结果通知.
     *
     * @param processId  流程实例 ID
     * @param processKey 流程定义 Key
     * @param applicant  申请人
     * @param outcome    审批结果 ("approved" / "rejected")
     * @param comment    审批意见
     */
    default void notifyApprovalResult(String processId, String processKey, String applicant, String outcome, String comment) {
        // 默认空实现
    }

    /**
     * 流程超时通知.
     *
     * @param taskId     任务 ID
     * @param assignee   被分配人
     * @param processKey 流程定义 Key
     * @param overdueMinutes 超时分钟数
     */
    default void notifyOverdue(String taskId, String assignee, String processKey, long overdueMinutes) {
        // 默认空实现
    }

    /**
     * 钩子类型标识.
     */
    default String hookType() {
        return "wf-notification";
    }
}
