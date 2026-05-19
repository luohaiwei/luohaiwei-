package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.SysUserMapper;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户DetailsService实现类
 * 用于Spring Security加载用户信息
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 根据用户名加载用户信息
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用");
        }

        // 查询用户角色
        List<String> roles = sysUserMapper.selectUserRoles(user.getId());
        // 查询用户权限
        List<String> permissions = sysUserMapper.selectUserPermissions(user.getId());

        // 当用户从未被分配过任何角色时（roles 为 null 或空），根据 userType 自动赋予预置角色；
        // 若 userType 本身也是一个自定义角色名，同样追加进去（确保自定义角色不被遗漏）
        if (roles == null || roles.isEmpty()) {
            roles = new ArrayList<>();
            String userType = user.getUserType();
            if ("SYSTEM_ADMIN".equals(userType)) {
                roles.add("SYSTEM_ADMIN");
            } else if ("LAB_ADMIN".equals(userType)) {
                roles.add("LAB_ADMIN");
            } else if ("TEACHER".equals(userType)) {
                roles.add("TEACHER");
            } else if ("STUDENT".equals(userType)) {
                roles.add("STUDENT");
            } else if ("MAINTAINER".equals(userType)) {
                roles.add("MAINTAINER");
            }
            // 兜底：若 userType 非空但不在上面，也作为角色名加入
            if (userType != null && !userType.isEmpty() && !roles.contains(userType)) {
                roles.add(userType);
            }
        }
        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        // 构建UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,
                true,
                true,
                true,
                authorities(roles, permissions)
        );
    }

    /**
     * 构建权限集合
     */
    private Collection<? extends GrantedAuthority> authorities(List<String> roles, List<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 添加角色（Spring Security角色需要ROLE_前缀）
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        // 添加权限
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }
}
