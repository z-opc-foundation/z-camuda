package com.zifang.z.camuda.starter.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SPI 调用计数聚合器 — 由 4 个 {@code Audit*Post/Spi} 共享调用计数状态.
 *
 * @author zifang
 */
@Component
public class AuditAggregator {

    public final AtomicInteger submitPreCount = new AtomicInteger(0);
    public final AtomicInteger submitPostCount = new AtomicInteger(0);
    public final AtomicInteger agreePreCount = new AtomicInteger(0);
    public final AtomicInteger agreePostCount = new AtomicInteger(0);
    public final List<String> callOrder = new ArrayList<>();

    public void reset() {
        submitPreCount.set(0);
        submitPostCount.set(0);
        agreePreCount.set(0);
        agreePostCount.set(0);
        callOrder.clear();
    }
}