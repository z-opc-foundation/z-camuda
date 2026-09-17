package com.zifang.z.wf.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.WfExtensionContext;
import com.zifang.z.wf.core.spi.WfFormDataSubmitPreHandlerService;
import com.zifang.z.wf.core.spi.WfSpi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 端到端测试用 SPI — 表单发起前审计.
 *
 * <p>实现 {@link WfFormDataSubmitPreHandlerService} + 标注 {@link WfSpi}
 * 注册到 {@code FormDataSubmitPreHandlerService} 分组, 每次流程启动都会触发.
 *
 * @author zifang
 */
@Component
@WfSpi(name = "测试-SubmitPre 审计", code = "FormDataSubmitPreHandlerService", group = "表单", order = 999)
public class AuditSubmitPreSpi implements WfFormDataSubmitPreHandlerService {

    private static final Logger log = LogManager.getLogger(AuditSubmitPreSpi.class);

    @Autowired
    private AuditAggregator agg;

    @Override
    public Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data) {
        agg.submitPreCount.incrementAndGet();
        agg.callOrder.add("SubmitPre:" + context.getProcessInstanceId());
        log.debug("[Audit/SubmitPre] #{} dataKeys={}", agg.submitPreCount.get(), data.keySet());
        return Result.success(data);
    }
}