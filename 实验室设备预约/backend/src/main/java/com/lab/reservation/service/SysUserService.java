package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.SysUser;

import java.util.List;
import java.util.Map;

/**
 * 用户Service接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);

    /**
     * 注册用户
     */
    boolean register(SysUser user);

    /**
     * 修改密码
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 通过手机号重置密码
     */
    boolean resetPasswordByPhone(String username, String phone, String newPassword);

    /**
     * 管理员将用户密码重置为系统默认初始密码（与演示数据一致，BCrypt 存储）
     */
    void resetPasswordToDefaultByAdmin(Long userId);

    /**
     * 统计未删除用户总数（显式SQL，避免 MyBatis-Plus 自动填充逻辑误判）
     */
    long countAllUsers();

    /**
     * 用户侧栏菜单行（与 sys_role_permission 一致）
     */
    List<Map<String, Object>> listUserMenus(Long userId);

    /**
     * 登录/会话展示用角色编码：以 sys_user_role 为准，避免 user_type 与关联表不一致
     */
    String resolveEffectiveUserType(SysUser user);

    /**
     * 用户通过角色关联获得的全部权限编码（含菜单与按钮），与 Spring Security 授权一致
     */
    List<String> listUserPermissionCodes(Long userId);

    /**
     * 管理员将指定用户的爽约次数清零（解除禁止预约状态）
     * @param userId 用户ID
     */
    void resetMissedCount(Long userId);
}
