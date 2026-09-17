package com.zifang.z.wf.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.WfExtensionContext;
import com.zifang.z.wf.core.spi.WfFormDataSubmitPostHandlerService;
import com.zifang.z.wf.core.spi.WfSpi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 端到端测试用 SPI — 表单发起后审计.
 *
 * @author zifang
 */
@Component
@WfSpi(name = "测试-SubmitPost 审计", code = "FormDataSubmitPostHandlerService", group = "表单", order = 999)
public class AuditSubmitPostSpi implements WfFormDataSubmitPostHandlerService {

    private static final Logger log = LogManager.getLogger(AuditSubmitPostSpi.class);

    @Autowired
    private AuditAggregator agg;

    @Override
    public Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data) {
        agg.submitPostCount.incrementAndGet();
        agg.callOrder.add("SubmitPost:" + context.getProcessInstanceId());
        log.debug("[Audit/SubmitPost] #{} dataKeys={}", agg.submitPostCount.get(), data.keySet());
        return Result.success(Boolean.TRUE);
    }
}