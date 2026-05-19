package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.BookingOrderMapper;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.SysConfigService;
import com.lab.reservation.service.SysMessageService;
import com.lab.reservation.service.SysUserService;
import com.lab.reservation.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预约Service实现类
 */
@Service
public class BookingOrderServiceImpl extends ServiceImpl<BookingOrderMapper, BookingOrder> implements BookingOrderService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrderServiceImpl.class);

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysMessageService sysMessageService;

    private static final String KEY_AUDIT_MODE = "booking.audit.mode";
    private static final String KEY_AUTO_CONDITIONS = "booking.audit.autoConditions";
    private static final String KEY_GLOBAL_BASIC = "booking.global.basic";
    private static final String KEY_GLOBAL_TIME = "booking.global.timeRange";
    private static final String KEY_GLOBAL_ROLE = "booking.global.roleLimits";
    private static final String KEY_GLOBAL_ROLE_LEGACY = "booking.global.role";

    @Override
    @Transactional
    public boolean createBooking(BookingOrder bookingOrder) {
        log.info("[createBooking] START - bookingDate={}, startTime={}, endTime={}",
                bookingOrder.getBookingDate(), bookingOrder.getStartTime(), bookingOrder.getEndTime());

        // 验证预约日期不能是过去的时间
        if (bookingOrder.getBookingDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime bookingDateTime = bookingOrder.getBookingDate();
            LocalDate bookingDateOnly = bookingDateTime.toLocalDate();
            if (bookingDateOnly.isBefore(LocalDate.now())) {
                throw new RuntimeException("预约日期不能选择过去的时间");
            }
            // 今天不能选择已过期的时间段
            if (bookingDateOnly.isEqual(LocalDate.now())) {
                LocalTime nowTime = now.toLocalTime();
                String startTimeStr = bookingOrder.getStartTime();
                if (startTimeStr != null && !startTimeStr.isEmpty()) {
                    try {
                        String[] parts = startTimeStr.split(":");
                        int hour = Integer.parseInt(parts[0]);
                        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                        LocalTime startTime = LocalTime.of(hour, minute);
                        if (!startTime.isAfter(nowTime)) {
                            throw new RuntimeException("不能预约已过去的时间段");
                        }
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("时间格式错误");
                    }
                }
            }
        }

        try {
            validateGlobalBookingConstraints(bookingOrder, null);
            log.info("[createBooking] validateGlobalBookingConstraints PASSED");
        } catch (RuntimeException e) {
            log.warn("[createBooking] validateGlobalBookingConstraints REJECTED: {}", e.getMessage());
            throw e;
        }

        // 检测时间冲突（AUTO 模式下仅在勾选 noConflict 时强制）
        if (shouldEnforceNoConflictConstraint() && checkConflict(bookingOrder.getDeviceId(), bookingOrder.getBookingDate(),
                bookingOrder.getStartTime(), bookingOrder.getEndTime())) {
            throw new RuntimeException("该时间段已被预约");
        }

        // 爽约超限拦截：累加爽约次数达到阈值后禁止预约
        if (bookingOrder.getUserId() != null) {
            SysUser user = sysUserService.getById(bookingOrder.getUserId());
            if (user != null) {
                int missed = user.getMissedCount() == null ? 0 : user.getMissedCount();
                int threshold = intFromMap(mergedBasicRule(), "noShowThreshold", 3);
                if (missed >= threshold) {
                    throw new RuntimeException("您已累计爽约" + missed + "次，已被禁止预约，请联系管理员");
                }
            }
        }

        // 生成订单号
        bookingOrder.setOrderNo(CommonUtil.generateOrderNo());

        // 读取审核模式配置
        String auditMode = sysConfigService.getConfigValue(KEY_AUDIT_MODE);
        List<String> conditions = sysConfigService.getJsonArray(KEY_AUTO_CONDITIONS);
        log.info("[createBooking] auditMode={}, autoConditions={}", auditMode, conditions);

        boolean isAuto = "AUTO".equalsIgnoreCase(auditMode);
        // 默认：待审核（人工审核模式或自动审核条件未全部满足时）
        int finalStatus = 0;

        // 学生强制审核：无论自动审核模式如何，只要学生NeedAudit=true就必须待审
        boolean studentForcePending = false;
        if (isAuto) {
            List<String> failures = new ArrayList<>();

            if (conditions.contains("noConflict")) {
                // 冲突已在前面检测，此条件已满足
            }
            if (conditions.contains("withinLimit")) {
                if (!checkWithinUserDailyLimit(bookingOrder)) {
                    failures.add("超出每日预约次数限制");
                }
            }
            if (conditions.contains("advanceTime")) {
                if (!checkAdvanceTime(bookingOrder)) {
                    failures.add("不符合提前申请时间要求");
                }
            }

            // 全部勾选的条件都满足时，自动通过（但学生强制审核例外）
            if (failures.isEmpty()) {
                // 检查学生是否需要强制审核
                SysUser applicant = sysUserService.getById(bookingOrder.getUserId());
                String ut = applicant != null && applicant.getUserType() != null ? applicant.getUserType() : "";
                if ("STUDENT".equalsIgnoreCase(ut)) {
                    Map<String, Object> roleLimits = mergedRoleLimits();
                    boolean needAudit = parseBooleanFlag(roleLimits.get("studentNeedAudit"));
                    if (needAudit) {
                        studentForcePending = true;
                        log.info("[createBooking] AUTO mode: student booking, studentNeedAudit=true, force pending");
                    }
                }
                if (!studentForcePending) {
                    finalStatus = 1;
                    log.info("[createBooking] AUTO mode: all conditions met, status=1 (approved)");
                }
            } else {
                log.info("[createBooking] AUTO mode: conditions not met, failures={}, status=0 (pending)");
            }
        } else {
            log.info("[createBooking] MANUAL mode (isAuto=false), status=0 (pending)");
        }

        bookingOrder.setStatus(finalStatus);
        boolean saved = save(bookingOrder);

        // 审核通过时不立即更新设备状态，等待用户签到时再更新
        // if (saved && finalStatus == 1 && bookingOrder.getDeviceId() != null) {
        //     deviceInfoService.updateDeviceStatus(bookingOrder.getDeviceId(), 1);
        // }

        // 待人工审核：通知系统管理员、实验室管理员；学生发起的预约另通知全体教师（与审核列表职责一致）
        if (saved && finalStatus == 0) {
            notifyAuditorsNewPendingBooking(bookingOrder);
        }

        return saved;
    }

    @Override
    public void validateGlobalBookingConstraints(BookingOrder order, Long excludeBookingId) {
        log.info("[validateConstraints] bookingDate={}, startTime={}, endTime={}, userId={}, deviceId={}",
                order.getBookingDate(), order.getStartTime(), order.getEndTime(),
                order.getUserId(), order.getDeviceId());

        if (order.getDeviceId() == null) {
            throw new RuntimeException("设备不能为空");
        }
        if (order.getBookingDate() == null) {
            throw new RuntimeException("预约日期不能为空");
        }
        if (order.getUserId() == null) {
            throw new RuntimeException("用户未登录");
        }
        String st = order.getStartTime();
        String et = order.getEndTime();
        if (st == null || st.isEmpty() || et == null || et.isEmpty()) {
            throw new RuntimeException("请选择预约时段");
        }
        LocalTime bookingStartT;
        LocalTime bookingEndT;
        try {
            bookingStartT = parseTimeParts(st);
            bookingEndT = parseTimeParts(et);
        } catch (Exception e) {
            throw new RuntimeException("时间格式错误");
        }
        if (!bookingEndT.isAfter(bookingStartT)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }

        Map<String, Object> basic = mergedBasicRule();
        log.info("[validateConstraints] basic={}", basic);
        int minAdvanceHours = intFromMap(basic, "minAdvanceHours", 24);
        int maxBookingHours = intFromMap(basic, "maxBookingHours", 4);
        int maxPerDevice = intFromMap(basic, "maxBookingsPerDevicePerDay", 3);
        int maxPerUser = intFromMap(basic, "maxBookingsPerUserPerDay", 5);

        LocalDateTime bookingStartDt = order.getBookingDate().toLocalDate().atTime(bookingStartT);
        LocalDateTime now = LocalDateTime.now();
        log.info("[validateConstraints] bookingStartDt={}, now={}, minAdvanceHours={}", bookingStartDt, now, minAdvanceHours);

        boolean enforceAdvanceTime = shouldEnforceAdvanceTimeConstraint();
        if (enforceAdvanceTime && bookingStartDt.isBefore(now.plusHours(minAdvanceHours))) {
            log.warn("[validateConstraints] REJECTED: bookingStartDt={} is before now+minAdvanceHours={}",
                    bookingStartDt, now.plusHours(minAdvanceHours));
            throw new RuntimeException("距预约开始时间不足 " + minAdvanceHours + " 小时，不符合提前申请要求");
        }
        log.info("[validateConstraints] advance time check {}, enforce={} ",
                enforceAdvanceTime ? "PASSED" : "SKIPPED", enforceAdvanceTime);

        long bookingMinutes = ChronoUnit.MINUTES.between(bookingStartT, bookingEndT);

        boolean enforceWithinLimit = shouldEnforceWithinLimitConstraint();
        if (enforceWithinLimit && bookingMinutes > maxBookingHours * 60L) {
            throw new RuntimeException("单次预约时长不能超过 " + maxBookingHours + " 小时");
        }

        LocalDate d = order.getBookingDate().toLocalDate();
        if (enforceWithinLimit) {
            long userCnt = countUserBookingsOnDate(order.getUserId(), d, excludeBookingId);
            if (userCnt >= maxPerUser) {
                throw new RuntimeException("已达到每人每日最大预约次数（" + maxPerUser + " 次）");
            }
            long devCnt = countDeviceBookingsOnDate(order.getDeviceId(), d, excludeBookingId);
            if (devCnt >= maxPerDevice) {
                throw new RuntimeException("该设备当日预约次数已达上限（" + maxPerDevice + " 次）");
            }
        }

        Map<String, Object> tr = mergedTimeRange();
        @SuppressWarnings("unchecked")
        List<Object> weekdays = (List<Object>) tr.get("weekdays");
        if (weekdays == null || weekdays.isEmpty()) {
            throw new RuntimeException("系统未配置可预约星期，无法提交");
        }
        int dow = d.getDayOfWeek().getValue();
        if (!weekdayAllowed(weekdays, dow)) {
            throw new RuntimeException("当前日期不在允许的预约星期内");
        }
        LocalTime winStart = parseTimeParts(String.valueOf(tr.getOrDefault("start", "08:00")));
        LocalTime winEnd = parseTimeParts(String.valueOf(tr.getOrDefault("end", "22:00")));
        if (bookingStartT.isBefore(winStart) || bookingEndT.isAfter(winEnd)) {
            throw new RuntimeException("当前时段不在系统允许预约的时间内（"
                    + winStart + "~" + winEnd + "）");
        }

        Map<String, Object> role = mergedRoleLimits();
        SysUser user = sysUserService.getById(order.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String ut = user.getUserType() != null ? user.getUserType() : "";
        // 【注意】学生是否需要强制审核的逻辑不在此验证层处理；
        //        由 createBooking() 根据 studentNeedAudit 配置决定是自动通过(status=1)还是待审(status=0)。
        //        此处只做硬性约束（如维护人员完全无权限）。
        if ("MAINTAINER".equalsIgnoreCase(ut)) {
            boolean can = parseBooleanFlag(role.get("maintainerCanBook"));
            if (!can) {
                throw new RuntimeException("维护人员暂无设备预约权限");
            }
        }

        double newHours = order.getDuration() != null && order.getDuration() > 0
                ? order.getDuration()
                : bookingMinutes / 60.0;
        if (enforceWithinLimit) {
            if ("STUDENT".equalsIgnoreCase(ut)) {
                int cap = intFromMap(role, "studentDeviceHours", 4);
                double used = sumBookedHoursSameDeviceDay(order.getUserId(), order.getDeviceId(), d, excludeBookingId);
                if (used + newHours > cap + 1e-6) {
                    throw new RuntimeException("超过学生单台设备单日累计预约时长上限（" + cap + " 小时）");
                }
            } else if ("TEACHER".equalsIgnoreCase(ut)) {
                int cap = intFromMap(role, "teacherDeviceHours", 8);
                double used = sumBookedHoursSameDeviceDay(order.getUserId(), order.getDeviceId(), d, excludeBookingId);
                if (used + newHours > cap + 1e-6) {
                    throw new RuntimeException("超过教师单台设备单日累计预约时长上限（" + cap + " 小时）");
                }
            }
        }
    }

    private static LocalTime parseTimeParts(String ts) {
        String[] parts = ts.trim().split(":");
        int hour = Integer.parseInt(parts[0].trim());
        int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
        return LocalTime.of(hour, minute);
    }

    private static boolean parseBooleanFlag(Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private static int intFromMap(Map<String, Object> m, String key, int def) {
        if (m == null) {
            return def;
        }
        Object o = m.get(key);
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private Map<String, Object> mergedBasicRule() {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("minAdvanceHours", 24);
        def.put("maxBookingHours", 4);
        def.put("maxBookingsPerDevicePerDay", 3);
        def.put("maxBookingsPerUserPerDay", 5);
        def.put("cancelDeadlineHours", 2);
        def.put("noShowThreshold", 3);
        Map<String, Object> fromDb = sysConfigService.getJsonObject(KEY_GLOBAL_BASIC);
        if (fromDb != null && !fromDb.isEmpty()) {
            def.putAll(fromDb);
        }
        return def;
    }

    private Map<String, Object> mergedTimeRange() {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("start", "08:00");
        def.put("end", "22:00");
        def.put("weekdays", new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5")));
        Map<String, Object> fromDb = sysConfigService.getJsonObject(KEY_GLOBAL_TIME);
        if (fromDb != null && !fromDb.isEmpty()) {
            def.putAll(fromDb);
        }
        return def;
    }

    private Map<String, Object> mergedRoleLimits() {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("studentDeviceHours", 4);
        def.put("teacherDeviceHours", 8);
        def.put("maintainerCanBook", false);
        def.put("studentNeedAudit", true);

        Map<String, Object> fromDb = sysConfigService.getJsonObject(KEY_GLOBAL_ROLE);
        // 兼容历史键名 booking.global.role，防止读不到新配置导致限额不生效
        if (fromDb == null || fromDb.isEmpty()) {
            fromDb = sysConfigService.getJsonObject(KEY_GLOBAL_ROLE_LEGACY);
        }
        if (fromDb != null && !fromDb.isEmpty()) {
            def.putAll(fromDb);
        }
        return def;
    }

    private static boolean weekdayAllowed(List<?> weekdays, int dow) {
        for (Object o : weekdays) {
            if (o == null) {
                continue;
            }
            int v;
            if (o instanceof Number) {
                v = ((Number) o).intValue();
            } else {
                v = Integer.parseInt(o.toString().trim());
            }
            if (v == dow) {
                return true;
            }
        }
        return false;
    }

    private long countUserBookingsOnDate(Long userId, LocalDate date, Long excludeBookingId) {
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("user_id", userId);
        w.apply("DATE(booking_date) = {0}", date.toString());
        w.in("status", 0, 1);
        if (excludeBookingId != null) {
            w.ne("id", excludeBookingId);
        }
        return count(w);
    }

    private long countDeviceBookingsOnDate(Long deviceId, LocalDate date, Long excludeBookingId) {
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("device_id", deviceId);
        w.apply("DATE(booking_date) = {0}", date.toString());
        w.in("status", 0, 1);
        if (excludeBookingId != null) {
            w.ne("id", excludeBookingId);
        }
        return count(w);
    }

    private double sumBookedHoursSameDeviceDay(Long userId, Long deviceId, LocalDate date, Long excludeBookingId) {
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("user_id", userId).eq("device_id", deviceId);
        w.apply("DATE(booking_date) = {0}", date.toString());
        // 单台设备单日累计时长：待审核/已通过/已签到/已完成都计入
        // 仅已拒绝(2)与已取消(4)不计入
        w.in("status", 0, 1, 3, 5);
        if (excludeBookingId != null) {
            w.ne("id", excludeBookingId);
        }
        double sum = 0;
        for (BookingOrder b : list(w)) {
            sum += bookingOrderHours(b);
        }
        return sum;
    }

    private static double bookingOrderHours(BookingOrder b) {
        if (b.getDuration() != null && b.getDuration() > 0) {
            return b.getDuration();
        }
        try {
            LocalTime s = parseTimeParts(b.getStartTime());
            LocalTime e = parseTimeParts(b.getEndTime());
            return ChronoUnit.MINUTES.between(s, e) / 60.0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 新预约待审核时，向可审核角色推送站内消息（每人一条，避免申请人收到）
     */
    private void notifyAuditorsNewPendingBooking(BookingOrder bookingOrder) {
        SysUser applicant = bookingOrder.getUserId() != null ? sysUserService.getById(bookingOrder.getUserId()) : null;
        String applicantName = applicant != null
                ? (applicant.getRealName() != null && !applicant.getRealName().isEmpty()
                ? applicant.getRealName() : applicant.getUsername())
                : "用户";
        DeviceInfo device = bookingOrder.getDeviceId() != null ? deviceInfoService.getById(bookingOrder.getDeviceId()) : null;
        String deviceName = device != null ? device.getDeviceName() : "设备";
        String dateStr = "";
        if (bookingOrder.getBookingDate() != null) {
            dateStr = bookingOrder.getBookingDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        String title = "新预约待审核";
        String content = String.format(
                "申请人：%s，设备：%s，日期：%s %s-%s，单号：%s。请在「预约审核」中处理。",
                applicantName,
                deviceName,
                dateStr,
                bookingOrder.getStartTime() != null ? bookingOrder.getStartTime() : "",
                bookingOrder.getEndTime() != null ? bookingOrder.getEndTime() : "",
                bookingOrder.getOrderNo() != null ? bookingOrder.getOrderNo() : "");

        Set<Long> recipientIds = new LinkedHashSet<>();
        QueryWrapper<SysUser> adminLab = new QueryWrapper<>();
        adminLab.eq("status", 1).in("user_type", "SYSTEM_ADMIN", "LAB_ADMIN");
        for (SysUser u : sysUserService.list(adminLab)) {
            if (u.getId() != null) {
                recipientIds.add(u.getId());
            }
        }
        if (applicant != null && "STUDENT".equalsIgnoreCase(applicant.getUserType())) {
            QueryWrapper<SysUser> teachers = new QueryWrapper<>();
            teachers.eq("status", 1).eq("user_type", "TEACHER");
            for (SysUser t : sysUserService.list(teachers)) {
                if (t.getId() != null) {
                    recipientIds.add(t.getId());
                }
            }
        }
        Long applicantId = bookingOrder.getUserId();
        for (Long uid : recipientIds) {
            if (applicantId != null && applicantId.equals(uid)) {
                continue;
            }
            sysMessageService.sendMessage(uid, "BOOKING_AUDIT", title, content, bookingOrder.getId(), "booking_order");
        }
    }

    /** 检查用户当日预约次数是否超限 */
    private boolean checkWithinUserDailyLimit(BookingOrder booking) {
        if (booking.getUserId() == null) return false;
        LocalDate date = booking.getBookingDate().toLocalDate();
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("user_id", booking.getUserId())
         .apply("DATE(booking_date) = {0}", date.toString())
         .in("status", 1, 0);
        long existing = count(w);

        Map<String, Object> basic = mergedBasicRule();
        int maxPerUserPerDay = intFromMap(basic, "maxBookingsPerUserPerDay", 5);
        return existing < maxPerUserPerDay;
    }

    /** 检查是否符合提前申请时间要求 */
    private boolean checkAdvanceTime(BookingOrder booking) {
        Map<String, Object> basic = mergedBasicRule();
        int minAdvanceHours = intFromMap(basic, "minAdvanceHours", 24);
        if (booking.getBookingDate() == null) {
            return false;
        }
        LocalDateTime bookingStart = booking.getBookingDate().toLocalDate().atStartOfDay();
        try {
            String startTimeStr = booking.getStartTime();
            if (startTimeStr != null && startTimeStr.length() >= 4) {
                bookingStart = booking.getBookingDate().toLocalDate().atTime(parseTimeParts(startTimeStr));
            }
        } catch (Exception ignored) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !bookingStart.isBefore(now.plusHours(minAdvanceHours));
    }

    /**
     * 是否启用「提前申请时长」硬约束：
     * MANUAL 模式始终启用；AUTO 模式仅当勾选 advanceTime 时启用。
     */
    private boolean shouldEnforceAdvanceTimeConstraint() {
        try {
            String auditMode = sysConfigService.getConfigValue(KEY_AUDIT_MODE);
            if (!"AUTO".equalsIgnoreCase(auditMode)) {
                return true;
            }
            List<String> conditions = sysConfigService.getJsonArray(KEY_AUTO_CONDITIONS);
            return conditions != null && conditions.contains("advanceTime");
        } catch (Exception e) {
            // 配置异常时保守启用
            return true;
        }
    }

    /**
     * 是否启用「无时间冲突」硬约束：
     * MANUAL 模式始终启用；AUTO 模式仅当勾选 noConflict 时启用。
     */
    private boolean shouldEnforceNoConflictConstraint() {
        try {
            String auditMode = sysConfigService.getConfigValue(KEY_AUDIT_MODE);
            if (!"AUTO".equalsIgnoreCase(auditMode)) {
                return true;
            }
            List<String> conditions = sysConfigService.getJsonArray(KEY_AUTO_CONDITIONS);
            return conditions != null && conditions.contains("noConflict");
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 是否启用「在预约限额内」硬约束：
     * MANUAL 模式始终启用；AUTO 模式仅当勾选 withinLimit 时启用。
     */
    private boolean shouldEnforceWithinLimitConstraint() {
        try {
            String auditMode = sysConfigService.getConfigValue(KEY_AUDIT_MODE);
            if (!"AUTO".equalsIgnoreCase(auditMode)) {
                return true;
            }
            List<String> conditions = sysConfigService.getJsonArray(KEY_AUTO_CONDITIONS);
            return conditions != null && conditions.contains("withinLimit");
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    @Transactional
    public boolean auditBooking(Long id, Long auditorId, Integer status, String opinion) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (bookingOrder.getStatus() != 0) {
            throw new RuntimeException("该预约已审核");
        }

        Long oldDeviceId = bookingOrder.getDeviceId();
        Long pendingReplaceId = bookingOrder.getReplaceDeviceId();

        // 审核通过且存在设备替换申请：将正式预约设备更新为替换目标（与 replace_reason 一并落库后再清空替换字段）
        if (status == 1 && pendingReplaceId != null) {
            if (pendingReplaceId.equals(oldDeviceId)) {
                throw new RuntimeException("替换设备数据无效");
            }
            if (checkConflict(pendingReplaceId, bookingOrder.getBookingDate(),
                    bookingOrder.getStartTime(), bookingOrder.getEndTime())) {
                throw new RuntimeException("替换设备在该时段已被预约，无法审核通过");
            }
            bookingOrder.setDeviceId(pendingReplaceId);
            bookingOrder.setReplaceDeviceId(null);
            bookingOrder.setReplaceReason(null);
        }

        bookingOrder.setAuditorId(auditorId);
        bookingOrder.setStatus(status);
        bookingOrder.setAuditOpinion(opinion);
        bookingOrder.setAuditTime(LocalDateTime.now());

        boolean updated = updateById(bookingOrder);
        if (updated) {
            // 发送审核结果通知
            String title = status == 1 ? "预约审核通过" : "预约审核未通过";
            String content = status == 1
                    ? "您的预约申请已审核通过，设备：" + (deviceInfoService.getById(bookingOrder.getDeviceId()) != null ? deviceInfoService.getById(bookingOrder.getDeviceId()).getDeviceName() : "")
                    : "您的预约申请未通过审核，原因：" + (opinion != null ? opinion : "无");
            sysMessageService.sendMessage(
                    bookingOrder.getUserId(),
                    "BOOKING_AUDIT",
                    title,
                    content,
                    bookingOrder.getId(),
                    "booking_order"
            );

            if (status == 1 && bookingOrder.getDeviceId() != null) {
                // 审核通过时不立即更新设备状态，等待用户签到时再更新
                // deviceInfoService.updateDeviceStatus(bookingOrder.getDeviceId(), 1);
                if (pendingReplaceId != null && oldDeviceId != null && !oldDeviceId.equals(bookingOrder.getDeviceId())) {
                    restoreDeviceIfNoActiveBookings(oldDeviceId);
                }
            } else if (status == 2 && oldDeviceId != null) {
                restoreDeviceIfNoActiveBookings(oldDeviceId);
            }
        }
        return updated;
    }

    @Override
    public boolean completeBooking(Long id, Long userId) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!bookingOrder.getUserId().equals(userId)) {
            throw new RuntimeException("无权结束该预约");
        }
        if (bookingOrder.getStatus() != 1 && bookingOrder.getStatus() != 5) {
            throw new RuntimeException("仅已通过的预约可标记使用完成");
        }
        bookingOrder.setStatus(3); // 已完成
        bookingOrder.setActualEndTime(LocalDateTime.now());
        boolean updated = updateById(bookingOrder);
        if (updated && bookingOrder.getDeviceId() != null) {
            restoreDeviceIfNoActiveBookings(bookingOrder.getDeviceId());
        }
        return updated;
    }

    @Override
    public void syncDeviceStatusWithBookings(Long deviceId) {
        if (deviceId == null) {
            return;
        }
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("device_id", deviceId);
        // 只检查已签到的预约，已通过的预约不视为占用设备
        w.eq("status", 5);
        if (count(w) > 0) {
            deviceInfoService.updateDeviceStatus(deviceId, 1);
        } else {
            deviceInfoService.updateDeviceStatus(deviceId, 0);
        }
    }

    @Override
    public boolean cancelBooking(Long id, Long userId) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!bookingOrder.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该预约");
        }

        Integer status = bookingOrder.getStatus();
        if (status != null && status == 3) {
            throw new RuntimeException("已完成的预约无法取消");
        }
        if (status != null && status == 4) {
            throw new RuntimeException("预约已取消");
        }

        // 已签到视为进入使用流程，不允许再走取消（应走签退/完成）
        if (status != null && status == 5) {
            throw new RuntimeException("已签到的预约无法取消，请先签退");
        }

        // 检查取消时限：对「已通过（待签到）」预约生效
        if (status != null && status == 1) {
            Map<String, Object> basic = mergedBasicRule();
            int cancelDeadlineHours = intFromMap(basic, "cancelDeadlineHours", 2);
            if (cancelDeadlineHours > 0) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime bookingStart = bookingOrder.getBookingDate().toLocalDate()
                        .atTime(parseTimeParts(bookingOrder.getStartTime()));
                if (bookingStart.isBefore(now.plusHours(cancelDeadlineHours))) {
                    throw new RuntimeException("预约开始前 " + cancelDeadlineHours + " 小时内不允许取消");
                }
            }
        }

        bookingOrder.setStatus(4); // 已取消
        Long deviceId = bookingOrder.getDeviceId();
        boolean updated = updateById(bookingOrder);
        if (updated && deviceId != null) {
            restoreDeviceIfNoActiveBookings(deviceId);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean adminCancelBooking(Long id, Long operatorId, String reason) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (bookingOrder.getStatus() != null && bookingOrder.getStatus() == 3) {
            throw new RuntimeException("已完成的预约无法取消");
        }
        if (bookingOrder.getStatus() != null && bookingOrder.getStatus() == 4) {
            throw new RuntimeException("预约已取消");
        }
        String reasonNote = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "";
        String append = "【管理员取消】" + (reasonNote.isEmpty() ? ("操作人ID:" + operatorId) : reasonNote);
        String oldOpinion = bookingOrder.getAuditOpinion() != null ? bookingOrder.getAuditOpinion() : "";
        bookingOrder.setAuditOpinion(oldOpinion.isEmpty() ? append : (oldOpinion + "；" + append));
        bookingOrder.setStatus(4);
        Long deviceId = bookingOrder.getDeviceId();
        boolean updated = updateById(bookingOrder);
        if (updated && deviceId != null) {
            restoreDeviceIfNoActiveBookings(deviceId);
        }
        return updated;
    }

    private void restoreDeviceIfNoActiveBookings(Long deviceId) {
        QueryWrapper<BookingOrder> w = new QueryWrapper<>();
        w.eq("device_id", deviceId);
        // 只有“已签到”预约视为占用设备
        w.eq("status", 5);
        if (count(w) == 0) {
            deviceInfoService.updateDeviceStatus(deviceId, 0);
        }
    }

    @Override
    public boolean checkConflict(Long deviceId, LocalDateTime bookingDate, String startTime, String endTime) {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        wrapper.apply("DATE(booking_date) = {0}", bookingDate.toLocalDate().toString());
        wrapper.in("status", 0, 1); // 待审核和已通过
        wrapper.and(w -> w.le("start_time", endTime).ge("end_time", startTime));
        return count(wrapper) > 0;
    }

    @Override
    public Map<String, Object> checkConflictWithDetails(Long deviceId, LocalDateTime bookingDate, String startTime, String endTime) {
        Map<String, Object> result = new java.util.HashMap<>();
        List<String> conflicts = new ArrayList<>();
        List<DeviceInfo> suggestions = new ArrayList<>();

        // 1. 检测设备冲突
        if (checkConflict(deviceId, bookingDate, startTime, endTime)) {
            conflicts.add("设备时间冲突：该设备在所选时间段已被预约");
            suggestions.addAll(findAlternativeDevices(deviceId, bookingDate, startTime, endTime));
        }

        // 2. 获取设备信息，检查场地冲突
        DeviceInfo device = deviceInfoService.getById(deviceId);
        if (device != null && device.getLaboratory() != null) {
            // 检查同一实验室其他设备是否有冲突
            QueryWrapper<DeviceInfo> labWrapper = new QueryWrapper<>();
            labWrapper.eq("laboratory", device.getLaboratory());
            labWrapper.ne("id", deviceId); // 排除当前预约的设备
            List<DeviceInfo> sameLabDevices = deviceInfoService.list(labWrapper);

            for (DeviceInfo labDevice : sameLabDevices) {
                if (checkConflict(labDevice.getId(), bookingDate, startTime, endTime)) {
                    // 同一实验室有设备冲突
                    conflicts.add("场地冲突：同一实验室（" + device.getLaboratory() + "）存在其他设备在该时段被预约");
                    break;
                }
            }
        }

        // 3. 检查时间边界冲突（临界点）
        // 预约开始时间等于其他预约结束时间，或者结束时间等于其他预约开始时间，不算冲突
        QueryWrapper<BookingOrder> boundaryWrapper = new QueryWrapper<>();
        boundaryWrapper.eq("device_id", deviceId);
        boundaryWrapper.apply("DATE(booking_date) = {0}", bookingDate.toLocalDate().toString());
        boundaryWrapper.in("status", 0, 1);
        // 只检查真正重叠的时间段
        boundaryWrapper.and(w -> w.lt("start_time", endTime).gt("end_time", startTime));
        long conflictCount = count(boundaryWrapper);

        result.put("hasConflict", conflictCount > 0 || !conflicts.isEmpty());
        result.put("conflicts", conflicts);
        result.put("suggestions", suggestions);
        result.put("conflictCount", conflictCount);

        return result;
    }

    private List<DeviceInfo> findAlternativeDevices(Long deviceId, LocalDateTime bookingDate, String startTime, String endTime) {
        List<DeviceInfo> alternatives = new ArrayList<>();
        DeviceInfo original = deviceInfoService.getById(deviceId);
        if (original == null) return alternatives;

        // 查找同类型、同实验室的可用设备
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        if (original.getCategoryId() != null) {
            wrapper.eq("category_id", original.getCategoryId());
        }
        if (original.getLaboratory() != null) {
            wrapper.eq("laboratory", original.getLaboratory());
        }
        wrapper.eq("status", 0);
        wrapper.ne("id", deviceId);

        List<DeviceInfo> candidates = deviceInfoService.list(wrapper);
        for (DeviceInfo candidate : candidates) {
            if (!checkConflict(candidate.getId(), bookingDate, startTime, endTime)) {
                alternatives.add(candidate);
                if (alternatives.size() >= 3) break; // 最多推荐3个
            }
        }
        return alternatives;
    }

    @Override
    public Page<BookingOrder> pageUserBookings(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<BookingOrder> page = new Page<>(pageNum, pageSize);
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        return page(page, wrapper);
    }

    @Override
    public Page<BookingOrder> pagePendingAudit(Integer pageNum, Integer pageSize) {
        Page<BookingOrder> page = new Page<>(pageNum, pageSize);
        baseMapper.selectPendingAuditPage(page);
        return page;
    }

    @Override
    public long getTodayBookingCount() {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.apply("DATE(booking_date) = {0}", LocalDate.now().toString());
        return count(wrapper);
    }

    @Override
    public long getWeekBookingCount() {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.ge("booking_date", LocalDate.now().minusDays(6));
        return count(wrapper);
    }

    @Override
    public long countAllBookings() {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        return count(wrapper);
    }

    @Override
    public long countPendingAudit() {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0); // 待审核
        return count(wrapper);
    }

    @Override
    public List<Map<String, Object>> getBookingTrend() {
        return baseMapper.selectBookingTrend();
    }

    @Override
    public List<Map<String, Object>> getPeakHours() {
        return baseMapper.selectPeakHours();
    }

    @Override
    public Double getAvgWaitTime(String period) {
        return baseMapper.selectAvgWaitTime(period);
    }

    @Override
    public List<Map<String, Object>> getWaitTimeTrend(Integer days) {
        return baseMapper.selectWaitTimeTrend(days);
    }

    @Override
    public Integer getUserBookingCount(Long userId, String period) {
        Integer count = baseMapper.countUserBookings(userId, period);
        return count != null ? count : 0;
    }

    @Override
    public Double getUserBookingDuration(Long userId, String period) {
        Double duration = baseMapper.sumUserBookingDuration(userId, period);
        return duration != null ? duration : 0.0;
    }

    @Override
    public boolean applyDeviceReplace(Long bookingId, Long newDeviceId, String reason, Long userId) {
        BookingOrder booking = getById(bookingId);
        if (booking == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此预约");
        }
        if (booking.getStatus() != 0) {
            throw new RuntimeException("仅待审核状态的预约可以申请设备替换");
        }
        if (newDeviceId.equals(booking.getDeviceId())) {
            throw new RuntimeException("替换设备不能与原设备相同");
        }

        // 检查新设备是否可用（无冲突）
        if (checkConflict(newDeviceId, booking.getBookingDate(), booking.getStartTime(), booking.getEndTime())) {
            throw new RuntimeException("替换设备在所选时间段已被预约");
        }

        // 更新预约信息
        booking.setReplaceDeviceId(newDeviceId);
        booking.setReplaceReason(reason);
        return updateById(booking);
    }

    @Override
    public List<DeviceInfo> getReplaceableDevices(Long originalDeviceId, LocalDateTime bookingDate, String startTime, String endTime) {
        // 获取同类型且可用的设备
        DeviceInfo originalDevice = deviceInfoService.getById(originalDeviceId);
        if (originalDevice == null) {
            return new ArrayList<>();
        }

        // 查询同实验室下的可用设备
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("laboratory", originalDevice.getLaboratory());
        wrapper.eq("status", 0); // 空闲状态
        wrapper.ne("id", originalDeviceId); // 排除原设备
        List<DeviceInfo> devices = deviceInfoService.list(wrapper);

        // 过滤掉有冲突的设备
        List<DeviceInfo> availableDevices = new ArrayList<>();
        for (DeviceInfo device : devices) {
            if (!checkConflict(device.getId(), bookingDate, startTime, endTime)) {
                availableDevices.add(device);
            }
        }
        return availableDevices;
    }

    @Override
    public Page<BookingOrder> pageGlobalBookings(Integer pageNum, Integer pageSize,
            String orderNo, String deviceName, String userName, Integer status, String auditStatus) {
        Page<BookingOrder> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        baseMapper.selectGlobalListPage(page, orderNo, deviceName, userName, status, auditStatus);
        return page;
    }

    @Override
    @Transactional
    public boolean adminForceCloseBooking(Long id) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (bookingOrder.getStatus() != null && bookingOrder.getStatus() == 3) {
            throw new RuntimeException("已完成的预约不可强制关闭");
        }
        int oldStatus = bookingOrder.getStatus() == null ? -1 : bookingOrder.getStatus();
        bookingOrder.setStatus(4);
        boolean updated = updateById(bookingOrder);
        if (updated && bookingOrder.getDeviceId() != null && oldStatus == 1) {
            restoreDeviceIfNoActiveBookings(bookingOrder.getDeviceId());
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean adminMarkComplete(Long id, Long operatorId) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        Integer s = bookingOrder.getStatus();
        if (s == null || (s != 1 && s != 5)) {
            throw new RuntimeException("仅已通过或已签到的预约可标记完成");
        }
        bookingOrder.setStatus(3); // 已完成
        bookingOrder.setActualEndTime(LocalDateTime.now());
        boolean updated = updateById(bookingOrder);
        if (updated && bookingOrder.getDeviceId() != null) {
            restoreDeviceIfNoActiveBookings(bookingOrder.getDeviceId());
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean adminMarkNoShow(Long id, Long operatorId) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        Integer s = bookingOrder.getStatus();
        if (s == null || (s != 1 && s != 5)) {
            throw new RuntimeException("仅已通过或已签到的预约可标记爽约");
        }
        // 1. 标记预约为已完成
        bookingOrder.setStatus(3); // 已完成
        bookingOrder.setActualEndTime(LocalDateTime.now());
        updateById(bookingOrder);
        // 2. 释放设备占用
        if (bookingOrder.getDeviceId() != null) {
            restoreDeviceIfNoActiveBookings(bookingOrder.getDeviceId());
        }
        // 3. 累加用户爽约计数
        SysUser user = sysUserService.getById(bookingOrder.getUserId());
        if (user != null) {
            int missed = user.getMissedCount() == null ? 0 : user.getMissedCount();
            user.setMissedCount(missed + 1);
            sysUserService.updateById(user);
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> listHotDevices(int days, int limit) {
        return baseMapper.selectHotDevicesByBookings(days, limit > 0 ? limit : 10);
    }

    @Override
    public List<Map<String, Object>> listIdleDevicesAnalysis(int days, int limit) {
        return baseMapper.selectIdleDevicesNotBooked(days, limit > 0 ? limit : 20);
    }

    @Override
    @Transactional
    public boolean checkInBooking(Long id, Long userId) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!bookingOrder.getUserId().equals(userId)) {
            throw new RuntimeException("无权签到该预约");
        }
        if (bookingOrder.getStatus() == null || bookingOrder.getStatus() != 1) {
            throw new RuntimeException("仅已通过的预约可以签到");
        }
        if (bookingOrder.getActualStartTime() != null) {
            throw new RuntimeException("该预约已签到");
        }
        bookingOrder.setActualStartTime(LocalDateTime.now());
        bookingOrder.setStatus(5); // 已签到
        boolean updated = updateById(bookingOrder);
        // 签到后设备状态改为"使用中"
        if (updated && bookingOrder.getDeviceId() != null) {
            deviceInfoService.updateDeviceStatus(bookingOrder.getDeviceId(), 1);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean checkOutBooking(Long id, Long userId, String evaluation) {
        BookingOrder bookingOrder = getById(id);
        if (bookingOrder == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!bookingOrder.getUserId().equals(userId)) {
            throw new RuntimeException("无权签退该预约");
        }
        Integer s = bookingOrder.getStatus();
        if (s == null || (s != 1 && s != 3 && s != 5)) {
            throw new RuntimeException("预约状态不支持签退");
        }
        bookingOrder.setActualEndTime(LocalDateTime.now());
        bookingOrder.setEvaluation(evaluation);
        bookingOrder.setStatus(3); // 已完成
        boolean updated = updateById(bookingOrder);
        if (updated && bookingOrder.getDeviceId() != null) {
            restoreDeviceIfNoActiveBookings(bookingOrder.getDeviceId());
        }
        return updated;
    }
}
