package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.CalibrationRecord;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.RepairOrder;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.RepairOrderService;
import com.lab.reservation.service.StatisticsExportService;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据统计控制器
 * 提供首页统计数据和图表数据，支持Excel/PDF导出。
 * <p>通用图表/看板类接口使用 {@code isAuthenticated()}：自定义角色仅能通过菜单进入本页，
 * 若仍按内置五角色做 hasAnyRole，会导致 403 且前端静默失败表现为「全 0、图表空白」。
 * 管理员专属的分项统计（/user、设备深度分析等）仍仅 SYSTEM_ADMIN。
 */
@RestController
@RequestMapping("/statistics")
@CrossOrigin
public class StatisticsController {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private StatisticsExportService statisticsExportService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private com.lab.reservation.service.SysUserService sysUserService;

    @Autowired
    private com.lab.reservation.service.CalibrationRecordService calibrationRecordService;

    @Autowired
    private PredictionService predictionService;

    /**
     * 获取首页统计数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        // 用户总数
        data.put("totalUsers", sysUserService.countAllUsers());
        // 预约总数
        data.put("totalBookings", bookingOrderService.countAllBookings());
        // 设备总数
        data.put("totalDevices", deviceInfoService.count());
        // 设备使用率
        data.put("usageRate", deviceInfoService.calculateUsageRate());
        // 今日预约数
        data.put("todayBookings", bookingOrderService.getTodayBookingCount());
        // 本周预约数
        data.put("weekBookings", bookingOrderService.getWeekBookingCount());
        // 空闲设备数
        data.put("idleDevices", deviceInfoService.countByStatus(0));
        // 使用中设备数
        data.put("usingDevices", deviceInfoService.countByStatus(1));
        // 维修中设备数
        data.put("maintainingDevices", deviceInfoService.countByStatus(2));
        // 待审核预约数
        data.put("pendingAudits", bookingOrderService.countPendingAudit());
        // 待处理报修数（status=0 待处理 + status=1 处理中，因自动分配机制导致新工单直接进入处理中状态）
        data.put("pendingRepairs", repairOrderService.count(
                Wrappers.<RepairOrder>lambdaQuery().in(RepairOrder::getStatus, 0, 1)));
        // 处理中工单数（status=1）
        data.put("processingRepairs", repairOrderService.count(
                Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getStatus, 1)));
        // 设备状态异常数（维修中 status=2 + 校准中 status=3）
        data.put("abnormalDevices", deviceInfoService.countByStatus(2) + deviceInfoService.countByStatus(3));
        // 校准到期提醒数（下次校准时间 <= 当前日期 + 7 天）
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(7);
        long calibrationDueCount = deviceInfoService.count(
                Wrappers.<DeviceInfo>lambdaQuery()
                        .eq(DeviceInfo::getDeleted, 0)
                        .isNotNull(DeviceInfo::getNextCalibrationDate)
                        .le(DeviceInfo::getNextCalibrationDate, deadline.atStartOfDay()));
        data.put("calibrationDueCount", calibrationDueCount);
        // 待校准设备数（与calibrationDueCount相同，供前端使用）
        data.put("pendingCalibrations", calibrationDueCount);
        // 今日使用记录（今日已完成的预约数）
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        long completedTodayBookings = bookingOrderService.count(
                Wrappers.<BookingOrder>lambdaQuery()
                        .eq(BookingOrder::getStatus, 3)
                        .eq(BookingOrder::getDeleted, 0)
                        .ge(BookingOrder::getStartTime, todayStart)
                        .le(BookingOrder::getEndTime, todayEnd));
        data.put("completedTodayBookings", completedTodayBookings);
        data.put("todayUsage", completedTodayBookings);
        // 本周设备状态变更次数（近7天）
        long weekStatusChanges = 0;
        try {
            Map<String, Object> statusLogResult = deviceInfoService.pageGlobalStatusLogs(
                    1, 1, null, null, null, null,
                    today.minusDays(7), today, null);
            if (statusLogResult != null && statusLogResult.get("total") != null) {
                weekStatusChanges = ((Number) statusLogResult.get("total")).longValue();
            }
        } catch (Exception ignored) {}
        data.put("weekStatusChanges", weekStatusChanges);

        return ResponseEntity.ok(data);
    }

    /**
     * 获取设备状态分布
     */
    @GetMapping("/device-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDeviceStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("idle", deviceInfoService.countByStatus(0));
        result.put("using", deviceInfoService.countByStatus(1));
        result.put("maintaining", deviceInfoService.countByStatus(2));
        result.put("calibrating", deviceInfoService.countByStatus(3));
        result.put("scrapped", deviceInfoService.countByStatus(4));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取预约趋势数据（近7天真实数据）
     */
    @GetMapping("/booking-trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBookingTrend() {
        List<Map<String, Object>> rawList = bookingOrderService.getBookingTrend();
        Map<String, Integer> dateCountMap = new HashMap<>();
        for (Map<String, Object> row : rawList) {
            Object dateObj = row.get("dateStr");
            if (dateObj == null) {
                dateObj = firstNonNull(row, "datestr", "DATESTR");
            }
            Object cntObj = row.get("cnt");
            if (dateObj == null) {
                cntObj = firstNonNull(row, "CNT", "Cnt");
            }
            if (dateObj != null && cntObj != null) {
                String key = normalizeDateKey(dateObj);
                dateCountMap.put(key, ((Number) cntObj).intValue());
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String dateStr = d.format(fmt);
            dates.add(dateStr);
            counts.add(dateCountMap.getOrDefault(dateStr, 0));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return ResponseEntity.ok(result);
    }

    /**
     * 预约趋势预测（历史数据 + Holt-Winters双指数平滑预测）
     * @param historyDays 历史天数，默认30天
     * @param predictDays 预测天数，默认7天
     */
    @GetMapping("/booking-prediction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBookingPrediction(
            @RequestParam(defaultValue = "30") Integer historyDays,
            @RequestParam(defaultValue = "7") Integer predictDays) {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", predictionService.isPredictionEnabled());
        try {
            List<Map<String, Object>> list = predictionService.predictBookingTrend(historyDays, predictDays);
            // 按 type 拆分为 history 和 forecast 两组，符合 PredictionChart.vue 期望的格式
            List<Map<String, Object>> history = new ArrayList<>();
            List<Map<String, Object>> forecast = new ArrayList<>();
            for (Map<String, Object> item : list) {
                if ("history".equals(item.get("type"))) {
                    history.add(item);
                } else {
                    forecast.add(item);
                }
            }
            result.put("history", history);
            result.put("forecast", forecast);
        } catch (Exception e) {
            result.put("enabled", false);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取设备类型分布（真实数据）
     */
    @GetMapping("/device-type")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDeviceType() {
        List<Map<String, Object>> rawList = deviceInfoService.getDeviceTypeDistribution();
        List<String> categories = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : rawList) {
            Object name = row.get("categoryName");
            if (name == null) {
                name = firstNonNull(row, "categoryname", "CATEGORYNAME");
            }
            Object cnt = row.get("cnt");
            if (cnt == null) {
                cnt = firstNonNull(row, "CNT", "Cnt");
            }
            if (name != null && cnt != null) {
                categories.add(name.toString());
                counts.add(((Number) cnt).intValue());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        result.put("counts", counts);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取预约高峰时段（真实数据）
     */
    @GetMapping("/peak-hours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPeakHours() {
        List<Map<String, Object>> rawList = bookingOrderService.getPeakHours();
        String[] allHours = {"08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"};
        Map<String, Integer> hourCountMap = new HashMap<>();
        for (String h : allHours) {
            hourCountMap.put(h, 0);
        }
        for (Map<String, Object> row : rawList) {
            Object hourObj = row.get("hourStr");
            if (hourObj == null) {
                hourObj = firstNonNull(row, "hourstr", "HOURSTR", "start_time", "startTime");
            }
            Object cntObj = row.get("cnt");
            if (cntObj == null) {
                cntObj = firstNonNull(row, "CNT", "Cnt");
            }
            if (hourObj != null && cntObj != null) {
                // 库中常为 "09:00:00"，图表横轴为 "09:00"，需归一到整点
                String h = normalizePeakHourSlot(hourObj.toString());
                if (hourCountMap.containsKey(h)) {
                    hourCountMap.put(h, hourCountMap.get(h) + ((Number) cntObj).intValue());
                }
            }
        }
        List<String> hours = Arrays.asList(allHours);
        List<Integer> counts = new ArrayList<>();
        for (String h : allHours) {
            counts.add(hourCountMap.getOrDefault(h, 0));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hours", hours);
        result.put("counts", counts);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取平均预约等待时长
     * @param period 统计周期：day-今日，week-本周，month-本月
     */
    @GetMapping("/avg-wait-time")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAvgWaitTime(
            @RequestParam(defaultValue = "week") String period) {
        Double avgWaitTime = bookingOrderService.getAvgWaitTime(period);
        Map<String, Object> result = new HashMap<>();
        result.put("avgWaitTime", avgWaitTime != null ? String.format("%.2f", avgWaitTime) : "0.00");
        result.put("unit", "小时");
        result.put("period", period);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取等待时长趋势数据
     * @param days 天数，默认7天
     */
    @GetMapping("/wait-time-trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getWaitTimeTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> rawList = bookingOrderService.getWaitTimeTrend(days);
        Map<String, Double> dateWaitMap = new HashMap<>();
        for (Map<String, Object> row : rawList) {
            Object dateObj = row.get("dateStr");
            if (dateObj == null) {
                dateObj = firstNonNull(row, "datestr", "DATESTR");
            }
            Object waitObj = row.get("avgWaitHours");
            if (waitObj == null) {
                waitObj = firstNonNull(row, "avgwaithours", "AVGWAIT_HOURS");
            }
            if (dateObj != null && waitObj != null) {
                dateWaitMap.put(normalizeDateKey(dateObj), ((Number) waitObj).doubleValue());
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>();
        List<Double> waitTimes = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String dateStr = d.format(fmt);
            dates.add(dateStr);
            waitTimes.add(dateWaitMap.getOrDefault(dateStr, 0.0));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("waitTimes", waitTimes);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户活跃度统计
     * @param period 统计周期：day/week/month
     * @param days 天数（用于TopN统计）
     * @param limit 返回数量限制
     */
    @GetMapping("/user-activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserActivity(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Map<String, Object>> activityList = sysLogService.getUserActivityStats(days, limit);

        // 转换数据格式
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> item : activityList) {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("userId", item.get("userId"));
            formatted.put("username", item.get("username"));
            formatted.put("realName", item.get("realName"));
            formatted.put("userType", formatUserType(item.get("userType") != null ? item.get("userType").toString() : null));
            formatted.put("loginCount", item.get("loginCount") != null ? item.get("loginCount") : 0);
            formatted.put("bookingCount", item.get("bookingCount") != null ? item.get("bookingCount") : 0);
            formatted.put("totalDuration", item.get("totalDuration") != null ? item.get("totalDuration") : 0);
            resultList.add(formatted);
        }

        // 统计各角色活跃用户数
        Map<String, Integer> roleStats = new HashMap<>();
        roleStats.put("学生", 0);
        roleStats.put("教师", 0);
        roleStats.put("实验室管理员", 0);
        roleStats.put("系统管理员", 0);
        roleStats.put("设备维护人员", 0);
        for (Map<String, Object> item : activityList) {
            String type = formatUserType(item.get("userType") != null ? item.get("userType").toString() : null);
            roleStats.put(type, roleStats.getOrDefault(type, 0) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activities", resultList);
        result.put("roleStats", roleStats);
        result.put("period", period);
        return ResponseEntity.ok(result);
    }

    private String formatUserType(String userType) {
        if (userType == null) return "未知";
        switch (userType) {
            case "STUDENT": return "学生";
            case "TEACHER": return "教师";
            case "LAB_ADMIN": return "实验室管理员";
            case "SYSTEM_ADMIN": return "系统管理员";
            case "MAINTAINER": return "设备维护人员";
            default: return userType;
        }
    }

    /** MySQL/JDBC 下 Map 的 key 大小写不一致时的兜底读取 */
    private static Object firstNonNull(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k)) {
                return row.get(k);
            }
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            for (String k : keys) {
                if (k != null && k.equalsIgnoreCase(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    /** 预约趋势、等待时长趋势：与 Java 侧 yyyy-MM-dd 对齐 */
    private static String normalizeDateKey(Object dateObj) {
        if (dateObj == null) {
            return "";
        }
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate().toString();
        }
        if (dateObj instanceof java.util.Date) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            return fmt.format((java.util.Date) dateObj);
        }
        String s = dateObj.toString().trim();
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return s.substring(0, 10);
        }
        return s;
    }

    /**
     * 高峰时段：start_time 常见为 09:00:00 / 9:00，统一为图表使用的 HH:00（与 08:00..20:00 对齐）
     */
    private static String normalizePeakHourSlot(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }
        if (s.length() == 4 && s.charAt(1) == ':') {
            s = "0" + s;
        }
        String[] parts = s.split(":");
        if (parts.length < 2) {
            return s;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            if (hour < 0 || hour > 23) {
                return s;
            }
            return String.format("%02d:00", hour);
        } catch (NumberFormatException e) {
            return s;
        }
    }

    /**
     * 导出统计报表为Excel
     */
    @GetMapping("/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            statisticsExportService.exportExcel(out);
            String filename = "statistics_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出统计报表为PDF
     */
    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPdf() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            statisticsExportService.exportPdf(out);
            String filename = "statistics_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 系统管理员：用户统计
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> data = new HashMap<>();
        // 与首页 dashboard 一致，避免 QueryWrapper 列名与逻辑删除组合在部分环境下统计异常
        data.put("total", sysUserService.countAllUsers());
        data.put("systemAdmin", countUsersByType("SYSTEM_ADMIN"));
        data.put("labAdmin", countUsersByType("LAB_ADMIN"));
        data.put("teacher", countUsersByType("TEACHER"));
        data.put("student", countUsersByType("STUDENT"));
        data.put("maintainer", countUsersByType("MAINTAINER"));
        data.put("activeWeek", sysLogService.countDistinctLoginUsers(7));
        data.put("activeMonth", sysLogService.countDistinctLoginUsers(30));
        data.put("newMonth", sysUserService.count(
                Wrappers.<SysUser>lambdaQuery()
                        .apply("YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())")));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> trendDates = new ArrayList<>();
        List<Integer> trendCounts = new ArrayList<>();
        Map<String, Integer> trendMap = new HashMap<>();
        for (Map<String, Object> row : sysLogService.loginDistinctTrend(7)) {
            Object d = row.get("dateStr");
            if (d == null) {
                d = firstNonNull(row, "datestr", "DATESTR");
            }
            Object c = row.get("cnt");
            if (c == null) {
                c = firstNonNull(row, "CNT", "Cnt");
            }
            if (d != null && c != null) {
                String key = normalizeDateKey(d);
                trendMap.put(key, ((Number) c).intValue());
            }
        }
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String ds = day.format(fmt);
            trendDates.add(ds);
            trendCounts.add(trendMap.getOrDefault(ds, 0));
        }
        data.put("trendDates", trendDates);
        data.put("trendCounts", trendCounts);
        return ResponseEntity.ok(data);
    }

    /**
     * 用户画像分析（任务书第31-33行：用户画像构建可视化）
     * 返回实验类型分布、技能等级分布、使用频率排行等数据
     */
    @GetMapping("/user-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserProfile() {
        Map<String, Object> result = new HashMap<>();

        // 1. 实验类型分布（饼图数据）
        List<SysUser> allUsers = sysUserService.list(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeleted, 0));
        Map<String, Integer> experimentTypeCount = new HashMap<>();
        Map<String, Integer> skillLevelCount = new HashMap<>();
        Map<String, Integer> userTypeCount = new HashMap<>();
        skillLevelCount.put("初学(1级)", 0);
        skillLevelCount.put("一般(2级)", 0);
        skillLevelCount.put("熟练(3级)", 0);

        for (SysUser user : allUsers) {
            // 统计实验类型
            String expType = user.getExperimentType();
            if (expType != null && !expType.isEmpty()) {
                experimentTypeCount.put(expType, experimentTypeCount.getOrDefault(expType, 0) + 1);
            } else {
                experimentTypeCount.put("未设置", experimentTypeCount.getOrDefault("未设置", 0) + 1);
            }

            // 统计技能等级
            Integer skillLevel = user.getSkillLevel();
            if (skillLevel != null) {
                switch (skillLevel) {
                    case 1:
                        skillLevelCount.put("初学(1级)", skillLevelCount.get("初学(1级)") + 1);
                        break;
                    case 2:
                        skillLevelCount.put("一般(2级)", skillLevelCount.get("一般(2级)") + 1);
                        break;
                    case 3:
                        skillLevelCount.put("熟练(3级)", skillLevelCount.get("熟练(3级)") + 1);
                        break;
                }
            }

            // 统计用户类型
            String userType = formatUserType(user.getUserType());
            userTypeCount.put(userType, userTypeCount.getOrDefault(userType, 0) + 1);
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> experimentTypeData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : experimentTypeCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            experimentTypeData.add(item);
        }

        List<Map<String, Object>> skillLevelData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : skillLevelCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            skillLevelData.add(item);
        }

        result.put("experimentType", experimentTypeData);
        result.put("skillLevel", skillLevelData);
        result.put("userType", userTypeCount);

        // 2. 用户使用频率排行（柱状图数据 - Top 10）
        List<Map<String, Object>> bookingTrend = bookingOrderService.getBookingTrend();
        Map<Long, Integer> userBookingCount = new HashMap<>();
        for (Map<String, Object> row : bookingTrend) {
            // 从预约趋势中统计用户预约次数
        }

        // 直接从booking_order统计用户预约次数
        QueryWrapper<BookingOrder> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("deleted", 0);
        List<BookingOrder> bookings = bookingOrderService.list(bookingWrapper);
        for (BookingOrder bo : bookings) {
            if (bo.getUserId() != null) {
                userBookingCount.put(bo.getUserId(), userBookingCount.getOrDefault(bo.getUserId(), 0) + 1);
            }
        }

        List<Map<String, Object>> userUsageRanking = new ArrayList<>();
        List<Map.Entry<Long, Integer>> sortedUserBookings = new ArrayList<>(userBookingCount.entrySet());
        sortedUserBookings.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedUserBookings) {
            if (rank > 10) break;
            SysUser user = sysUserService.getById(entry.getKey());
            if (user != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", rank++);
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("realName", user.getRealName() != null ? user.getRealName() : user.getUsername());
                item.put("userType", formatUserType(user.getUserType()));
                item.put("experimentType", user.getExperimentType() != null ? user.getExperimentType() : "未设置");
                item.put("bookingCount", entry.getValue());
                item.put("skillLevel", user.getSkillLevel() != null ? user.getSkillLevel() : 1);
                userUsageRanking.add(item);
            }
        }
        result.put("userUsageRanking", userUsageRanking);

        // 3. 用户能力雷达图数据（按用户类型和能力维度聚合）
        Map<String, Map<String, Double>> userTypeAbility = new HashMap<>();
        for (Map<String, Object> item : userUsageRanking) {
            String type = (String) item.get("userType");
            if (!userTypeAbility.containsKey(type)) {
                userTypeAbility.put(type, new HashMap<>());
            }
            Map<String, Double> abilities = userTypeAbility.get(type);
            abilities.put("使用频次", abilities.getOrDefault("使用频次", 0.0) + (Integer) item.get("bookingCount"));
            abilities.put("技能等级", abilities.getOrDefault("技能等级", 0.0) + (Integer) item.get("skillLevel"));
            abilities.put("活跃度", abilities.getOrDefault("活跃度", 0.0) + 1);
        }

        List<Map<String, Object>> radarData = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : userTypeAbility.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            Map<String, Double> abilities = entry.getValue();
            int count = abilities.getOrDefault("活跃度", 0.0).intValue();
            item.put("value", Arrays.asList(
                    Math.min(abilities.getOrDefault("使用频次", 0.0) / 10, 100),
                    Math.round(abilities.getOrDefault("技能等级", 0.0) / count * 10) / 10.0,
                    50
            ));
            radarData.add(item);
        }
        result.put("radarData", radarData);

        // 4. 汇总统计
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", allUsers.size());
        summary.put("activeUsers", sortedUserBookings.size());
        summary.put("avgSkillLevel", calculateAvgSkillLevel(allUsers));
        summary.put("mostPopularExpType", findMostPopularExperimentType(experimentTypeCount));
        result.put("summary", summary);

        return ResponseEntity.ok(result);
    }

    /**
     * 计算平均技能等级
     */
    private double calculateAvgSkillLevel(List<SysUser> users) {
        if (users.isEmpty()) return 0;
        int total = 0;
        int count = 0;
        for (SysUser user : users) {
            if (user.getSkillLevel() != null) {
                total += user.getSkillLevel();
                count++;
            }
        }
        return count > 0 ? Math.round(total * 10.0 / count) / 10.0 : 0;
    }

    /**
     * 找出最受欢迎的实验类型
     */
    private String findMostPopularExperimentType(Map<String, Integer> experimentTypeCount) {
        if (experimentTypeCount.isEmpty()) return "无数据";
        Map.Entry<String, Integer> max = null;
        for (Map.Entry<String, Integer> entry : experimentTypeCount.entrySet()) {
            if (max == null || entry.getValue() > max.getValue()) {
                max = entry;
            }
        }
        return max != null ? max.getKey() : "无数据";
    }

    /**
     * 系统管理员/实验室管理员：设备使用分析
     */
    @GetMapping("/device-usage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDeviceUsageAnalysis() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", deviceInfoService.count());
        data.put("idle", deviceInfoService.countByStatus(0));
        data.put("using", deviceInfoService.countByStatus(1));
        data.put("maintaining", deviceInfoService.countByStatus(2));
        data.put("calibrating", deviceInfoService.countByStatus(3));
        data.put("scrapped", deviceInfoService.countByStatus(4));
        data.put("usageRate", deviceInfoService.calculateUsageRate());
        data.put("avgUsageRate", deviceInfoService.calculateUsageRate());

        List<Map<String, Object>> typeRaw = deviceInfoService.getDeviceTypeDistribution();
        List<String> categories = new ArrayList<>();
        List<Integer> categoryCounts = new ArrayList<>();
        for (Map<String, Object> row : typeRaw) {
            Object name = row.get("categoryName");
            if (name == null) {
                name = firstNonNull(row, "categoryname", "CATEGORYNAME");
            }
            Object cnt = row.get("cnt");
            if (cnt == null) {
                cnt = firstNonNull(row, "CNT", "Cnt");
            }
            if (name != null && cnt != null) {
                categories.add(name.toString());
                categoryCounts.add(((Number) cnt).intValue());
            }
        }
        data.put("categories", categories);
        data.put("categoryCounts", categoryCounts);
        data.put("hotDevices", bookingOrderService.listHotDevices(30, 10));
        data.put("idleDevices", bookingOrderService.listIdleDevicesAnalysis(7, 20));
        return ResponseEntity.ok(data);
    }

    /**
     * 系统管理员/实验室管理员：预约全量分析
     */
    @GetMapping("/booking-analysis")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> getBookingAnalysis() {
        Map<String, Object> data = new HashMap<>();
        data.put("weekTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(7).atStartOfDay())));
        data.put("todayTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .apply("DATE(create_time) = CURDATE()")));
        data.put("monthTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(30).atStartOfDay())));
        long totalNonDeleted = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0));
        long approved = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 1));
        data.put("successRate", totalNonDeleted > 0 ? Math.round(approved * 1000.0 / totalNonDeleted) / 10.0 : 0);
        long cancelled = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 4));
        data.put("cancelRate", totalNonDeleted > 0 ? Math.round(cancelled * 1000.0 / totalNonDeleted) / 10.0 : 0);
        data.put("pending", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 0)));
        data.put("approved", approved);
        data.put("rejected", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 2)));
        data.put("completed", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 3)));
        data.put("cancelled", cancelled);
        Double avgAudit = bookingOrderService.getAvgWaitTime("week");
        data.put("avgAuditHours", avgAudit != null ? avgAudit : 0);
        data.put("avgWaitHours", avgAudit != null ? avgAudit : 0);

        List<Map<String, Object>> rawTrend = bookingOrderService.getBookingTrend();
        Map<String, Integer> cntByDate = new HashMap<>();
        for (Map<String, Object> row : rawTrend) {
            Object d = row.get("dateStr");
            if (d == null) {
                d = firstNonNull(row, "datestr", "DATESTR");
            }
            Object c = row.get("cnt");
            if (c == null) {
                c = firstNonNull(row, "CNT", "Cnt");
            }
            if (d != null && c != null) {
                cntByDate.put(normalizeDateKey(d), ((Number) c).intValue());
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String ds = day.format(fmt);
            dates.add(ds);
            counts.add(cntByDate.getOrDefault(ds, 0));
        }
        data.put("dates", dates);
        data.put("counts", counts);

        Map<String, Object> peakBody = getPeakHours().getBody();
        if (peakBody != null) {
            data.put("peakHours", peakBody.get("hours"));
            data.put("peakCounts", peakBody.get("counts"));
        }
        return ResponseEntity.ok(data);
    }

    /**
     * 管理员/实验室管理员/维护人员：维护统计
     */
    @GetMapping("/maintenance")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list')")
    public ResponseEntity<Map<String, Object>> getMaintenanceStatistics() {
        Map<String, Object> data = new HashMap<>();
        data.put("weekCount", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(7).atStartOfDay())));
        data.put("monthCount", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(30).atStartOfDay())));
        data.put("pending", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 0)));
        data.put("processing", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 1)));

        List<RepairOrder> monthDone = repairOrderService.list(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 2)
                .ge("handle_end_time", LocalDate.now().minusDays(30).atStartOfDay()));
        double avgH = 0;
        int n = 0;
        double monthCost = 0;
        for (RepairOrder r : monthDone) {
            if (r.getHandleStartTime() != null && r.getHandleEndTime() != null) {
                avgH += java.time.Duration.between(r.getHandleStartTime(), r.getHandleEndTime()).toMinutes() / 60.0;
                n++;
            }
            if (r.getRepairCost() != null) {
                monthCost += r.getRepairCost();
            }
        }
        data.put("avgRepairHours", n > 0 ? Math.round(avgH * 10.0 / n) / 10.0 : 0);
        data.put("monthCost", Math.round(monthCost * 100.0) / 100.0);

        // 校准统计（近7天和近30天）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime monthAgo = now.minusDays(30).toLocalDate().atStartOfDay();
        data.put("weekCalibration", calibrationRecordService.count(
                new QueryWrapper<CalibrationRecord>().eq("deleted", 0)
                        .ge("calibration_date", weekAgo)));
        data.put("monthCalibration", calibrationRecordService.count(
                new QueryWrapper<CalibrationRecord>().eq("deleted", 0)
                        .ge("calibration_date", monthAgo)));

        List<Map<String, Object>> faultTypes = new ArrayList<>();
        Map<String, Long> fc = new HashMap<>();
        List<RepairOrder> recent = repairOrderService.list(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(30).atStartOfDay()).last("LIMIT 200"));
        for (RepairOrder r : recent) {
            String key = r.getFaultDescription() != null && r.getFaultDescription().length() > 0
                    ? r.getFaultDescription().substring(0, Math.min(20, r.getFaultDescription().length()))
                    : "其他";
            fc.put(key, fc.getOrDefault(key, 0L) + 1);
        }
        for (Map.Entry<String, Long> e : fc.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("count", e.getValue().intValue());
            faultTypes.add(m);
        }
        data.put("faultTypes", faultTypes);

        // 近7天按「完成日」汇总维修时长与成本（与卡片数据一致，供趋势图使用）
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate weekStart = LocalDate.now().minusDays(6);
        LocalDateTime rangeStart = weekStart.atStartOfDay();
        LocalDateTime rangeEnd = LocalDate.now().atTime(23, 59, 59);
        List<RepairOrder> doneInWeek = repairOrderService.list(
                new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 2)
                        .isNotNull("handle_end_time")
                        .ge("handle_end_time", rangeStart)
                        .le("handle_end_time", rangeEnd));
        Map<String, Double> hoursByDay = new HashMap<>();
        Map<String, Double> costByDay = new HashMap<>();
        for (RepairOrder r : doneInWeek) {
            String ds = r.getHandleEndTime().toLocalDate().format(fmt);
            if (r.getHandleStartTime() != null) {
                double h = Duration.between(r.getHandleStartTime(), r.getHandleEndTime()).toMinutes() / 60.0;
                if (h >= 0) {
                    hoursByDay.merge(ds, h, Double::sum);
                }
            }
            if (r.getRepairCost() != null) {
                costByDay.merge(ds, r.getRepairCost(), Double::sum);
            }
        }
        List<String> repairDates = new ArrayList<>();
        List<Double> repairDurations = new ArrayList<>();
        List<String> costDates = new ArrayList<>();
        List<Double> costAmounts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String ds = day.format(fmt);
            repairDates.add(ds);
            costDates.add(ds);
            repairDurations.add(Math.round(hoursByDay.getOrDefault(ds, 0.0) * 10.0) / 10.0);
            costAmounts.add(Math.round(costByDay.getOrDefault(ds, 0.0) * 100.0) / 100.0);
        }
        data.put("repairDates", repairDates);
        data.put("repairDurations", repairDurations);
        data.put("costDates", costDates);
        data.put("costAmounts", costAmounts);
        return ResponseEntity.ok(data);
    }

    /**
     * 设备维护人员：个人维修统计
     * 支持设备维护人员和实验室管理员访问
     */
    @GetMapping("/maintainer-stats")
    @PreAuthorize("hasAnyRole('MAINTAINER','LAB_ADMIN','SYSTEM_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list')")
    public ResponseEntity<Map<String, Object>> getMaintainerStats() {
        Map<String, Object> data = new HashMap<>();
        Long userId = null;
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof SysUser) {
                    userId = ((SysUser) principal).getId();
                }
                // 生产环境中 principal 常为 Spring Security 的 User，需通过用户名反查业务用户ID
                if (userId == null) {
                    String username = authentication.getName();
                    if (username != null && !username.isEmpty()) {
                        // 统一走用户服务，避免 deleted 字段在部分库中为 NULL 时被误过滤导致拿不到当前用户
                        SysUser current = sysUserService.getByUsername(username);
                        if (current != null) {
                            userId = current.getId();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 若无法识别当前用户，返回空统计，避免把 handler_id=null 当成筛选条件导致全0
        if (userId == null) {
            data.put("processing", 0);
            data.put("monthCompleted", 0);
            data.put("weekCompleted", 0);
            data.put("monthCost", 0);
            data.put("avgRepairHours", 0);
            data.put("faultTypes", new ArrayList<>());
            data.put("monthCalibration", 0);
            return ResponseEntity.ok(data);
        }

        // 本人处理中的工单
        data.put("processing", repairOrderService.count(
                new QueryWrapper<RepairOrder>().eq("deleted", 0)
                        .eq("status", 1).eq("handler_id", userId)));
        // 本人本月完成的工单
        data.put("monthCompleted", repairOrderService.count(
                new QueryWrapper<RepairOrder>().eq("deleted", 0)
                        .eq("status", 2).eq("handler_id", userId)
                        .ge("handle_end_time", LocalDate.now().minusDays(30).atStartOfDay())));
        // 本人本周完成的工单
        data.put("weekCompleted", repairOrderService.count(
                new QueryWrapper<RepairOrder>().eq("deleted", 0)
                        .eq("status", 2).eq("handler_id", userId)
                        .ge("handle_end_time", LocalDate.now().minusDays(7).atStartOfDay())));
        // 本人本月维修成本
        List<RepairOrder> monthDone = repairOrderService.list(
                new QueryWrapper<RepairOrder>().eq("deleted", 0)
                        .eq("status", 2).eq("handler_id", userId)
                        .ge("handle_end_time", LocalDate.now().minusDays(30).atStartOfDay()));
        double monthCost = 0;
        for (RepairOrder r : monthDone) {
            if (r.getRepairCost() != null) monthCost += r.getRepairCost();
        }
        data.put("monthCost", Math.round(monthCost * 100.0) / 100.0);
        // 平均维修时长
        double totalHours = 0; int cnt = 0;
        for (RepairOrder r : monthDone) {
            if (r.getHandleStartTime() != null && r.getHandleEndTime() != null) {
                totalHours += java.time.Duration.between(r.getHandleStartTime(), r.getHandleEndTime()).toMinutes() / 60.0;
                cnt++;
            }
        }
        data.put("avgRepairHours", cnt > 0 ? Math.round(totalHours * 10.0 / cnt) / 10.0 : 0);
        // 本人故障类型分布（口径与“维修记录查询”一致：按维修完成时间统计近30天已完成工单）
        List<Map<String, Object>> faultTypes = new ArrayList<>();
        Map<String, Long> fc = new HashMap<>();
        List<RepairOrder> recent = repairOrderService.list(
                new QueryWrapper<RepairOrder>().eq("deleted", 0)
                        .eq("handler_id", userId)
                        .eq("status", 2)
                        .ge("handle_end_time", LocalDate.now().minusDays(30).atStartOfDay()).last("LIMIT 200"));
        for (RepairOrder r : recent) {
            String key = r.getFaultDescription() != null && r.getFaultDescription().length() > 0
                    ? r.getFaultDescription().substring(0, Math.min(20, r.getFaultDescription().length()))
                    : "其他";
            fc.put(key, fc.getOrDefault(key, 0L) + 1);
        }
        for (Map.Entry<String, Long> e : fc.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("count", e.getValue().intValue());
            faultTypes.add(m);
        }
        data.put("faultTypes", faultTypes);
        // 本人校准记录数
        data.put("monthCalibration", calibrationRecordService.count(
                new QueryWrapper<CalibrationRecord>().eq("deleted", 0)
                        .ge("calibration_date", LocalDate.now().minusDays(30).atStartOfDay())));
        return ResponseEntity.ok(data);
    }

    /**
     * 校准达标率统计（任务书：设备数据分析 → 校准达标率，生成柱状图、折线图）
     * @param months 统计月数，默认6个月
     */
    @GetMapping("/calibration-rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCalibrationRate(
            @RequestParam(defaultValue = "6") Integer months) {
        Map<String, Object> result = new HashMap<>();

        // 查询最近N个月的校准记录
        LocalDate startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();

        // 统计总校准次数和达标次数
        List<CalibrationRecord> records = calibrationRecordService.list(
                new QueryWrapper<CalibrationRecord>()
                        .ge("calibration_date", startDate.atStartOfDay())
                        .le("calibration_date", endDate.atTime(23, 59, 59))
                        .eq("deleted", 0)
                        .orderByDesc("calibration_date")
        );

        int totalCount = records.size();
        int passedCount = 0;
        Map<String, int[]> monthlyData = new LinkedHashMap<>();

        // 初始化每月数据
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter monthLabelFmt = DateTimeFormatter.ofPattern("MM月");
        for (int i = months - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusMonths(i);
            String monthKey = d.format(monthFmt);
            monthlyData.put(monthKey, new int[]{0, 0}); // [total, passed]
        }

        // 统计每月数据
        for (CalibrationRecord record : records) {
            if (record.getCalibrationDate() == null) continue;

            String monthKey = record.getCalibrationDate().toLocalDate().format(monthFmt);
            if (monthlyData.containsKey(monthKey)) {
                int[] data = monthlyData.get(monthKey);
                data[0]++; // 总次数
                if (record.getResult() != null && record.getResult() == 1) {
                    data[1]++; // 达标次数
                    passedCount++;
                }
            }
        }

        // 转换为前端需要的格式
        List<String> monthLabels = new ArrayList<>();
        List<Integer> totalList = new ArrayList<>();
        List<Integer> passedList = new ArrayList<>();
        List<Double> rateList = new ArrayList<>();

        for (Map.Entry<String, int[]> entry : monthlyData.entrySet()) {
            String monthKey = entry.getKey();
            LocalDate monthDate = LocalDate.parse(monthKey + "-01");
            monthLabels.add(monthDate.format(monthLabelFmt));
            int total = entry.getValue()[0];
            int passed = entry.getValue()[1];
            totalList.add(total);
            passedList.add(passed);
            double rate = total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0;
            rateList.add(rate);
        }

        // 计算总体达标率
        double overallRate = totalCount > 0 ? Math.round(passedCount * 1000.0 / totalCount) / 10.0 : 0.0;

        result.put("months", monthLabels);
        result.put("totalList", totalList);
        result.put("passedList", passedList);
        result.put("rateList", rateList);
        result.put("total", totalCount);
        result.put("passed", passedCount);
        result.put("rate", overallRate);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/report/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list')")
    public ResponseEntity<byte[]> exportAdminReport(
            @RequestParam String type,
            @RequestParam(defaultValue = "excel") String format) {
        try {
            if (!"excel".equalsIgnoreCase(format) && !"pdf".equalsIgnoreCase(format)) {
                return ResponseEntity.badRequest().build();
            }
            if ("pdf".equalsIgnoreCase(format)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                statisticsExportService.exportPdfByType(out, type);
                String filename = "report_" + type + "_" + LocalDate.now() + ".pdf";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                                + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(out.toByteArray());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("报表");
                int r = 0;
                if ("user".equals(type)) {
                    Map<String, Object> u = getUserStatistics().getBody();
                    row(sheet, r++, "用户总数", u != null ? u.get("total") : "");
                    row(sheet, r++, "系统管理员", u != null ? u.get("systemAdmin") : "");
                    row(sheet, r++, "实验室管理员", u != null ? u.get("labAdmin") : "");
                    row(sheet, r++, "教师", u != null ? u.get("teacher") : "");
                    row(sheet, r++, "学生", u != null ? u.get("student") : "");
                    row(sheet, r++, "设备维护人员", u != null ? u.get("maintainer") : "");
                    row(sheet, r++, "本周活跃(登录去重)", u != null ? u.get("activeWeek") : "");
                    row(sheet, r++, "本月活跃(登录去重)", u != null ? u.get("activeMonth") : "");
                    row(sheet, r++, "新增用户(本月)", u != null ? u.get("newMonth") : "");
                } else if ("device".equals(type)) {
                    Map<String, Object> d = getDeviceUsageAnalysis().getBody();
                    row(sheet, r++, "设备总数", d != null ? d.get("total") : "");
                    row(sheet, r++, "空闲设备", d != null ? d.get("idle") : "");
                    row(sheet, r++, "使用中", d != null ? d.get("using") : "");
                    row(sheet, r++, "维修中", d != null ? d.get("maintaining") : "");
                    row(sheet, r++, "校准中", d != null ? d.get("calibrating") : "");
                    row(sheet, r++, "已报废", d != null ? d.get("scrapped") : "");
                    row(sheet, r++, "设备利用率(%)", d != null ? d.get("usageRate") : "");
                    row(sheet, r++, "平均利用率(%)", d != null ? d.get("avgUsageRate") : "");
                } else if ("booking".equals(type)) {
                    Map<String, Object> b = getBookingAnalysis().getBody();
                    row(sheet, r++, "本周预约总量", b != null ? b.get("weekTotal") : "");
                    row(sheet, r++, "今日预约", b != null ? b.get("todayTotal") : "");
                    row(sheet, r++, "预约成功率(%)", b != null ? b.get("successRate") : "");
                    row(sheet, r++, "预约取消率(%)", b != null ? b.get("cancelRate") : "");
                    row(sheet, r++, "待审核", b != null ? b.get("pending") : "");
                    row(sheet, r++, "平均审核时长(h)", b != null ? b.get("avgAuditHours") : "");
                    row(sheet, r++, "平均等待时长(h)", b != null ? b.get("avgWaitHours") : "");
                    row(sheet, r++, "本月预约总量", b != null ? b.get("monthTotal") : "");
                } else if ("maintenance".equals(type) || "fault".equals(type)) {
                    Map<String, Object> m = getMaintenanceStatistics().getBody();
                    row(sheet, r++, "本周维护次数", m != null ? m.get("weekCount") : "");
                    row(sheet, r++, "本月维护次数", m != null ? m.get("monthCount") : "");
                    row(sheet, r++, "待处理工单", m != null ? m.get("pending") : "");
                    row(sheet, r++, "处理中工单", m != null ? m.get("processing") : "");
                    row(sheet, r++, "平均维修时长(h)", m != null ? m.get("avgRepairHours") : "");
                    row(sheet, r++, "本月维修成本(元)", m != null ? m.get("monthCost") : "");
                    row(sheet, r++, "本周校准次数", m != null ? m.get("weekCalibration") : "");
                    row(sheet, r++, "本月校准次数", m != null ? m.get("monthCalibration") : "");
                    List<Map<String, Object>> faultTypes = m != null ? (List<Map<String, Object>>) m.get("faultTypes") : null;
                    if (faultTypes != null && !faultTypes.isEmpty()) {
                        r++;
                        row(sheet, r++, "故障类型", "数量");
                        for (Map<String, Object> ft : faultTypes) {
                            row(sheet, r++, ft.get("name"), ft.get("count"));
                        }
                    }
                } else if ("calibration".equals(type)) {
                    Map<String, Object> m = getCalibrationRate(6).getBody();
                    row(sheet, r++, "应校准总数", m != null ? m.get("total") : "");
                    row(sheet, r++, "已完成", m != null ? m.get("passed") : "");
                    row(sheet, r++, "达标率(%)", m != null ? m.get("rate") : "");
                    r++;
                    row(sheet, r++, "--- 月度校准数据（近6个月） ---", "");
                    List<String> months = m != null ? (List<String>) m.get("months") : null;
                    List<Integer> totalList = m != null ? (List<Integer>) m.get("totalList") : null;
                    List<Integer> passedList = m != null ? (List<Integer>) m.get("passedList") : null;
                    List<Double> rateList = m != null ? (List<Double>) m.get("rateList") : null;
                    if (months != null && !months.isEmpty()) {
                        Row headerRow = sheet.createRow(r++);
                        headerRow.createCell(0).setCellValue("月份");
                        headerRow.createCell(1).setCellValue("应校准数");
                        headerRow.createCell(2).setCellValue("已完成");
                        headerRow.createCell(3).setCellValue("达标率(%)");
                        for (int i = 0; i < months.size(); i++) {
                            Row dataRow = sheet.createRow(r++);
                            dataRow.createCell(0).setCellValue(months.get(i));
                            dataRow.createCell(1).setCellValue(totalList != null ? String.valueOf(totalList.get(i)) : "");
                            dataRow.createCell(2).setCellValue(passedList != null ? String.valueOf(passedList.get(i)) : "");
                            dataRow.createCell(3).setCellValue(rateList != null ? String.valueOf(rateList.get(i)) : "");
                        }
                    }
                } else if ("user-profile".equals(type)) {
                    Map<String, Object> profile = getUserProfile().getBody();
                    Map<String, Object> summary = profile != null ? (Map<String, Object>) profile.get("summary") : null;
                    row(sheet, r++, "--- 用户画像汇总 ---", "");
                    row(sheet, r++, "用户总数", summary != null ? summary.get("totalUsers") : "");
                    row(sheet, r++, "活跃用户", summary != null ? summary.get("activeUsers") : "");
                    row(sheet, r++, "平均技能等级", summary != null ? summary.get("avgSkillLevel") : "");
                    row(sheet, r++, "最受欢迎实验类型", summary != null ? summary.get("mostPopularExpType") : "");
                    
                    r++;
                    row(sheet, r++, "--- 实验类型分布 ---", "");
                    List<Map<String, Object>> expTypes = profile != null ? (List<Map<String, Object>>) profile.get("experimentType") : null;
                    if (expTypes != null && !expTypes.isEmpty()) {
                        row(sheet, r++, "实验类型", "数量");
                        for (Map<String, Object> et : expTypes) {
                            row(sheet, r++, et.get("name"), et.get("value"));
                        }
                    }
                    
                    r++;
                    row(sheet, r++, "--- 技能等级分布 ---", "");
                    List<Map<String, Object>> skillLevels = profile != null ? (List<Map<String, Object>>) profile.get("skillLevel") : null;
                    if (skillLevels != null && !skillLevels.isEmpty()) {
                        row(sheet, r++, "技能等级", "数量");
                        for (Map<String, Object> sl : skillLevels) {
                            row(sheet, r++, sl.get("name"), sl.get("value"));
                        }
                    }
                    
                    r++;
                    row(sheet, r++, "--- 用户使用频率排行 TOP 10 ---", "");
                    List<Map<String, Object>> rankings = profile != null ? (List<Map<String, Object>>) profile.get("userUsageRanking") : null;
                    if (rankings != null && !rankings.isEmpty()) {
                        Row headerRow = sheet.createRow(r++);
                        headerRow.createCell(0).setCellValue("排名");
                        headerRow.createCell(1).setCellValue("用户名");
                        headerRow.createCell(2).setCellValue("真实姓名");
                        headerRow.createCell(3).setCellValue("用户类型");
                        headerRow.createCell(4).setCellValue("实验类型");
                        headerRow.createCell(5).setCellValue("预约次数");
                        headerRow.createCell(6).setCellValue("技能等级");
                        for (Map<String, Object> rk : rankings) {
                            Row dataRow = sheet.createRow(r++);
                            dataRow.createCell(0).setCellValue(String.valueOf(rk.get("rank")));
                            dataRow.createCell(1).setCellValue(String.valueOf(rk.get("username")));
                            dataRow.createCell(2).setCellValue(String.valueOf(rk.get("realName")));
                            dataRow.createCell(3).setCellValue(String.valueOf(rk.get("userType")));
                            dataRow.createCell(4).setCellValue(String.valueOf(rk.get("experimentType")));
                            dataRow.createCell(5).setCellValue(String.valueOf(rk.get("bookingCount")));
                            dataRow.createCell(6).setCellValue(String.valueOf(rk.get("skillLevel")));
                        }
                    }
                } else {
                    row(sheet, r++, "类型", type);
                }
                wb.write(out);
            }
            String filename = "report_" + type + "_" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                            + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private long countUsersByType(String userType) {
        return sysUserService.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserType, userType));
    }

    private void exportMaintainerPdf(ByteArrayOutputStream out) throws Exception {
        Map<String, Object> m = getMaintainerStats().getBody();
        
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
        document.open();
        
        com.lowagie.text.pdf.BaseFont bf = loadChineseBaseFontForPdf();
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(bf, 16, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font metaFont = new com.lowagie.text.Font(bf, 10);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(bf, 11, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font cellFont = new com.lowagie.text.Font(bf, 10);
        
        document.add(new com.lowagie.text.Paragraph("实验室设备预约系统 - 数据统计报表", titleFont));
        document.add(new com.lowagie.text.Paragraph("导出时间：" + LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), metaFont));
        document.add(new com.lowagie.text.Paragraph("报表类型：维护统计", metaFont));
        document.add(com.lowagie.text.Chunk.NEWLINE);
        
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
        table.setWidthPercentage(100);
        
        addPdfRow(table, "统计项", "数值", headerFont);
        addPdfRow(table, "处理中工单", m != null ? String.valueOf(m.get("processing")) : "", cellFont);
        addPdfRow(table, "本周完成", m != null ? String.valueOf(m.get("weekCompleted")) : "", cellFont);
        addPdfRow(table, "本月完成", m != null ? String.valueOf(m.get("monthCompleted")) : "", cellFont);
        addPdfRow(table, "平均维修时长(h)", m != null ? String.valueOf(m.get("avgRepairHours")) : "", cellFont);
        addPdfRow(table, "本月维修成本(元)", m != null ? String.valueOf(m.get("monthCost")) : "", cellFont);
        addPdfRow(table, "本月校准次数", m != null ? String.valueOf(m.get("monthCalibration")) : "", cellFont);
        
        List<Map<String, Object>> faultTypes = m != null ? (List<Map<String, Object>>) m.get("faultTypes") : null;
        if (faultTypes != null && !faultTypes.isEmpty()) {
            addPdfRow(table, "", "", cellFont);
            addPdfRow(table, "故障类型分布", "", headerFont);
            for (Map<String, Object> ft : faultTypes) {
                addPdfRow(table, String.valueOf(ft.get("name")), String.valueOf(ft.get("count")), cellFont);
            }
        }
        
        document.add(table);
        document.close();
    }
    
    private void addPdfRow(com.lowagie.text.pdf.PdfPTable table, String col1, String col2, com.lowagie.text.Font font) {
        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col1, font)));
        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(col2, font)));
    }
    
    private com.lowagie.text.pdf.BaseFont loadChineseBaseFontForPdf() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansSC-Regular.otf")) {
            if (is != null) {
                byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);
                return com.lowagie.text.pdf.BaseFont.createFont("NotoSansSC-Regular.otf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED, true, null, bytes);
            }
        }
        String[] candidates = {"C:/Windows/Fonts/msyh.ttc,0", "C:/Windows/Fonts/simsun.ttc,0"};
        for (String path : candidates) {
            try { return com.lowagie.text.pdf.BaseFont.createFont(path, com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED); } catch (Exception ignored) {}
        }
        return com.lowagie.text.pdf.BaseFont.createFont(com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.WINANSI, com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
    }

    private static void row(Sheet sheet, int idx, Object a, Object b) {
        Row row = sheet.createRow(idx);
        row.createCell(0).setCellValue(a != null ? a.toString() : "");
        row.createCell(1).setCellValue(b != null ? b.toString() : "");
    }
}
