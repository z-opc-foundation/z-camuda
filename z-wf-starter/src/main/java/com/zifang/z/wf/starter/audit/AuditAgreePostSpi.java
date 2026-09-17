package com.zifang.z.wf.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.WfAgreePostService;
import com.zifang.z.wf.core.spi.WfExtensionContext;
import com.zifang.z.wf.core.spi.WfSpi;
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
@WfSpi(name = "测试-AgreePost 审计", code = "AgreePostService", group = "审批", order = 999)
public class AuditAgreePostSpi implements WfAgreePostService {

    private static final Logger log = LogManager.getLogger(AuditAgreePostSpi.class);

    @Autowired
    private AuditAggregator agg;

    @Override
    public Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data) {
        agg.agreePostCount.incrementAndGet();
        agg.callOrder.add("AgreePost:" + context.getProcessInstanceId());
        log.debug("[Audit/AgreePost] #{} approvalResult={}",
                agg.agreePostCount.get(), data.get("approvalResult"));
        return Result.success(Boolean.TRUE);
    }
}