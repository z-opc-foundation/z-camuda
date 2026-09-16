package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审批中心仪表盘统计数据
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "仪表盘统计数据")
public class DashboardStatsVO {

    @Schema(description = "待我审批数量")
    private Long todoCount; // 待审批任务数量

    @Schema(description = "我已审批数量")
    private Long doneCount; // 已审批任务数量

    @Schema(description = "我发起的流程数量")
    private Long myProcessCount; // 我发起的流程数量

    @Schema(description = "抄送我的数量")
    private Long ccCount; // 抄送数量

    public Long getTodoCount() {
        return todoCount;
    }

    public void setTodoCount(Long todoCount) {
        this.todoCount = todoCount;
    }

    public Long getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(Long doneCount) {
        this.doneCount = doneCount;
    }

    public Long getMyProcessCount() {
        return myProcessCount;
    }

    public void setMyProcessCount(Long myProcessCount) {
        this.myProcessCount = myProcessCount;
    }

    public Long getCcCount() {
        return ccCount;
    }

    public void setCcCount(Long ccCount) {
        this.ccCount = ccCount;
    }
}
