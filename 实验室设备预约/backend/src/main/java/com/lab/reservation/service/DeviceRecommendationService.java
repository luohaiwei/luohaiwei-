package com.lab.reservation.service;

import com.lab.reservation.entity.DeviceInfo;

import java.util.List;
import java.util.Map;

/**
 * 设备智能推荐服务接口
 * 基于协同过滤算法推荐设备及最优使用时段
 */
public interface DeviceRecommendationService {

    /**
     * 基于协同过滤推荐设备
     * 根据用户历史预约、专业方向、设备使用率进行推荐
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 推荐设备列表及推荐理由
     */
    List<Map<String, Object>> recommendDevices(Long userId, int limit);

    /**
     * 推荐最优预约时段
     * 基于历史预约数据，避开高峰，推荐空闲时段
     *
     * @param deviceId 设备ID
     * @param dateStr  日期 yyyy-MM-dd
     * @return 推荐时段列表
     */
    List<Map<String, Object>> recommendTimeSlots(Long deviceId, String dateStr);

    /**
     * 清除指定用户的推荐缓存
     *
     * @param userId 用户ID
     */
    void clearUserCache(Long userId);

    /**
     * 清除所有推荐缓存
     */
    void clearAllCache();
}
