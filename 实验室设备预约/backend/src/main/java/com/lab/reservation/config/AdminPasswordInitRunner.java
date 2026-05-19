package com.lab.reservation.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.entity.SysUserRole;
import com.lab.reservation.mapper.SysUserMapper;
import com.lab.reservation.mapper.SysUserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动时检查并修复 admin 用户密码与角色，确保使用当前 PasswordEncoder 生成的哈希，避免与数据库中的旧哈希不一致导致无法登录。
 */
@Component
@Order(1)
public class AdminPasswordInitRunner implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final long ROLE_SYSTEM_ADMIN = 1L;

    public AdminPasswordInitRunner(SysUserMapper sysUserMapper,
                                  SysUserRoleMapper sysUserRoleMapper,
                                  PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            SysUser admin = sysUserMapper.selectByUsername(ADMIN_USERNAME);
            if (admin == null) return;

            boolean needUpdate = false;
            if (!passwordEncoder.matches(DEFAULT_ADMIN_PASSWORD, admin.getPassword())) {
                String encoded = passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD);
                sysUserMapper.updatePasswordById(admin.getId(), encoded);
                needUpdate = true;
            }

            Long count = sysUserRoleMapper.selectCount(
                    new QueryWrapper<SysUserRole>()
                            .eq("user_id", admin.getId())
                            .eq("role_id", ROLE_SYSTEM_ADMIN));
            if (count == null || count == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(admin.getId());
                ur.setRoleId(ROLE_SYSTEM_ADMIN);
                sysUserRoleMapper.insert(ur);
                needUpdate = true;
            }

            if (needUpdate) {
                System.out.println("[AdminPasswordInit] admin 用户密码或角色已修复，可使用 admin / 123456 登录");
            }
        } catch (Exception e) {
            System.err.println("[AdminPasswordInit] 检查 admin 用户时出错: " + e.getMessage());
        }
    }
}
