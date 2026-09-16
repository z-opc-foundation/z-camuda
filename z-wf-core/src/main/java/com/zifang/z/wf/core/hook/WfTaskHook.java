package com.zifang.z.wf.core.hook;

import java.util.Map;

/**
 * 工作流任务钩子.
 *
 * <p>z-wf-core 定义接口, 调用方通过此钩子注入任务生命周期逻辑.
 * 支持多种实现方式 (HTTP / RPC / 本地).
 *
 * <p>使用方式:
 * <pre>{@code
 * // 任务分配钩子
 * @Bean
 * public WfTaskHook wfTaskHook() {
 *     return new HttpWfTaskHook("http://z-wf:8080");
 * }
 * }</pre>
 */
public interface WfTaskHook {

    /**
     * 任务创建前回调.
     *
     * @param taskId    任务 ID
     * @param assignee  被分配人
     * @param variables 任务变量 (可修改)
     * @return true 继续创建, false 取消创建
     */
    default boolean onBeforeCreate(String taskId, String assignee, Map<String, Object> variables) {
        return true;
    }

    /**
     * 任务分配变更回调.
     *
     * @param taskId      任务 ID
     * @param fromAssignee 原分配人
     * @param toAssignee   新分配人
     */
    default void onAssigneeChanged(String taskId, String fromAssignee, String toAssignee) {
        // 默认空实现
    }

    /**
     * 任务完成前回调.
     *
     * @param taskId    任务 ID
     * @param assignee  执行人
     * @param variables 任务变量
     * @return true 继续完成, false 取消完成
     */
    default boolean onBeforeComplete(String taskId, String assignee, Map<String, Object> variables) {
        return true;
    }

    /**
     * 任务完成回调.
     *
     * @param taskId   任务 ID
     * @param assignee 执行人
     * @param outcome  任务结果
     */
    default void onAfterComplete(String taskId, String assignee, String outcome) {
        // 默认空实现
    }

    /**
     * 钩子类型标识.
     */
    default String hookType() {
        return "wf-task";
    }
}
