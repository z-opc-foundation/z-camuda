package com.zifang.z.camuda.core.spi.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Apex 员工 DTO — 蒸馏自 ace-platform-sdk {@code ApexStaffDTO}
 * （{@code com.c2f.ace.sdk.dto}），字段语义完全对齐.
 *
 * <p>业务方实现 {@link com.zifang.z.camuda.core.spi.CamudaApexListStaffService} 时，
 * 返回的人员列表元素.
 *
 * @author zifang
 */
public class CamudaApexStaffDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 员工 ID. */
    private String id;

    /** 员工姓名. */
    private String name;

    /** 员工工号. */
    private String code;

    /** 员工邮箱. */
    private String email;

    /** 员工手机号. */
    private String phone;

    /** 所属部门 ID. */
    private String deptId;

    /** 所属部门名称. */
    private String deptName;

    /** 直属领导 ID. */
    private String leaderId;

    /** 直属领导姓名. */
    private String leaderName;

    /** 角色列表. */
    private List<String> roles;

    /** 职位. */
    private String position;

    public CamudaApexStaffDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public CamudaApexStaffDTO addRole(String role) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        this.roles.add(role);
        return this;
    }
}
