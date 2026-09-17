package com.zifang.z.wf.core.spi.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 员工列表查询请求 — 蒸馏自 ace-platform-sdk {@code StaffListRequest}
 * （{@code com.c2f.ace.sdk.request}），字段语义完全对齐.
 *
 * @author zifang
 */
public class WfStaffListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关键字（姓名/工号/邮箱模糊匹配） */
    private String keyword;

    /** 部门 ID（精确） */
    private String deptId;

    /** 部门 ID 列表（批量查询 — 取 deptIds + deptId 并集） */
    private List<String> deptIds;

    /** 角色（精确） */
    private String role;

    /** 角色列表（任一命中） */
    private List<String> roles;

    /** 是否在职（true 过滤离职） */
    private Boolean active;

    /** 分页大小 */
    private Integer size;

    /** 分页页码（从 1 开始） */
    private Integer page;

    public WfStaffListRequest() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public List<String> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<String> deptIds) {
        this.deptIds = deptIds;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
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
