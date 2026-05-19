package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.DeviceStatusLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 设备Service接口
 */
public interface DeviceInfoService extends IService<DeviceInfo> {

    /**
     * 分页查询设备
     */
    Page<DeviceInfo> pageDevices(Integer pageNum, Integer pageSize, DeviceInfo deviceInfo);

    /**
     * 查询所有设备（带分类名称）
     */
    List<DeviceInfo> listDevicesWithCategory();

    /**
     * 根据ID查询设备详情
     */
    DeviceInfo getDeviceDetail(Long id);

    /**
     * 更新设备状态
     */
    boolean updateDeviceStatus(Long id, Integer status);

    /**
     * 统计设备数量
     */
    long countByStatus(Integer status);

    /**
     * 统计设备使用率
     */
    double calculateUsageRate();

    /**
     * 设备类型分布统计
     */
    List<Map<String, Object>> getDeviceTypeDistribution();

    /**
     * 查询设备状态变更历史（按时间倒序）
     */
    List<DeviceStatusLog> getStatusLogs(Long deviceId);

    /**
     * 全量分页查询设备状态变更日志（设备状态追踪页）
     */
    Map<String, Object> pageGlobalStatusLogs(Integer pageNum, Integer pageSize,
            String deviceName, String deviceNo, String changeType, String operator,
            LocalDate startDate, LocalDate endDate, String laboratoryFilter);

    /**
     * 根据关键词搜索设备（用于全局搜索）
     */
    List<DeviceInfo> searchByKeyword(String keyword);
}
