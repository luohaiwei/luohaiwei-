package com.lab.reservation.entity;

import lombok.Data;

/**
 * 数据权限上下文
 * 用于在请求级别保存当前用户的数据权限信息
 */
@Data
public class DataScopeContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型：SYSTEM_ADMIN, LAB_ADMIN, TEACHER, STUDENT, MAINTAINER
     */
    private String userType;

    /**
     * 数据权限范围类型：ALL, DEPT, SELF, CUSTOM
     */
    private String scopeType;

    /**
     * 可管理的实验室ID列表（逗号分隔）
     */
    private String customLabIds;

    /**
     * 设备数据权限：VIEW,EDIT,DELETE,STATUS
     */
    private String deviceScope;

    /**
     * 预约数据权限：VIEW,AUDIT,CANCEL,EXPORT
     */
    private String bookingScope;

    /**
     * 用户数据权限：VIEW,EDIT,ROLE,DELETE
     */
    private String userScope;

    /**
     * 用户所属部门/班级
     */
    private String department;

    /**
     * 用户所属实验室（用于数据权限过滤，与device_info.laboratory对应）
     */
    private String laboratory;

    /**
     * 判断是否有 VIEW 设备权限
     */
    public boolean canViewDevice() {
        return hasScope("VIEW", deviceScope);
    }

    /**
     * 可预约场景下是否可浏览设备（列表/详情/智能推荐）。
     * 学生/教师/实验室管理员以预约为主流程，即使角色数据权限未勾选「查看设备信息」也允许浏览；
     * 实验室范围仍由 {@code scopeType} / {@code laboratory} / {@code customLabIds} 在后端过滤。
     */
    public boolean canBrowseReservableDevices() {
        if (isSystemAdmin() || "MAINTAINER".equals(userType)) {
            return true;
        }
        if ("STUDENT".equals(userType) || "TEACHER".equals(userType) || "LAB_ADMIN".equals(userType)) {
            return true;
        }
        return canViewDevice();
    }

    /**
     * 判断是否有 EDIT 设备权限
     */
    public boolean canEditDevice() {
        return hasScope("EDIT", deviceScope);
    }

    /**
     * 判断是否有 DELETE 设备权限
     */
    public boolean canDeleteDevice() {
        return hasScope("DELETE", deviceScope);
    }

    /**
     * 判断是否有变更设备状态权限（与前端「STATUS」勾选一致）
     */
    public boolean canChangeDeviceStatusFlag() {
        return hasScope("STATUS", deviceScope);
    }

    /**
     * 判断是否有 VIEW 预约权限
     */
    public boolean canViewBooking() {
        return hasScope("VIEW", bookingScope);
    }

    /**
     * 判断是否有 AUDIT 预约权限
     */
    public boolean canAuditBooking() {
        return hasScope("AUDIT", bookingScope);
    }

    /**
     * 判断是否有 CANCEL 预约权限
     */
    public boolean canCancelBooking() {
        return hasScope("CANCEL", bookingScope);
    }

    /**
     * 判断是否有 EXPORT 预约权限（与权限分配页「导出预约数据」勾选一致）
     */
    public boolean canExportBooking() {
        return hasScope("EXPORT", bookingScope);
    }

    /**
     * 判断是否有 VIEW 用户权限
     */
    public boolean canViewUser() {
        return hasScope("VIEW", userScope);
    }

    /**
     * 判断是否有 EDIT 用户权限
     */
    public boolean canEditUser() {
        return hasScope("EDIT", userScope);
    }

    /**
     * 判断是否有 ROLE 用户权限（分配角色）
     */
    public boolean canAssignRole() {
        return hasScope("ROLE", userScope);
    }

    /**
     * 判断是否有 DELETE 用户权限
     */
    public boolean canDeleteUser() {
        return hasScope("DELETE", userScope);
    }

    /**
     * 判断是否为系统管理员（拥有全部权限）
     */
    public boolean isSystemAdmin() {
        return "SYSTEM_ADMIN".equals(userType);
    }

    /**
     * 判断是否为实验室管理员
     */
    public boolean isLabAdmin() {
        return "LAB_ADMIN".equals(userType);
    }

    /**
     * 判断是否有全部数据权限
     */
    public boolean hasAllScope() {
        return "ALL".equals(scopeType);
    }

    /**
     * 判断是否仅能查看自己的数据（SELF）
     */
    public boolean isSelfScope() {
        return "SELF".equals(scopeType);
    }

    /**
     * 判断是否为部门级数据权限（DEPT）
     */
    public boolean isDeptScope() {
        return "DEPT".equals(scopeType);
    }

    /**
     * 判断是否为自定义数据权限（CUSTOM）
     */
    public boolean isCustomScope() {
        return "CUSTOM".equals(scopeType);
    }

    /**
     * 检查是否包含指定权限
     */
    private boolean hasScope(String scope, String scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        String[] scopeList = scopes.split(",");
        for (String s : scopeList) {
            if (s.trim().equalsIgnoreCase(scope)) {
                return true;
            }
        }
        return false;
    }
}
