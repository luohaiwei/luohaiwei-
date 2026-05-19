package com.lab.reservation.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预约规则控制器（仅全局规则，系统管理员）
 * 注意：原 booking_rule 表的 CRUD 功能已废弃，预约规则统一通过 sys_config 存储
 */
@RestController
@RequestMapping("/booking-rule")
@CrossOrigin
public class BookingRuleController {

    @Autowired
    private SysConfigService sysConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CFG_BOOKING_GLOBAL_BASIC = "booking.global.basic";
    private static final String CFG_BOOKING_GLOBAL_ROLE = "booking.global.roleLimits";
    private static final String CFG_BOOKING_GLOBAL_TIME = "booking.global.timeRange";

    /**
     * 全局预约规则——读取
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('booking-rule-global')")
    public ResponseEntity<Map<String, Object>> getGlobalBookingRules() {
        Map<String, Object> out = new HashMap<>();
        out.put("rule", readJsonMap(sysConfigService.getConfigValue(CFG_BOOKING_GLOBAL_BASIC), defaultBasicRule()));
        out.put("roleLimits", readJsonMap(sysConfigService.getConfigValue(CFG_BOOKING_GLOBAL_ROLE), defaultRoleLimits()));
        out.put("timeRange", readJsonMap(sysConfigService.getConfigValue(CFG_BOOKING_GLOBAL_TIME), defaultTimeRange()));
        return ResponseEntity.ok(out);
    }

    /**
     * 全局预约规则——按块保存
     */
    @PutMapping("/global")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('booking-rule-global')")
    public ResponseEntity<Map<String, Object>> saveGlobalBookingRules(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String type = body.get("type") != null ? body.get("type").toString() : "";
            Object data = body.get("data");
            if (data == null) {
                res.put("message", "data 不能为空");
                return ResponseEntity.badRequest().body(res);
            }
            String json = objectMapper.writeValueAsString(data);
            switch (type) {
                case "basic": {
                    Map<String, Object> basicMap = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
                    validateBasicRule(basicMap);
                    sysConfigService.saveConfig(CFG_BOOKING_GLOBAL_BASIC, json, "JSON",
                            "全局预约基础规则", "BOOKING_RULE", "");
                    break;
                }
                case "roleLimits": {
                    Map<String, Object> roleMap = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
                    validateRoleLimits(roleMap);
                    sysConfigService.saveConfig(CFG_BOOKING_GLOBAL_ROLE, json, "JSON",
                            "按角色预约限额", "BOOKING_RULE", "");
                    break;
                }
                case "timeRange": {
                    Map<String, Object> timeMap = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
                    validateTimeRange(timeMap);
                    sysConfigService.saveConfig(CFG_BOOKING_GLOBAL_TIME, json, "JSON",
                            "全局工作时段", "BOOKING_RULE", "");
                    break;
                }
                default:
                    res.put("message", "不支持的 type");
                    return ResponseEntity.badRequest().body(res);
            }
            res.put("message", "保存成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    private Map<String, Object> readJsonMap(String json, Map<String, Object> defaults) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>(defaults);
        }
        try {
            Map<String, Object> m = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> merged = new LinkedHashMap<>(defaults);
            merged.putAll(m);
            return merged;
        } catch (Exception e) {
            return new LinkedHashMap<>(defaults);
        }
    }

    private static Map<String, Object> defaultBasicRule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minAdvanceHours", 24);
        m.put("maxBookingHours", 4);
        m.put("maxBookingsPerDevicePerDay", 3);
        m.put("maxBookingsPerUserPerDay", 5);
        m.put("cancelDeadlineHours", 2);
        m.put("noShowThreshold", 3);
        return m;
    }

    private static Map<String, Object> defaultRoleLimits() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studentDeviceHours", 4);
        m.put("teacherDeviceHours", 8);
        m.put("maintainerCanBook", false);
        m.put("studentNeedAudit", true);
        return m;
    }

    private static Map<String, Object> defaultTimeRange() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("start", "08:00");
        m.put("end", "22:00");
        m.put("weekdays", Arrays.asList("1", "2", "3", "4", "5"));
        return m;
    }

    private static void validateBasicRule(Map<String, Object> m) {
        if (m == null) {
            throw new IllegalArgumentException("data 无效");
        }
        requireIntInRange(m, "minAdvanceHours", 1, 720);
        requireIntInRange(m, "maxBookingHours", 1, 24);
        requireIntInRange(m, "maxBookingsPerDevicePerDay", 1, 50);
        requireIntInRange(m, "maxBookingsPerUserPerDay", 1, 50);
        requireIntInRange(m, "cancelDeadlineHours", 0, 168);
        requireIntInRange(m, "noShowThreshold", 1, 99);
    }

    private static void validateRoleLimits(Map<String, Object> m) {
        if (m == null) {
            throw new IllegalArgumentException("data 无效");
        }
        requireIntInRange(m, "studentDeviceHours", 1, 24);
        requireIntInRange(m, "teacherDeviceHours", 1, 24);
    }

    private static void validateTimeRange(Map<String, Object> m) {
        if (m == null) {
            throw new IllegalArgumentException("data 无效");
        }
        String start = m.get("start") != null ? m.get("start").toString().trim() : "";
        String end = m.get("end") != null ? m.get("end").toString().trim() : "";
        if (start.isEmpty() || end.isEmpty()) {
            throw new IllegalArgumentException("开始/结束时间不能为空");
        }
        LocalTime ts = parseHm(start);
        LocalTime te = parseHm(end);
        if (!te.isAfter(ts)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        @SuppressWarnings("unchecked")
        List<Object> wd = (List<Object>) m.get("weekdays");
        if (wd == null || wd.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个星期");
        }
        for (Object o : wd) {
            int d = Integer.parseInt(o.toString().trim());
            if (d < 1 || d > 7) {
                throw new IllegalArgumentException("星期配置无效");
            }
        }
    }

    private static LocalTime parseHm(String s) {
        String[] p = s.split(":");
        int h = Integer.parseInt(p[0].trim());
        int mi = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
        return LocalTime.of(h, mi);
    }

    private static void requireIntInRange(Map<String, Object> m, String key, int min, int max) {
        int v = asInt(m.get(key), key);
        if (v < min || v > max) {
            throw new IllegalArgumentException(key + " 须在 " + min + "~" + max + " 范围内");
        }
    }

    private static int asInt(Object o, String key) {
        if (o == null) {
            throw new IllegalArgumentException("缺少字段: " + key);
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString().trim());
    }
}
