package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.entity.SysUserRole;
import com.lab.reservation.mapper.SysUserMapper;
import com.lab.reservation.mapper.SysUserRoleMapper;
import com.lab.reservation.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 用户Service实现类
 * 使用与登录认证相同的 PasswordEncoder Bean，保证注册/重置密码与登录校验一致
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;

    // 角色ID常量
    private static final long ROLE_STUDENT = 4L;
    private static final long ROLE_TEACHER = 3L;
    private static final long ROLE_MAINTAINER = 5L;

    /** 与数据库 init 脚本演示账号一致的默认密码明文（重置后需通知用户并建议尽快修改） */
    private static final String DEFAULT_INITIAL_PASSWORD = "123456";

    public SysUserServiceImpl(PasswordEncoder passwordEncoder, SysUserRoleMapper sysUserRoleMapper) {
        this.passwordEncoder = passwordEncoder;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    @Transactional
    public boolean register(SysUser user) {
        // 检查用户名是否存在
        SysUser existUser = baseMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 加密密码（去除首尾空格，避免与登录输入不一致）
        String rawPassword = user.getPassword() == null ? "" : user.getPassword().trim();
        user.setPassword(passwordEncoder.encode(rawPassword));
        // 设置默认状态
        user.setStatus(1);

        // 先保存用户，获取ID
        baseMapper.insert(user);

        // 为用户分配角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        String userType = user.getUserType() == null ? "STUDENT" : user.getUserType().trim();
        if ("TEACHER".equals(userType)) {
            userRole.setRoleId(ROLE_TEACHER);
        } else if ("MAINTAINER".equals(userType)) {
            userRole.setRoleId(ROLE_MAINTAINER);
        } else {
            userRole.setRoleId(ROLE_STUDENT); // 默认学生角色
        }
        sysUserRoleMapper.insert(userRole);

        return true;
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String oldPwd = oldPassword == null ? "" : oldPassword.trim();
        String newPwd = newPassword == null ? "" : newPassword.trim();
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        String encoded = passwordEncoder.encode(newPwd);
        return baseMapper.updatePasswordById(userId, encoded) > 0;
    }

    @Override
    public boolean resetPasswordByPhone(String username, String phone, String newPassword) {
        SysUser user = baseMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getPhone() == null || !user.getPhone().equals(phone)) {
            throw new RuntimeException("手机号与用户不匹配");
        }
        String newPwd = newPassword == null ? "" : newPassword.trim();
        String encoded = passwordEncoder.encode(newPwd);
        return baseMapper.updatePasswordById(user.getId(), encoded) > 0;
    }

    @Override
    public void resetPasswordToDefaultByAdmin(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new RuntimeException("不能重置内置超级管理员密码");
        }
        String encoded = passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD);
        if (baseMapper.updatePasswordById(userId, encoded) <= 0) {
            throw new RuntimeException("密码重置失败");
        }
    }

    @Override
    public long countAllUsers() {
        return baseMapper.countAllUsers();
    }

    @Override
    public List<Map<String, Object>> listUserMenus(Long userId) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        return baseMapper.selectUserMenus(userId);
    }

    @Override
    public String resolveEffectiveUserType(SysUser user) {
        if (user == null) {
            return null;
        }
        List<String> roleCodes = baseMapper.selectUserRoles(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            return user.getUserType();
        }
        String ut = user.getUserType();
        if (ut != null && roleCodes.contains(ut)) {
            return ut;
        }
        String[] order = {"SYSTEM_ADMIN", "LAB_ADMIN", "MAINTAINER", "TEACHER", "STUDENT"};
        for (String o : order) {
            if (roleCodes.contains(o)) {
                return o;
            }
        }
        return roleCodes.get(0);
    }

    @Override
    public List<String> listUserPermissionCodes(Long userId) {
        List<String> list = baseMapper.selectUserPermissions(userId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String c : list) {
            if (c != null && !c.trim().isEmpty()) {
                set.add(c.trim());
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public void resetMissedCount(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setMissedCount(0);
        baseMapper.updateById(user);
    }
}
