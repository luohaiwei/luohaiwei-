package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.mapper.SysLogMapper;
import com.lab.reservation.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 系统日志Service实现类
 */
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public Page<SysLog> pageLogs(Integer pageNum, Integer pageSize, String username, String module) {
        Page<SysLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like("username", username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like("module", module);
        }
        wrapper.orderByDesc("create_time");
        return page(page, wrapper);
    }

    @Override
    public Integer countUserLogins(Long userId, String period) {
        return baseMapper.countUserLogins(userId, period);
    }

    @Override
    public List<Map<String, Object>> getUserActivityStats(Integer days, Integer limit) {
        return baseMapper.selectUserActivityStats(days, limit);
    }

    @Override
    public Integer countDistinctLoginUsers(int days) {
        Integer n = baseMapper.countDistinctLoginUsers(days);
        return n != null ? n : 0;
    }

    @Override
    public List<Map<String, Object>> loginDistinctTrend(int days) {
        return baseMapper.selectLoginDistinctTrend(days);
    }

    @Override
    public Page<SysLog> pageOperationLogs(Integer pageNum, Integer pageSize, String username, String module,
            String operationType, Integer status, LocalDateTime start, LocalDateTime end) {
        Page<SysLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<SysLog> w = new QueryWrapper<>();
        if (StringUtils.hasText(username)) {
            w.like("username", username.trim());
        }
        if (StringUtils.hasText(module)) {
            w.like("module", module.trim());
        }
        String op = StringUtils.hasText(operationType) ? operationType.trim() : null;
        if (StringUtils.hasText(op)) {
            if ("AUDIT".equalsIgnoreCase(op)) {
                w.in("operation", Arrays.asList("AUDIT", "PERM_ASSIGN", "PERM_REVOKE", "USER_ROLE_CHANGE", "ROLE_UPDATE", "DATA_SCOPE_SAVE"));
            } else if ("SAVE".equalsIgnoreCase(op)) {
                w.in("operation", Arrays.asList("SAVE", "CREATE", "UPDATE", "INSERT"));
            } else {
                w.eq("operation", op);
            }
        }
        if (status != null) {
            w.eq("status", status);
        }
        if (start != null) {
            w.ge("create_time", start);
        }
        if (end != null) {
            w.le("create_time", end);
        }
        // 默认在「用户操作日志」中排除登录/登出，避免与登录日志重复；
        // 当用户显式筛选 LOGIN/LOGOUT 时，允许查出对应记录。
        boolean queryingLoginLogout = StringUtils.hasText(op)
                && ("LOGIN".equalsIgnoreCase(op) || "LOGOUT".equalsIgnoreCase(op));
        if (!queryingLoginLogout) {
            w.and(q -> q.isNull("request_url").or(q2 -> q2
                    .notLike("request_url", "%/auth/login%")
                    .notLike("request_url", "%/auth/logout%")
                    .notLike("request_url", "%/user/login%")
                    .notLike("request_url", "%/user/logout%")));
        }
        w.orderByDesc("create_time");
        return page(page, w);
    }

    @Override
    public Page<SysLog> pageLoginLogs(Integer pageNum, Integer pageSize, String username, Integer status,
            LocalDateTime start, LocalDateTime end) {
        Page<SysLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<SysLog> w = new QueryWrapper<>();
        // 兼容历史数据：曾用 CREATE + /auth/login，现统一为 LOGIN；URL 匹配覆盖旧记录
        w.and(q -> q.eq("operation", "LOGIN")
                .or().eq("operation", "LOGOUT")
                .or().like("request_url", "%/auth/login%")
                .or().like("request_url", "%/user/login%")
                .or().like("request_url", "%/auth/logout%")
                .or().like("request_url", "%/user/logout%"));
        if (StringUtils.hasText(username)) {
            w.like("username", username.trim());
        }
        if (status != null) {
            w.eq("status", status);
        }
        if (start != null) {
            w.ge("create_time", start);
        }
        if (end != null) {
            w.le("create_time", end);
        }
        w.orderByDesc("create_time");
        return page(page, w);
    }

    @Override
    public Page<SysLog> pagePermissionLogs(Integer pageNum, Integer pageSize, String operator, String changeType,
            LocalDateTime start, LocalDateTime end) {
        Page<SysLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<SysLog> w = new QueryWrapper<>();
        // 仅展示「真实权限/角色变更」审计记录。勿用 module LIKE「角色/权限」：会把角色列表、权限树等 GET（operation=QUERY）全查进来。
        w.in("operation", Arrays.asList(
                "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE",
                "PERM_ASSIGN", "PERM_REVOKE", "USER_ROLE_CHANGE",
                "DATA_SCOPE_SAVE"));
        if (StringUtils.hasText(operator)) {
            w.like("username", operator.trim());
        }
        if (StringUtils.hasText(changeType)) {
            w.eq("operation", changeType.trim());
        }
        if (start != null) {
            w.ge("create_time", start);
        }
        if (end != null) {
            w.le("create_time", end);
        }
        w.orderByDesc("create_time");
        return page(page, w);
    }
}
