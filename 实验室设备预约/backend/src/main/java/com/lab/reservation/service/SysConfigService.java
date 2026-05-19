package com.lab.reservation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SysConfigService {

    /**
     * 获取所有配置
     */
    List<Map<String, Object>> getAllConfigs();

    /**
     * 按分组获取配置
     */
    List<Map<String, Object>> getConfigsByGroup(String group);

    /**
     * 获取单个配置值（根据类型自动解析）
     */
    String getConfigValue(String key);

    /**
     * 获取布尔值配置
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 获取整数配置
     */
    int getInt(String key, int defaultValue);

    /**
     * 获取JSON数组配置
     */
    List<String> getJsonArray(String key);

    /**
     * 获取JSON对象配置
     */
    Map<String, Object> getJsonObject(String key);

    /**
     * 保存或更新配置
     */
    void saveConfig(String key, String value, String type, String name, String group, String description);

    /**
     * 批量保存配置
     */
    void saveConfigs(List<Map<String, String>> configs);
}
