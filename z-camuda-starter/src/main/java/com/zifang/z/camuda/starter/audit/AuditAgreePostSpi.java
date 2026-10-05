package com.zifang.z.camuda.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.camuda.core.spi.CamudaAgreePostService;
import com.zifang.z.camuda.core.spi.CamudaExtensionContext;
import com.zifang.z.camuda.core.spi.CamudaSpi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 端到端测试用 SPI — 审批后审计.
 *
 * @author zifang
 */
@Component
@CamudaSpi(name = "测试-AgreePost 审计", code = "AgreePostService", group = "审批", order = 999)
public class AuditAgreePostSpi implements CamudaAgreePostService {

    private static final Logger log = LogManager.getLogger(AuditAgreePostSpi.class);

    @Autowired
    private AuditAggregator agg;

    @Override
    public Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data) {
        agg.agreePostCount.incrementAndGet();
        agg.callOrder.add("AgreePost:" + context.getProcessInstanceId());
        log.debug("[Audit/AgreePost] #{} approvalResult={}",
                agg.agreePostCount.get(), data.get("approvalResult"));
        return Result.success(Boolean.TRUE);
    }
}