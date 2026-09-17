package com.zifang.z.wf.core.spi.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 部门列表查询请求 — 蒸馏自 ace-platform-sdk {@code DeptListRequest}
 * （{@code com.c2f.ace.sdk.request}），字段语义完全对齐.
 *
 * @author zifang
 */
public class WfDeptListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String keyword;
    private String parentId;
    private List<String> parentIds;
    private String leaderId;
    private Boolean active;
    private Integer size;
    private Integer page;

    public WfDeptListRequest() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}
