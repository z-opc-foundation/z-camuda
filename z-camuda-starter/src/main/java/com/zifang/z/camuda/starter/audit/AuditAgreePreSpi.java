package com.zifang.z.camuda.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.spi.CamudaAgreePreService;
import com.zifang.z.camuda.core.spi.CamudaExtensionContext;
import com.zifang.z.camuda.core.spi.CamudaSpi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 端到端测试用 SPI — 审批前审计.
 *
 * @author zifang
 */
@Component
@CamudaSpi(name = "测试-AgreePre 审计", code = "AgreePreService", group = "审批", order = 999)
public class AuditAgreePreSpi implements CamudaAgreePreService {

    private static final Logger log = LogManager.getLogger(AuditAgreePreSpi.class);

    @Autowired
    private AuditAggregator agg;

    @Override
    public Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data) {
        agg.agreePreCount.incrementAndGet();
        agg.callOrder.add("AgreePre:" + context.getProcessInstanceId());
        log.debug("[Audit/AgreePre] #{} dataKeys={}", agg.agreePreCount.get(), data.keySet());
        return Result.success(data);
    }
}