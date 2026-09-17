package com.zifang.z.wf.starter.audit;

import com.zifang.util.core.meta.Result;
import com.zifang.z.wf.core.spi.*;
import com.zifang.z.wf.core.spi.dto.WfApexDeptDTO;
import com.zifang.z.wf.core.spi.dto.WfApexStaffDTO;
import com.zifang.z.wf.core.spi.dto.WfDeptListRequest;
import com.zifang.z.wf.core.spi.dto.WfStaffListRequest;
import com.zifang.z.wf.core.spi.dto.WfWorkflowContextParam;
import com.zifang.z.wf.core.spi.dto.WfWorkflowLogicAssigneeFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * z-wf 全能力对齐 ace BPMN callable — 综合 SPI 覆盖测试夹具.
 *
 * <p>本夹具一次性实现 z-wf 全部 22 个 SPI 接口 + 共享 {@link ComprehensiveAudit}
 * 计数聚合器，让端到端测试可以一次性验证 ace 平台"流程 / 表单 / 审批 / Apex / 上下文注入"
 * 五大类 SPI 全链路.
 *
 * <p>蒸馏自 ace-platform-core 的 BPMN callable 集群
 * （{@code GenericHttpServiceCallListener / InnerTaskListener / GenericMsgNotifyListener} +
 * 业务 SPI 实现），在 z-wf 抽象为统一可插拔的 SPI 接口 + 共享计数的实现.
 *
 * @author zifang
 */
public class ComprehensiveSpiBundle {

    /**
     * 综合审计计数器 — 跟踪每个 SPI 触发的次数和顺序.
     */
    @Component
    public static class ComprehensiveAudit {
        public final AtomicInteger initCount = new AtomicInteger(0);
        public final AtomicInteger lifecycleCount = new AtomicInteger(0);
        public final AtomicInteger validateCount = new AtomicInteger(0);
        public final AtomicInteger submitPreCount = new AtomicInteger(0);
        public final AtomicInteger submitPostCount = new AtomicInteger(0);
        public final AtomicInteger queryPreCount = new AtomicInteger(0);
        public final AtomicInteger queryPostCount = new AtomicInteger(0);
        public final AtomicInteger modifyPostCount = new AtomicInteger(0);
        public final AtomicInteger removePostCount = new AtomicInteger(0);
        public final AtomicInteger removePreValidateCount = new AtomicInteger(0);
        public final AtomicInteger tempPreCount = new AtomicInteger(0);
        public final AtomicInteger tempPostCount = new AtomicInteger(0);
        public final AtomicInteger tempValidateCount = new AtomicInteger(0);
        public final AtomicInteger agreePreCount = new AtomicInteger(0);
        public final AtomicInteger agreePostCount = new AtomicInteger(0);
        public final AtomicInteger agreeValidateCount = new AtomicInteger(0);
        public final AtomicInteger rejectValidateCount = new AtomicInteger(0);
        public final AtomicInteger contextInjectCount = new AtomicInteger(0);
        public final AtomicInteger assigneeInjectCount = new AtomicInteger(0);
        public final AtomicInteger assigneeCallCount = new AtomicInteger(0);
        public final AtomicInteger staffListCount = new AtomicInteger(0);
        public final AtomicInteger deptListCount = new AtomicInteger(0);

        public final List<String> callOrder = new java.util.concurrent.CopyOnWriteArrayList<>();

        public void reset() {
            initCount.set(0);
            lifecycleCount.set(0);
            validateCount.set(0);
            submitPreCount.set(0);
            submitPostCount.set(0);
            queryPreCount.set(0);
            queryPostCount.set(0);
            modifyPostCount.set(0);
            removePostCount.set(0);
            removePreValidateCount.set(0);
            tempPreCount.set(0);
            tempPostCount.set(0);
            tempValidateCount.set(0);
            agreePreCount.set(0);
            agreePostCount.set(0);
            agreeValidateCount.set(0);
            rejectValidateCount.set(0);
            contextInjectCount.set(0);
            assigneeInjectCount.set(0);
            assigneeCallCount.set(0);
            staffListCount.set(0);
            deptListCount.set(0);
            callOrder.clear();
        }

        public int totalCount() {
            return initCount.get() + lifecycleCount.get() + validateCount.get()
                    + submitPreCount.get() + submitPostCount.get()
                    + queryPreCount.get() + queryPostCount.get()
                    + modifyPostCount.get() + removePostCount.get() + removePreValidateCount.get()
                    + tempPreCount.get() + tempPostCount.get() + tempValidateCount.get()
                    + agreePreCount.get() + agreePostCount.get() + agreeValidateCount.get()
                    + rejectValidateCount.get()
                    + contextInjectCount.get() + assigneeInjectCount.get() + assigneeCallCount.get()
                    + staffListCount.get() + deptListCount.get();
        }
    }

    /** 表单初始化. */
    @Component
    @WfSpi(name = "综合-表单初始化", code = "FormDataInitService", group = "表单", order = 50)
    public static class ComprehensiveInit implements WfFormDataInitService {
        private static final Logger log = LogManager.getLogger(ComprehensiveInit.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveInit(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> init(WfExtensionContext context, Map<String, Object> data) {
            audit.initCount.incrementAndGet();
            audit.callOrder.add("Init");
            log.info("[SPI=Init] appCode={} modelCode={}", context.getAppCode(), context.getModelCode());
            // 在表单初始化阶段填充默认字段（对齐 ace InitService 的"默认值注入"语义）
            Map<String, Object> enriched = new HashMap<>(data == null ? new HashMap<>() : data);
            enriched.putIfAbsent("defaultScorer", "zifang");
            enriched.putIfAbsent("defaultApprover", "ceo");
            return Result.success(enriched);
        }
    }

    /** 表单生命周期. */
    @Component
    @WfSpi(name = "综合-表单生命周期", code = "FormDataLifecycleService", group = "表单", order = 50)
    public static class ComprehensiveLifecycle implements WfFormDataLifecycleService {
        private static final Logger log = LogManager.getLogger(ComprehensiveLifecycle.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveLifecycle(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> lifecycle(WfExtensionContext context, Map<String, Object> data) {
            audit.lifecycleCount.incrementAndGet();
            audit.callOrder.add("Lifecycle");
            log.info("[SPI=Lifecycle] processInstanceId={}",
                    context.getProcessInstanceId());
            return Result.success(data);
        }
    }

    /** 表单校验. */
    @Component
    @WfSpi(name = "综合-表单校验", code = "FormDataValidateService", group = "表单", order = 50)
    public static class ComprehensiveValidate implements WfFormDataValidateService {
        private static final Logger log = LogManager.getLogger(ComprehensiveValidate.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveValidate(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data) {
            audit.validateCount.incrementAndGet();
            audit.callOrder.add("Validate");
            log.info("[SPI=Validate] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 审批前 — 与 AuditAgreePreSpi 协同. */
    @Component
    @WfSpi(name = "综合-审批前", code = "AgreePreService", group = "审批", order = 50)
    public static class ComprehensiveAgreePre implements WfAgreePreService {
        private static final Logger log = LogManager.getLogger(ComprehensiveAgreePre.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveAgreePre(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.agreePreCount.incrementAndGet();
            audit.callOrder.add("AgreePre");
            log.info("[SPI=AgreePre] approver={} dataKeys={}", data.get("approver"), data.keySet());
            return Result.success(data);
        }
    }

    /** 审批后. */
    @Component
    @WfSpi(name = "综合-审批后", code = "AgreePostService", group = "审批", order = 50)
    public static class ComprehensiveAgreePost implements WfAgreePostService {
        private static final Logger log = LogManager.getLogger(ComprehensiveAgreePost.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveAgreePost(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.agreePostCount.incrementAndGet();
            audit.callOrder.add("AgreePost");
            log.info("[SPI=AgreePost] processInstanceId={} approvalResult={}",
                    context.getProcessInstanceId(), data.get("approvalResult"));
            return Result.success(Boolean.TRUE);
        }
    }

    /** 审批通过校验. */
    @Component
    @WfSpi(name = "综合-审批通过校验", code = "AgreeValidateService", group = "审批", order = 50)
    public static class ComprehensiveAgreeValidate implements WfAgreeValidateService {
        private static final Logger log = LogManager.getLogger(ComprehensiveAgreeValidate.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveAgreeValidate(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data) {
            audit.agreeValidateCount.incrementAndGet();
            audit.callOrder.add("AgreeValidate");
            log.info("[SPI=AgreeValidate] processInstanceId={}", context.getProcessInstanceId());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 驳回校验 — 对齐 ace 驳回前的规则校验语义. */
    @Component
    @WfSpi(name = "综合-驳回校验", code = "RejectValidateService", group = "审批", order = 50)
    public static class ComprehensiveRejectValidate implements WfRejectValidateService {
        private static final Logger log = LogManager.getLogger(ComprehensiveRejectValidate.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveRejectValidate(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data) {
            audit.rejectValidateCount.incrementAndGet();
            audit.callOrder.add("RejectValidate");
            log.info("[SPI=RejectValidate] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 表单查询前 — 对齐 ace C2fOpsFormQueryPreServiceImpl. */
    @Component
    @WfSpi(name = "综合-表单查询前", code = "FormDataQueryPreHandlerService", group = "表单", order = 50)
    public static class ComprehensiveQueryPre implements WfFormDataQueryPreHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveQueryPre.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveQueryPre(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.queryPreCount.incrementAndGet();
            audit.callOrder.add("QueryPre");
            log.info("[SPI=QueryPre] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(data);
        }
    }

    /** 表单查询后. */
    @Component
    @WfSpi(name = "综合-表单查询后", code = "FormDataQueryPostHandlerService", group = "表单", order = 50)
    public static class ComprehensiveQueryPost implements WfFormDataQueryPostHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveQueryPost.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveQueryPost(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> postHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.queryPostCount.incrementAndGet();
            audit.callOrder.add("QueryPost");
            log.info("[SPI=QueryPost] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(data);
        }
    }

    /** 表单修改后. */
    @Component
    @WfSpi(name = "综合-表单修改后", code = "FormDataModifyPostHandlerService", group = "表单", order = 50)
    public static class ComprehensiveModifyPost implements WfFormDataModifyPostHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveModifyPost.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveModifyPost(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.modifyPostCount.incrementAndGet();
            audit.callOrder.add("ModifyPost");
            log.info("[SPI=ModifyPost] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 表单删除前校验. */
    @Component
    @WfSpi(name = "综合-表单删除前校验", code = "FormDataRemovePreValidateService", group = "表单", order = 50)
    public static class ComprehensiveRemovePreValidate implements WfFormDataRemovePreValidateService {
        private static final Logger log = LogManager.getLogger(ComprehensiveRemovePreValidate.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveRemovePreValidate(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> validate(WfExtensionContext context, Long id) {
            audit.removePreValidateCount.incrementAndGet();
            audit.callOrder.add("RemovePreValidate");
            log.info("[SPI=RemovePreValidate] id={}", id);
            return Result.success(Boolean.TRUE);
        }
    }

    /** 表单删除后. */
    @Component
    @WfSpi(name = "综合-表单删除后", code = "FormDataRemovePostHandlerService", group = "表单", order = 50)
    public static class ComprehensiveRemovePost implements WfFormDataRemovePostHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveRemovePost.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveRemovePost(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> postHandler(WfExtensionContext context, Long id) {
            audit.removePostCount.incrementAndGet();
            audit.callOrder.add("RemovePost");
            log.info("[SPI=RemovePost] id={}", id);
            return Result.success(Boolean.TRUE);
        }
    }

    /** 表单暂存前. */
    @Component
    @WfSpi(name = "综合-表单暂存前", code = "FormDataTempPreHandlerService", group = "表单", order = 50)
    public static class ComprehensiveTempPre implements WfFormDataTempPreHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveTempPre.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveTempPre(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.tempPreCount.incrementAndGet();
            audit.callOrder.add("TempPre");
            log.info("[SPI=TempPre] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(data);
        }
    }

    /** 表单暂存后. */
    @Component
    @WfSpi(name = "综合-表单暂存后", code = "FormDataTempPostHandlerService", group = "表单", order = 50)
    public static class ComprehensiveTempPost implements WfFormDataTempPostHandlerService {
        private static final Logger log = LogManager.getLogger(ComprehensiveTempPost.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveTempPost(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data) {
            audit.tempPostCount.incrementAndGet();
            audit.callOrder.add("TempPost");
            log.info("[SPI=TempPost] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 表单暂存校验. */
    @Component
    @WfSpi(name = "综合-表单暂存校验", code = "FormDataTempValidateService", group = "表单", order = 50)
    public static class ComprehensiveTempValidate implements WfFormDataTempValidateService {
        private static final Logger log = LogManager.getLogger(ComprehensiveTempValidate.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveTempValidate(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data) {
            audit.tempValidateCount.incrementAndGet();
            audit.callOrder.add("TempValidate");
            log.info("[SPI=TempValidate] dataKeys={}", data == null ? "null" : data.keySet());
            return Result.success(Boolean.TRUE);
        }
    }

    /** 流程上下文注入 — 对齐 ace WorkflowCommonInfoCollector 注入系统参数. */
    @Component
    @WfSpi(name = "综合-流程上下文注入", code = "WorkflowContextInjectService", group = "流程", order = 50)
    public static class ComprehensiveContextInject implements WfWorkflowContextInjectService {
        private static final Logger log = LogManager.getLogger(ComprehensiveContextInject.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveContextInject(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<List<WfWorkflowContextParam>> context(WfExtensionContext context) {
            audit.contextInjectCount.incrementAndGet();
            audit.callOrder.add("ContextInject");
            log.info("[SPI=ContextInject] processKey={}", context.getWorkflowDefinitionKey());
            List<WfWorkflowContextParam> params = Arrays.asList(
                    new WfWorkflowContextParam("$APP_CODE", context.getAppCode(), "String", "应用编码", Boolean.FALSE),
                    new WfWorkflowContextParam("$MODEL_CODE", context.getModelCode(), "String", "模型编码", Boolean.FALSE),
                    new WfWorkflowContextParam("$WORKFLOW_DEFINITION_KEY", context.getWorkflowDefinitionKey(), "String", "流程定义 Key", Boolean.FALSE)
            );
            return Result.success(params);
        }
    }

    /** 流程逻辑审批人注入. */
    @Component
    @WfSpi(name = "综合-流程逻辑审批人注入", code = "WorkflowLogicAssigneeInjectService", group = "流程", order = 50)
    public static class ComprehensiveAssigneeInject implements WfWorkflowLogicAssigneeInjectService {
        private static final Logger log = LogManager.getLogger(ComprehensiveAssigneeInject.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveAssigneeInject(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<List<WfWorkflowLogicAssigneeFunction>> register(WfExtensionContext context) {
            audit.assigneeInjectCount.incrementAndGet();
            audit.callOrder.add("AssigneeInject");
            log.info("[SPI=AssigneeInject] processKey={}",
                    context.getWorkflowDefinitionKey());
            WfWorkflowLogicAssigneeFunction fn = new WfWorkflowLogicAssigneeFunction();
            fn.setName("dynamicScorer");
            fn.setDesc("动态评分人逻辑");
            fn.setExplain("根据业务规则自动选取评分人");
            return Result.success(Arrays.asList(fn));
        }
    }

    /** 流程逻辑审批人调用. */
    @Component
    @WfSpi(name = "综合-流程逻辑审批人调用", code = "WorkflowLogicAssigneeCallService", group = "流程", order = 50)
    public static class ComprehensiveAssigneeCall implements WfWorkflowLogicAssigneeCallService {
        private static final Logger log = LogManager.getLogger(ComprehensiveAssigneeCall.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveAssigneeCall(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<List<String>> calculate(String name, Map<String, Object> context) {
            audit.assigneeCallCount.incrementAndGet();
            audit.callOrder.add("AssigneeCall");
            log.info("[SPI=AssigneeCall] name={} contextKeys={}", name,
                    context == null ? "null" : context.keySet());
            return Result.success(Arrays.asList("dynamic-approver-from-spi"));
        }
    }

    /** Apex 员工列表. */
    @Component
    @WfSpi(name = "综合-Apex 员工列表", code = "ApexListStaffService", group = "Apex", order = 50)
    public static class ComprehensiveStaffList implements WfApexListStaffService {
        private static final Logger log = LogManager.getLogger(ComprehensiveStaffList.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveStaffList(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<List<WfApexStaffDTO>> listStaff(WfExtensionContext context, WfStaffListRequest staffListRequest) {
            audit.staffListCount.incrementAndGet();
            audit.callOrder.add("StaffList");
            log.info("[SPI=StaffList] keyword={}", staffListRequest == null ? "null" : staffListRequest.getKeyword());
            WfApexStaffDTO dto = new WfApexStaffDTO();
            dto.setId("U001");
            dto.setName("alice");
            dto.setEmail("alice@example.com");
            return Result.success(Arrays.asList(dto));
        }
    }

    /** Apex 部门列表. */
    @Component
    @WfSpi(name = "综合-Apex 部门列表", code = "ApexListDeptService", group = "Apex", order = 50)
    public static class ComprehensiveDeptList implements WfApexListDeptService {
        private static final Logger log = LogManager.getLogger(ComprehensiveDeptList.class);
        private final ComprehensiveAudit audit;

        public ComprehensiveDeptList(ComprehensiveAudit audit) {
            this.audit = audit;
        }

        @Override
        public Result<List<WfApexDeptDTO>> listDept(WfExtensionContext context, WfDeptListRequest deptListRequest) {
            audit.deptListCount.incrementAndGet();
            audit.callOrder.add("DeptList");
            log.info("[SPI=DeptList] keyword={}", deptListRequest == null ? "null" : deptListRequest.getKeyword());
            WfApexDeptDTO dto = new WfApexDeptDTO();
            dto.setId("D001");
            dto.setName("技术部");
            return Result.success(Arrays.asList(dto));
        }
    }
}