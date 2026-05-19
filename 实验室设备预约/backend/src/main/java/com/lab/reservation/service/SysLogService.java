package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.SysLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统日志Service接口
 */
public interface SysLogService extends IService<SysLog> {

    /**
     * 分页查询日志
     */
    Page<SysLog> pageLogs(Integer pageNum, Integer pageSize, String username, String module);

    /**
     * 获取用户登录次数
     * @param userId 用户ID
     * @param period 统计周期：day/week/month
     * @return 登录次数
     */
    Integer countUserLogins(Long userId, String period);

    /**
     * 获取用户活跃度统计列表
     * @param days 统计天数
     * @param limit 返回数量限制
     * @return 用户活跃度统计列表
     */
    List<Map<String, Object>> getUserActivityStats(Integer days, Integer limit);

    Integer countDistinctLoginUsers(int days);

    List<Map<String, Object>> loginDistinctTrend(int days);

    Page<SysLog> pageOperationLogs(Integer pageNum, Integer pageSize, String username, String module,
            String operationType, Integer status, LocalDateTime start, LocalDateTime end);

    Page<SysLog> pageLoginLogs(Integer pageNum, Integer pageSize, String username, Integer status,
            LocalDateTime start, LocalDateTime end);

    Page<SysLog> pagePermissionLogs(Integer pageNum, Integer pageSize, String operator, String changeType,
            LocalDateTime start, LocalDateTime end);
}
