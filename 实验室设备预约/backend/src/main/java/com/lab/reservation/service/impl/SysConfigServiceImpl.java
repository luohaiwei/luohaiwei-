package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.entity.SysConfig;
import com.lab.reservation.mapper.SysConfigMapper;
import com.lab.reservation.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 系统配置服务实现
 */
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigServiceImpl.class);

    @Autowired
    private SysConfigMapper sysConfigMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> getAllConfigs() {
        List<SysConfig> configs = sysConfigMapper.selectList(new QueryWrapper<>());
        return convertToMapList(configs);
    }

    @Override
    public List<Map<String, Object>> getConfigsByGroup(String group) {
        List<SysConfig> configs = sysConfigMapper.selectByGroup(group);
        return convertToMapList(configs);
    }

    @Override
    public String getConfigValue(String key) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) return null;
        return parseValue(config.getConfigValue(), config.getConfigType());
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> getJsonArray(String key) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) return Collections.emptyList();
        String raw = config.getConfigValue();
        if (raw == null) return Collections.emptyList();
        try {
            // CASE 1: 已是标准 JSON 数组 [..]
            if (raw.startsWith("[") && raw.endsWith("]")) {
                return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            }
            // CASE 2: "[\"val\"]"  双引号包裹的 JSON 数组
            if (raw.startsWith("\"[") && raw.endsWith("]\"")) {
                String inner = raw.substring(1, raw.length() - 1);
                return objectMapper.readValue(inner, new TypeReference<List<String>>() {});
            }
            // CASE 3: 单个值如 "AUTO" 或 AUTO  — 去掉外层双引号后当作唯一元素
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                String inner = raw.substring(1, raw.length() - 1);
                if (!inner.startsWith("[")) {
                    // 纯字符串值，返回单元素列表
                    return Collections.singletonList(inner);
                }
                return objectMapper.readValue(inner, new TypeReference<List<String>>() {});
            }
            // CASE 4: 直接数组字面量（无外层引号）
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON数组配置失败 key={}, raw={}, error={}", key, raw, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getJsonObject(String key) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) return Collections.emptyMap();
        String raw = config.getConfigValue();
        if (raw == null) return Collections.emptyMap();
        try {
            // 如果是带引号的JSON对象字符串，先去掉引号
            if (raw.startsWith("\"{") && raw.endsWith("}\"")) {
                String inner = raw.substring(1, raw.length() - 1);
                return objectMapper.readValue(inner, new TypeReference<Map<String, Object>>() {});
            }
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析JSON对象配置失败 key={}, raw={}, error={}", key, raw, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public void saveConfig(String key, String value, String type, String name, String group, String description) {
        SysConfig existing = sysConfigMapper.selectByKey(key);
        // 统一存储格式：
        // - JSON类型：直接存原始JSON字符串（前端已 stringify），无需再加引号
        // - STRING类型：若value是JSON字符串（首尾双引号），去掉引号存原始值
        String storeValue = value;
        if ("STRING".equals(type) && value != null
                && value.startsWith("\"") && value.endsWith("\"")) {
            // 去掉首尾引号还原为原始字符串
            storeValue = value.substring(1, value.length() - 1);
        }
        if (existing != null) {
            existing.setConfigValue(storeValue);
            existing.setConfigType(type);
            existing.setConfigName(name);
            existing.setConfigGroup(group);
            existing.setDescription(description);
            sysConfigMapper.updateById(existing);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(storeValue);
            config.setConfigType(type);
            config.setConfigName(name);
            config.setConfigGroup(group);
            config.setDescription(description);
            sysConfigMapper.insert(config);
        }
    }

    @Override
    public void saveConfigs(List<Map<String, String>> configs) {
        for (Map<String, String> cfg : configs) {
            saveConfig(
                cfg.get("key"),
                cfg.get("value"),
                cfg.getOrDefault("type", "STRING"),
                cfg.getOrDefault("name", ""),
                cfg.getOrDefault("group", "SYSTEM"),
                cfg.getOrDefault("description", "")
            );
        }
    }

    private String parseValue(String raw, String type) {
        if (raw == null) return null;
        if ("STRING".equals(type)) {
            // 去掉首尾引号（JSON字符串格式）
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                return raw.substring(1, raw.length() - 1);
            }
        }
        return raw;
    }

    private Object parseRawValue(String raw, String type) {
        if (raw == null) return null;
        if ("STRING".equals(type)) {
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                return raw.substring(1, raw.length() - 1);
            }
            return raw;
        }
        if ("JSON".equals(type)) {
            try {
                return objectMapper.readValue(raw, Object.class);
            } catch (Exception e) {
                log.warn("解析JSON值失败 key type={}, raw={}, error={}", type, raw, e.getMessage());
                return raw;
            }
        }
        return raw;
    }

    private List<Map<String, Object>> convertToMapList(List<SysConfig> configs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysConfig c : configs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("configKey", c.getConfigKey());
            // JSON类型需要特殊处理：如果是JSON字符串则解析
            Object parsedValue = parseRawValue(c.getConfigValue(), c.getConfigType());
            m.put("configValue", parsedValue);
            m.put("configType", c.getConfigType());
            m.put("configName", c.getConfigName());
            m.put("configGroup", c.getConfigGroup());
            m.put("description", c.getDescription());
            result.add(m);
        }
        return result;
    }
}
