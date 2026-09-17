#!/usr/bin/env python3
"""
z-wf SPI 批量生成器 — 蒸馏 ace-platform-sdk 的 22 个 SPI 接口到 z-wf-core.

每个 SPI 的 Java 文件使用统一的 WfSpi 注解 + Result 返回 + WfExtensionContext 第一个参数。
"""
import os
from pathlib import Path

OUTPUT_DIR = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-wf/z-wf-core/src/main/java/com/zifang/z/wf/core/spi")

# SPI 定义列表 — 每项: (接口名, name, code, group, 方法签名)
SPIS = [
    # 表单数据系列 (14 个)
    ("WfFormDataInitService", "表单初始化", "FormDataInitService", "表单",
     "Result<Map<String, Object>> init(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataLifecycleService", "表单生命周期", "FormDataLifecycleService", "表单",
     "Result<Map<String, Object>> lifecycle(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataModifyPostHandlerService", "表单修改后处理", "FormDataModifyPostHandlerService", "表单",
     "Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataQueryPostHandlerService", "表单查询后处理", "FormDataQueryPostHandlerService", "表单",
     "Result<Map<String, Object>> postHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataQueryPreHandlerService", "表单查询前处理", "FormDataQueryPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataRemovePostHandlerService", "表单删除后处理", "FormDataRemovePostHandlerService", "表单",
     "Result<Boolean> postHandler(WfExtensionContext context, Long id);"),
    ("WfFormDataRemovePreValidateService", "表单删除前校验", "FormDataRemovePreValidateService", "表单",
     "Result<Boolean> validate(WfExtensionContext context, Long id);"),
    ("WfFormDataSubmitPostHandlerService", "表单发起后处理", "FormDataSubmitPostHandlerService", "表单",
     "Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataSubmitPreHandlerService", "表单发起前处理", "FormDataSubmitPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataTempPostHandlerService", "暂存后处理", "FormDataTempPostHandlerService", "表单",
     "Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataTempPreHandlerService", "暂存前处理", "FormDataTempPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataTempValidateService", "暂存校验", "FormDataTempValidateService", "表单",
     "Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data);"),
    ("WfFormDataValidateService", "表单校验服务", "FormDataValidateService", "表单",
     "Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data);"),

    # 审批系列 (4 个)
    ("WfAgreePostService", "审批后处理", "AgreePostService", "审批",
     "Result<Boolean> postHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfAgreePreService", "审批前处理", "AgreePreService", "审批",
     "Result<Map<String, Object>> preHandler(WfExtensionContext context, Map<String, Object> data);"),
    ("WfAgreeValidateService", "审批校验服务", "AgreeValidateService", "审批",
     "Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data);"),
    ("WfRejectValidateService", "拒绝校验服务", "RejectValidateService", "审批",
     "Result<Boolean> validate(WfExtensionContext context, Map<String, Object> data);"),

    # Apex 系列 (2 个)
    ("WfApexListStaffService", "Apex查询员工服务", "ApexListStaffService", "Apex",
     "Result<List<WfApexStaffDTO>> listStaff(WfExtensionContext context, WfStaffListRequest staffListRequest);"),
    ("WfApexListDeptService", "Apex查询部门服务", "ApexListDeptService", "Apex",
     "Result<List<WfApexDeptDTO>> listDept(WfExtensionContext context, WfDeptListRequest deptListRequest);"),

    # 流程系列 (3 个)
    ("WfWorkflowContextInjectService", "流程上下文坑位注册", "WorkflowContextKeyInjectService", "流程",
     "Result<List<WfWorkflowContextParam>> context(WfExtensionContext context);"),
    ("WfWorkflowLogicAssigneeCallService", "流程逻辑审批人执行", "WorkflowLogicAssigneeCallService", "流程",
     "Result<List<String>> calculate(String name, Map<String, Object> context);"),
    ("WfWorkflowLogicAssigneeInjectService", "流程逻辑审批人注册", "WorkflowLogicAssigneeInjectService", "流程",
     "Result<List<WfWorkflowLogicAssigneeFunction>> register(WfExtensionContext context);"),
]


TEMPLATE = """package com.zifang.z.wf.core.spi;

import com.zifang.util.core.meta.Result;
{dto_imports}

import java.util.List;
import java.util.Map;

/**
 * {name} — 蒸馏自 ace-platform-sdk {{@code {ace_name}}}
 * ({{@code com.c2f.ace.sdk}}，字段语义完全对齐.
 *
 * <p>SPI 元信息（来自 ace {{@code @InterfaceMapping}}）:
 * <ul>
 *   <li>中文名 — {name}</li>
 *   <li>英文 code — {code}</li>
 *   <li>分组 — {group}</li>
 * </ul>
 *
 * <p>业务方实现本接口，并标注 {{@link WfSpi @WfSpi}} 注解，
 * z-wf 引擎会在对应生命周期点通过 {{@link WfSpiRegistry}} 调用所有实现.
 *
 * @author zifang
 */
public interface {spi_name} {{

    /**
     * {name}.
     *
     * @param context SPI 调用上下文（应用/模型/流程定义/实例 ID 等）
     * @return 处理结果 — {{@link Result#isSuccess()}} 为 {{@code false}} 时引擎中断流程
     */
    {method}
}}
"""


def make_dto_imports(method_body: str) -> str:
    """根据方法签名生成需要的 dto import 列表."""
    imports = []
    if "WfApexStaffDTO" in method_body:
        imports.append("WfApexStaffDTO")
    if "WfApexDeptDTO" in method_body:
        imports.append("WfApexDeptDTO")
    if "WfWorkflowContextParam" in method_body:
        imports.append("WfWorkflowContextParam")
    if "WfWorkflowLogicAssigneeFunction" in method_body:
        imports.append("WfWorkflowLogicAssigneeFunction")
    if "WfStaffListRequest" in method_body:
        imports.append("WfStaffListRequest")
    if "WfDeptListRequest" in method_body:
        imports.append("WfDeptListRequest")
    return "\n".join(f"import com.zifang.z.wf.core.spi.dto.{x};" for x in imports)


for spi_name, name, code, group, method in SPIS:
    dto_imports = make_dto_imports(method)
    content = TEMPLATE.format(
        name=name,
        ace_name=code,
        code=code,
        group=group,
        spi_name=spi_name,
        method=method,
        dto_imports=dto_imports,
    )
    out_path = OUTPUT_DIR / f"{spi_name}.java"
    out_path.write_text(content, encoding="utf-8")
    print(f"created: {out_path.name}")

print(f"\nTotal: {len(SPIS)} SPI files")
