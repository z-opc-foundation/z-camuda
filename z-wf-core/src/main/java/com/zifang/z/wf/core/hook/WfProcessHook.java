package com.zifang.z.wf.core.hook;

import java.util.Map;

/**
 * 工作流流程钩子.
 *
 * <p>z-wf-core 定义接口, 调用方通过此钩子注入流程生命周期逻辑.
 * 支持多种实现方式:
 * <ul>
 *   <li>本地模式: 直接调用 z-wf-core 的 service</li>
 *   <li>HTTP 模式: 调用 z-wf-web 的 REST API</li>
 *   <li>RPC 模式: 通过 z-rpc 调用 z-wf 服务</li>
 * </ul>
 *
 * <p>使用方式:
 * <pre>{@code
 * // 流程启动前钩子
 * @Bean
 * public WfProcessHook wfProcessHook() {
 *     return new HttpWfProcessHook("http://z-wf:8080");
 * }
 * }</pre>
 */
public interface WfProcessHook {

    /**
     * 流程启动前回调.
     *
     * @param processKey 流程定义 Key (如 "leaveProcess")
     * @param variables  流程变量 (可修改)
     * @return true 继续启动, false 取消启动
     */
    default boolean onBeforeStart(String processKey, Map<String, Object> variables) {
        return true;
    }

    /**
     * 流程启动后回调.
     *
     * @param processKey  流程定义 Key
     * @param processId   流程实例 ID
     * @param variables   流程变量
     */
    default void onAfterStart(String processKey, String processId, Map<String, Object> variables) {
        // 默认空实现
    }

    /**
     * 流程完成回调.
     *
     * @param processKey  流程定义 Key
     * @param processId   流程实例 ID
     * @param outcome     流程结果 (如 "approved", "rejected")
     */
    default void onComplete(String processKey, String processId, String outcome) {
        // 默认空实现
    }

    /**
     * 钩子类型标识.
     */
    default String hookType() {
        return "wf-process";
    }
}
