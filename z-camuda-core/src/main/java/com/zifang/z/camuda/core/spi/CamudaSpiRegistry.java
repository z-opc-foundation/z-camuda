package com.zifang.z.camuda.core.spi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * z-camuda 流程 SPI 注册表 — 蒸馏自 ace-platform-core
 * {@code AcePlatformExtensionCollector} （{@code com.c2f.ace.core.extenssion.bdp.platform}}，
 * 行为完全对齐.
 *
 * <p>启动时扫描 Spring 容器中所有实现 SPI 接口的 Bean，读取其 {@link CamudaSpi} 注解元信息，
 * 按 {@link CamudaSpi#code()} 分组，按 {@link CamudaSpi#order()} 排序，注册到内存表.
 *
 * <p>调用方通过 {@link #getByCode(String)} 获取某一类 SPI 的全部实现
 * （有序 — 调用时按 {@code order} 升序串行执行）.
 *
 * <p>线程安全：所有 map 使用 {@link ConcurrentHashMap}.
 *
 * @author zifang
 */
@Component
public class CamudaSpiRegistry implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LogManager.getLogger(CamudaSpiRegistry.class);

    /** key = {@link CamudaSpi#code()}, value = 已按 order 升序排序的 SPI 实现列表. */
    private final Map<String, List<Object>> registry = new ConcurrentHashMap<>();

    /** key = SPI 全限定类名, value = 实现类引用（用于反查） */
    private final Map<String, Object> beanByClass = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        // 扫描全部 SPI 接口（通过 SPI_BASE_PACKAGE 包下所有 interface）
        scanSpiPackage("com.zifang.z.camuda.core.spi");
    }

    /**
     * 扫描指定包路径下的所有接口类型，找出 Spring 容器中实现了这些接口的 Bean.
     */
    private void scanSpiPackage(String packagePrefix) {
        if (applicationContext == null) {
            log.warn("CamudaSpiRegistry: applicationContext is null, skip scanning");
            return;
        }
        // 找到本注册表自己的包内所有 SPI 接口（除了 CamudaSpi/CamudaExtensionContext 等非 SPI 接口）
        List<Class<?>> spiClasses = discoverSpiInterfaces();
        log.info("CamudaSpiRegistry: discovered {} SPI interfaces in {}", spiClasses.size(), packagePrefix);

        // 对每个 SPI 接口，从 Spring 容器取所有实现
        for (Class<?> spiClass : spiClasses) {
            Map<String, ?> beans = applicationContext.getBeansOfType(spiClass);
            for (Object bean : beans.values()) {
                CamudaSpi annotation = AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), CamudaSpi.class);
                if (annotation == null) {
                    log.debug("skip bean {} — no @CamudaSpi annotation", bean.getClass().getName());
                    continue;
                }
                register(annotation.code(), bean, annotation.order());
                beanByClass.put(bean.getClass().getName(), bean);
            }
        }
        log.info("CamudaSpiRegistry: scanned {} SPI implementations across {} codes",
                beanByClass.size(), registry.size());
    }

    /**
     * 枚举本包内所有 SPI 接口（识别规则：以 {@code Wf} 开头且以 {@code Service} 结尾的 interface）.
     * <p>硬编码枚举避免引入 {@code Reflections} 等运行时扫描依赖.
     */
    private List<Class<?>> discoverSpiInterfaces() {
        List<Class<?>> list = new ArrayList<>();
        // 表单系列
        list.add(CamudaFormDataInitService.class);
        list.add(CamudaFormDataLifecycleService.class);
        list.add(CamudaFormDataModifyPostHandlerService.class);
        list.add(CamudaFormDataQueryPostHandlerService.class);
        list.add(CamudaFormDataQueryPreHandlerService.class);
        list.add(CamudaFormDataRemovePostHandlerService.class);
        list.add(CamudaFormDataRemovePreValidateService.class);
        list.add(CamudaFormDataSubmitPostHandlerService.class);
        list.add(CamudaFormDataSubmitPreHandlerService.class);
        list.add(CamudaFormDataTempPostHandlerService.class);
        list.add(CamudaFormDataTempPreHandlerService.class);
        list.add(CamudaFormDataTempValidateService.class);
        list.add(CamudaFormDataValidateService.class);
        // 审批系列
        list.add(CamudaAgreePostService.class);
        list.add(CamudaAgreePreService.class);
        list.add(CamudaAgreeValidateService.class);
        list.add(CamudaRejectValidateService.class);
        // Apex 系列
        list.add(CamudaApexListStaffService.class);
        list.add(CamudaApexListDeptService.class);
        // 流程系列
        list.add(CamudaWorkflowContextInjectService.class);
        list.add(CamudaWorkflowLogicAssigneeCallService.class);
        list.add(CamudaWorkflowLogicAssigneeInjectService.class);
        return list;
    }

    /**
     * 注册一个 SPI 实现到指定 code 下.
     */
    public void register(String code, Object bean, int order) {
        List<Object> list = registry.computeIfAbsent(code, k -> new ArrayList<>());
        list.add(bean);
        // 排序（按 order 升序）
        list.sort(Comparator.comparingInt(o -> {
            CamudaSpi a = AnnotatedElementUtils.findMergedAnnotation(o.getClass(), CamudaSpi.class);
            return a == null ? 0 : a.order();
        }));
    }

    /**
     * 获取指定 code 下的全部 SPI 实现（按 {@link CamudaSpi#order()} 升序）.
     *
     * @return 不可变快照列表（无注册时返回空列表）
     */
    public List<Object> getByCode(String code) {
        List<Object> list = registry.get(code);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 获取注册表中所有 code（用于调试 / 监控）.
     */
    public java.util.Set<String> allCodes() {
        return new java.util.TreeSet<>(registry.keySet());
    }

    /**
     * 统计每个 code 下注册数量.
     */
    public Map<String, Integer> stats() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, List<Object>> e : registry.entrySet()) {
            result.put(e.getKey(), e.getValue().size());
        }
        return result;
    }

    /**
     * 清空注册表 — 主要用于测试.
     */
    public void clear() {
        registry.clear();
        beanByClass.clear();
    }
}
