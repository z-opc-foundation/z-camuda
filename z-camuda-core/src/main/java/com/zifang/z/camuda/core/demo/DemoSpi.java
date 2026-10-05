package com.zifang.z.camuda.core.demo;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.spi.CamudaAgreePostService;
import com.zifang.z.camuda.core.spi.CamudaAgreePreService;
import com.zifang.z.camuda.core.spi.CamudaExtensionContext;
import com.zifang.z.camuda.core.spi.CamudaFormDataSubmitPostHandlerService;
import com.zifang.z.camuda.core.spi.CamudaFormDataSubmitPreHandlerService;
import com.zifang.z.camuda.core.spi.CamudaSpi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * z-camuda 端到端测试用 SPI 示例 — 演示业务方如何在流程生命周期注入逻辑.
 *
 * <p>蒸馏自 ace-platform-core
 * {@code C2fOpsFormQueryPreServiceImpl} + {@code C2fOpsFormDataPreServiceImpl} +
 * {@code AgreePreServiceImpl} + {@code AgreePostServiceImpl} 的简化版，
 * 仅做日志埋点（业务流程各阶段打印上下文）用于演示 SPI 链路打通.
 *
 * <p>z-camuda 启动时 {@link com.zifang.z.camuda.core.spi.CamudaSpiRegistry} 会扫描本实现，
 * 通过 {@code @CamudaSpi} 注解的 {@code code} 注册到对应分组，按 {@code order} 顺序执行.
 *
 * @author zifang
 */
public class DemoSpi {

    /**
     * 表单发起前 — 业务方预处理（可在此补充默认值 / 校验 / 转换）.
     */
    @Component
    @CamudaSpi(name = "示例-表单发起前处理", code = "FormDataSubmitPreHandlerService", group = "表单", order = 10)
    public static class DemoSubmitPreHandler implements CamudaFormDataSubmitPreHandlerService {
        private static final Logger log = LogManager.getLogger(DemoSubmitPreHandler.class);

        @Override
        public Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data) {
            log.info("[SPI=SubmitPre] appCode={} modelCode={} processKey={} dataKeys={}",
                    context.getAppCode(), context.getModelCode(),
                    context.getWorkflowDefinitionKey(), data.keySet());
            return Result.success(data);
        }
    }

    /**
     * 表单发起后 — 业务方后处理（审计 / 通知 / 触发其他流程）.
     */
    @Component
    @CamudaSpi(name = "示例-表单发起后处理", code = "FormDataSubmitPostHandlerService", group = "表单", order = 10)
    public static class DemoSubmitPostHandler implements CamudaFormDataSubmitPostHandlerService {
        private static final Logger log = LogManager.getLogger(DemoSubmitPostHandler.class);

        @Override
        public Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data) {
            log.info("[SPI=SubmitPost] processInstanceId={} dataKeys={}",
                    context.getProcessInstanceId(), data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /**
     * 审批前 — 业务方预处理（可在此做风控校验 / 风险提示）.
     */
    @Component
    @CamudaSpi(name = "示例-审批前处理", code = "AgreePreService", group = "审批", order = 10)
    public static class DemoAgreePre implements CamudaAgreePreService {
        private static final Logger log = LogManager.getLogger(DemoAgreePre.class);

        @Override
        public Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data) {
            log.info("[SPI=AgreePre] processInstanceId={} approver={} dataKeys={}",
                    context.getProcessInstanceId(), data.get("approver"), data.keySet());
            return Result.success(data);
        }
    }

    /**
     * 审批后 — 业务方后处理（同步到业务系统 / 通知发起人）.
     */
    @Component
    @CamudaSpi(name = "示例-审批后处理", code = "AgreePostService", group = "审批", order = 10)
    public static class DemoAgreePost implements CamudaAgreePostService {
        private static final Logger log = LogManager.getLogger(DemoAgreePost.class);

        @Override
        public Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data) {
            log.info("[SPI=AgreePost] processInstanceId={} approvalResult={}",
                    context.getProcessInstanceId(), data.get("approvalResult"));
            return Result.success(Boolean.TRUE);
        }
    }
}
