package com.lab.reservation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.mapper.SysLogMapper;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.utils.LogDisplayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统日志控制器（含日志审计管理多 Tab）
 */
@RestController
@RequestMapping("/sys/log")
@CrossOrigin
public class SysLogController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysLogMapper sysLogMapper;

    @GetMapping("/list")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('log')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module) {
        Page<SysLog> page = sysLogService.pageLogs(pageNum, pageSize, username, module);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/operation")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('log')")
    public ResponseEntity<Map<String, Object>> operationLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        Page<SysLog> page = sysLogService.pageOperationLogs(pageNum, pageSize, username, module,
                operationType, status, st, et);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysLog log : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("username", log.getUsername());
            m.put("userType", log.getUserType());
            m.put("module", LogDisplayUtil.moduleToZh(log.getModule()));
            m.put("operation", log.getOperation());
            m.put("operationType", log.getOperation());
            m.put("operationDesc", LogDisplayUtil.buildOperationDesc(
                    log.getModule(), log.getOperation(), log.getMethod(), log.getRequestUrl()));
            m.put("requestUrl", log.getRequestUrl());
            m.put("rawIpAddress", log.getIpAddress());
            m.put("ipAddress", LogDisplayUtil.ipToChineseLabel(log.getIpAddress()));
            m.put("status", log.getStatus());
            m.put("createTime", log.getCreateTime());
            rows.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/login")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('log')")
    public ResponseEntity<Map<String, Object>> loginLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        Page<SysLog> page = sysLogService.pageLoginLogs(pageNum, pageSize, username, status, st, et);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysLog log : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", log.getUsername());
            m.put("userType", firstNonBlank(log.getUserType(), jsonStringField(log.getRequestParams(), "userType")));
            m.put("loginType", "NORMAL");
            m.put("rawIpAddress", log.getIpAddress());
            m.put("ipAddress", LogDisplayUtil.ipToChineseLabel(log.getIpAddress()));
            m.put("ipSource", LogDisplayUtil.ipLocationLabel(log.getIpAddress()));
            m.put("userAgent", jsonStringField(log.getRequestParams(), "_userAgent"));
            m.put("status", log.getStatus());
            m.put("failReason", log.getErrorMsg());
            m.put("createTime", log.getCreateTime());
            rows.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/device-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deviceStatusLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        Map<String, Object> raw = deviceInfoService.pageGlobalStatusLogs(
                pageNum, pageSize, deviceName, null, changeType, operator, start, end, null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> src = (List<Map<String, Object>>) raw.get("list");
        List<Map<String, Object>> list = new ArrayList<>();
        if (src != null) {
            for (Map<String, Object> row : src) {
                Map<String, Object> m = new HashMap<>(row);
                // 前端：标签用 changeType（IDLE/…），文案用 toStatusText/fromStatusText
                Object ct = row.get("changeType");
                if (ct != null) {
                    m.put("toStatus", ct);
                }
                String rawIp = row.get("ipAddress") != null ? String.valueOf(row.get("ipAddress")) : null;
                if (StringUtils.hasText(rawIp)) {
                    m.put("rawIpAddress", rawIp);
                    m.put("ipAddress", LogDisplayUtil.ipToChineseLabel(rawIp));
                } else {
                    m.put("ipAddress", "未记录");
                    m.put("rawIpAddress", "");
                }
                list.add(m);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", raw.get("total"));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/permission")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-track') or hasAuthority('device-status') or hasAuthority('device-list') or hasAuthority('log')")
    public ResponseEntity<Map<String, Object>> permissionLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        Page<SysLog> page = sysLogService.pagePermissionLogs(pageNum, pageSize, operator, changeType, st, et);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysLog log : page.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("operator", log.getUsername());
            m.put("targetUser", extractTargetUser(log.getRequestParams(), log.getOperation()));
            m.put("changeType", log.getOperation());
            String targetRole = extractTargetRole(log.getRequestParams());
            m.put("targetRole", targetRole);
            m.put("changeDetail", formatChangeDetail(log));
            m.put("rawIpAddress", log.getIpAddress());
            m.put("ipAddress", LogDisplayUtil.ipToChineseLabel(log.getIpAddress()));
            m.put("createTime", log.getCreateTime());
            list.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('log')")
    public ResponseEntity<byte[]> export(
            @RequestParam String logType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) throws Exception {
        StringBuilder csv = new StringBuilder("\uFEFF");
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        LocalDate sd = parseDate(startDate);
        LocalDate ed = parseDate(endDate);
        if ("operation".equals(logType)) {
            csv.append("时间,用户,模块,操作,URL,状态\n");
            Page<SysLog> page = sysLogService.pageOperationLogs(1, 5000, username, module, operationType, status, st, et);
            for (SysLog log : page.getRecords()) {
                csv.append(esc(log.getCreateTime())).append(',')
                        .append(esc(log.getUsername())).append(',')
                        .append(esc(log.getModule())).append(',')
                        .append(esc(log.getOperation())).append(',')
                        .append(esc(log.getRequestUrl())).append(',')
                        .append(log.getStatus()).append('\n');
            }
        } else if ("login".equals(logType)) {
            csv.append("时间,用户,IP,状态\n");
            Page<SysLog> page = sysLogService.pageLoginLogs(1, 5000, username, status, st, et);
            for (SysLog log : page.getRecords()) {
                csv.append(esc(log.getCreateTime())).append(',')
                        .append(esc(log.getUsername())).append(',')
                        .append(esc(log.getIpAddress())).append(',')
                        .append(log.getStatus()).append('\n');
            }
        } else if ("device".equals(logType)) {
            csv.append("设备名称,设备编号,变更类型,操作人,时间\n");
            Map<String, Object> raw = deviceInfoService.pageGlobalStatusLogs(1, 5000, deviceName, null, changeType, operator, sd, ed, null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> src = (List<Map<String, Object>>) raw.get("list");
            if (src != null) {
                for (Map<String, Object> row : src) {
                    csv.append(esc(row.get("deviceName"))).append(',')
                            .append(esc(row.get("deviceNo"))).append(',')
                            .append(esc(row.get("changeType"))).append(',')
                            .append(esc(row.get("operator"))).append(',')
                            .append(esc(row.get("createTime"))).append('\n');
                }
            }
        } else {
            csv.append("时间,用户,模块,操作类型,详情\n");
            Page<SysLog> page = sysLogService.pagePermissionLogs(1, 5000, operator, changeType, st, et);
            for (SysLog log : page.getRecords()) {
                csv.append(esc(log.getCreateTime())).append(',')
                        .append(esc(log.getUsername())).append(',')
                        .append(esc(log.getModule())).append(',')
                        .append(esc(log.getOperation())).append(',')
                        .append(esc(log.getRequestParams())).append('\n');
            }
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "sys_log_" + logType + "_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                        + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private static String esc(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return StringUtils.hasText(b) ? b : null;
    }

    private static String jsonStringField(String json, String key) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(key)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = OBJECT_MAPPER.readValue(json, Map.class);
            Object v = map.get(key);
            return v != null ? String.valueOf(v) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 格式化变更详情为可读文本，过滤 _userAgent 等无关字段
     */
    private static String formatChangeDetail(SysLog log) {
        String raw = log.getRequestParams();
        if (!StringUtils.hasText(raw)) {
            return log.getResponseResult() != null ? log.getResponseResult() : "-";
        }
        // 若是纯 JSON（以 { 开头），尝试解析并提取关键字段
        if (raw.trim().startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = OBJECT_MAPPER.readValue(raw, Map.class);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    String k = e.getKey();
                    // 跳过 _userAgent、分页参数等无审计意义的字段
                    if (k == null || k.startsWith("_") || "userType".equals(k)
                            || "pageNum".equals(k) || "pageSize".equals(k) || "current".equals(k) || "size".equals(k)) {
                        continue;
                    }
                    if (sb.length() > 0) sb.append("；");
                    sb.append(k).append("：").append(e.getValue());
                }
                return sb.length() > 0 ? sb.toString() : "-";
            } catch (Exception ex) {
                // 解析失败则原样输出
            }
        }
        return raw;
    }

    private static String extractTargetUser(String detail, String operation) {
        if (!StringUtils.hasText(detail)) {
            return "-";
        }
        if ("USER_ROLE_CHANGE".equals(operation)) {
            Matcher m = Pattern.compile("目标用户[:：]\\s*[^\\s(]+\\s*\\(([^)]+)\\)").matcher(detail);
            if (m.find()) {
                return m.group(1).trim();
            }
            m = Pattern.compile("目标用户[:：]\\s*(\\S+)").matcher(detail);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return "-";
    }

    private static String extractTargetRole(String detail) {
        if (!StringUtils.hasText(detail)) {
            return "未知";
        }
        Matcher m1 = Pattern.compile("角色\\s*\\[([^\\]]+)\\]").matcher(detail);
        if (m1.find()) {
            return m1.group(1).trim();
        }
        Matcher m2 = Pattern.compile("新增角色[:：]\\s*([^(\n]+)").matcher(detail);
        if (m2.find()) {
            return m2.group(1).trim();
        }
        Matcher m3 = Pattern.compile("修改角色[:：]\\s*([^(\n]+)").matcher(detail);
        if (m3.find()) {
            return m3.group(1).trim();
        }
        Matcher m4 = Pattern.compile("删除角色[:：]\\s*([^(\n]+)").matcher(detail);
        if (m4.find()) {
            return m4.group(1).trim();
        }
        return "未知";
    }

    /**
     * 立即清理旧日志
     * 保留最近的100条日志
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(@RequestParam(defaultValue = "100") Integer keepCount) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 查询总日志数
            long totalCount = sysLogMapper.selectCount(null);

            if (totalCount <= keepCount) {
                result.put("success", true);
                result.put("message", "日志总数 " + totalCount + " 条，未超过保留条数 " + keepCount + "，无需清理");
                result.put("totalCount", totalCount);
                result.put("keepCount", keepCount);
                result.put("deletedCount", 0);
                return ResponseEntity.ok(result);
            }

            // 查询需要保留的最新日志的最小ID
            QueryWrapper<SysLog> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("id");
            wrapper.last("LIMIT " + keepCount);

            java.util.List<SysLog> logsToKeep = sysLogMapper.selectList(wrapper);

            if (logsToKeep.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有需要保留的日志");
                return ResponseEntity.ok(result);
            }

            // 获取需要保留的最小ID
            Long minIdToKeep = logsToKeep.get(logsToKeep.size() - 1).getId();

            // 删除ID小于minIdToKeep的日志
            QueryWrapper<SysLog> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.lt("id", minIdToKeep);

            int deletedCount = sysLogMapper.delete(deleteWrapper);

            result.put("success", true);
            result.put("message", "清理完成：总日志 " + totalCount + " 条，保留 " + keepCount + " 条，删除 " + deletedCount + " 条");
            result.put("totalCount", totalCount);
            result.put("keepCount", keepCount);
            result.put("deletedCount", deletedCount);

            System.out.println("[日志清理] 手动清理完成：总日志 " + totalCount + " 条，保留 " + keepCount + " 条，删除 " + deletedCount + " 条");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("[日志清理] 手动清理失败: " + e.getMessage());
            e.printStackTrace();

            result.put("success", false);
            result.put("message", "清理失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
