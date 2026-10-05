#!/usr/bin/env python3
"""
z-camuda SPI 批量生成器 — 蒸馏 ace-platform-sdk 的 22 个 SPI 接口到 z-camuda-core.

每个 SPI 的 Java 文件使用统一的 CamudaSpi 注解 + Result 返回 + CamudaExtensionContext 第一个参数。
"""
import os
from pathlib import Path

OUTPUT_DIR = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-camuda/z-camuda-core/src/main/java/com/zifang/z/camuda/core/spi")

# SPI 定义列表 — 每项: (接口名, name, code, group, 方法签名)
SPIS = [
    # 表单数据系列 (14 个)
    ("CamudaFormDataInitService", "表单初始化", "FormDataInitService", "表单",
     "Result<Map<String, Object>> init(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataLifecycleService", "表单生命周期", "FormDataLifecycleService", "表单",
     "Result<Map<String, Object>> lifecycle(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataModifyPostHandlerService", "表单修改后处理", "FormDataModifyPostHandlerService", "表单",
     "Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataQueryPostHandlerService", "表单查询后处理", "FormDataQueryPostHandlerService", "表单",
     "Result<Map<String, Object>> postHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataQueryPreHandlerService", "表单查询前处理", "FormDataQueryPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataRemovePostHandlerService", "表单删除后处理", "FormDataRemovePostHandlerService", "表单",
     "Result<Boolean> postHandler(CamudaExtensionContext context, Long id);"),
    ("CamudaFormDataRemovePreValidateService", "表单删除前校验", "FormDataRemovePreValidateService", "表单",
     "Result<Boolean> validate(CamudaExtensionContext context, Long id);"),
    ("CamudaFormDataSubmitPostHandlerService", "表单发起后处理", "FormDataSubmitPostHandlerService", "表单",
     "Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataSubmitPreHandlerService", "表单发起前处理", "FormDataSubmitPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataTempPostHandlerService", "暂存后处理", "FormDataTempPostHandlerService", "表单",
     "Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataTempPreHandlerService", "暂存前处理", "FormDataTempPreHandlerService", "表单",
     "Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataTempValidateService", "暂存校验", "FormDataTempValidateService", "表单",
     "Result<Boolean> validate(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaFormDataValidateService", "表单校验服务", "FormDataValidateService", "表单",
     "Result<Boolean> validate(CamudaExtensionContext context, Map<String, Object> data);"),

    # 审批系列 (4 个)
    ("CamudaAgreePostService", "审批后处理", "AgreePostService", "审批",
     "Result<Boolean> postHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaAgreePreService", "审批前处理", "AgreePreService", "审批",
     "Result<Map<String, Object>> preHandler(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaAgreeValidateService", "审批校验服务", "AgreeValidateService", "审批",
     "Result<Boolean> validate(CamudaExtensionContext context, Map<String, Object> data);"),
    ("CamudaRejectValidateService", "拒绝校验服务", "RejectValidateService", "审批",
     "Result<Boolean> validate(CamudaExtensionContext context, Map<String, Object> data);"),

    # Apex 系列 (2 个)
    ("CamudaApexListStaffService", "Apex查询员工服务", "ApexListStaffService", "Apex",
     "Result<List<CamudaApexStaffDTO>> listStaff(CamudaExtensionContext context, CamudaStaffListRequest staffListRequest);"),
    ("CamudaApexListDeptService", "Apex查询部门服务", "ApexListDeptService", "Apex",
     "Result<List<CamudaApexDeptDTO>> listDept(CamudaExtensionContext context, CamudaDeptListRequest deptListRequest);"),

    # 流程系列 (3 个)
    ("CamudaWorkflowContextInjectService", "流程上下文坑位注册", "WorkflowContextKeyInjectService", "流程",
     "Result<List<CamudaWorkflowContextParam>> context(CamudaExtensionContext context);"),
    ("CamudaWorkflowLogicAssigneeCallService", "流程逻辑审批人执行", "WorkflowLogicAssigneeCallService", "流程",
     "Result<List<String>> calculate(String name, Map<String, Object> context);"),
    ("CamudaWorkflowLogicAssigneeInjectService", "流程逻辑审批人注册", "WorkflowLogicAssigneeInjectService", "流程",
     "Result<List<CamudaWorkflowLogicAssigneeFunction>> register(CamudaExtensionContext context);"),
]


TEMPLATE = """package com.zifang.z.camuda.core.spi;

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
 * <p>业务方实现本接口，并标注 {{@link CamudaSpi @CamudaSpi}} 注解，
 * z-camuda 引擎会在对应生命周期点通过 {{@link CamudaSpiRegistry}} 调用所有实现.
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
    if "CamudaApexStaffDTO" in method_body:
        imports.append("CamudaApexStaffDTO")
    if "CamudaApexDeptDTO" in method_body:
        imports.append("CamudaApexDeptDTO")
    if "CamudaWorkflowContextParam" in method_body:
        imports.append("CamudaWorkflowContextParam")
    if "CamudaWorkflowLogicAssigneeFunction" in method_body:
        imports.append("CamudaWorkflowLogicAssigneeFunction")
    if "CamudaStaffListRequest" in method_body:
        imports.append("CamudaStaffListRequest")
    if "CamudaDeptListRequest" in method_body:
        imports.append("CamudaDeptListRequest")
    return "\n".join(f"import com.zifang.z.camuda.core.spi.dto.{x};" for x in imports)


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
