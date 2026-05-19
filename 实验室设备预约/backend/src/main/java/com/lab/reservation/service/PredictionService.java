package com.lab.reservation.service;

import java.util.List;
import java.util.Map;

/**
 * 预测服务接口
 * 提供预约趋势预测、设备使用率预测等功能
 */
public interface PredictionService {

    /**
     * 预测预约趋势
     * @param historyDays 历史数据天数，默认30天
     * @param predictDays 预测天数，默认7天
     * @return 预测结果列表
     */
    List<Map<String, Object>> predictBookingTrend(int historyDays, int predictDays);

    /**
     * 预测设备使用率
     * @param deviceId 设备ID（可选，为null则预测全部设备）
     * @param historyDays 历史数据天数
     * @param predictDays 预测天数
     * @return 设备使用率预测结果
     */
    List<Map<String, Object>> predictDeviceUsage(Long deviceId, int historyDays, int predictDays);

    /**
     * 获取预测配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfig(String key, String defaultValue);

    /**
     * 获取预测配置值（整数）
     */
    default int getConfigInt(String key, int defaultValue) {
        String value = getConfig(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取预测配置值（浮点数）
     */
    default double getConfigDouble(String key, double defaultValue) {
        String value = getConfig(key, null);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 检查预测功能是否启用
     */
    default boolean isPredictionEnabled() {
        String value = getConfig("prediction.enabled", "true");
        return "true".equalsIgnoreCase(value);
    }
}
