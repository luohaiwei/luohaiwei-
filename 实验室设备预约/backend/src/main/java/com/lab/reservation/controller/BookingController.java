package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.DataScopeContext;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.BookingOrderMapper;
import com.lab.reservation.mapper.DeviceInfoMapper;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.DataScopeService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.SysConfigService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预约管理控制器
 * 处理预约相关的业务逻辑
 */
@RestController
@RequestMapping("/booking")
@CrossOrigin
public class BookingController {

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private BookingOrderMapper bookingOrderMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return null;
            }
            String username = authentication.getName();
            SysUser user = sysUserService.getByUsername(username);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 填充设备名称、拟替换设备名称、申请人展示名（列表/详情共用）
     */
    private void enrichBookingOrder(BookingOrder booking) {
        if (booking == null) {
            return;
        }
        if (booking.getDeviceId() != null) {
            // 直接Mapper查询，避免触发数据权限检查
            DeviceInfo device = deviceInfoMapper.selectById(booking.getDeviceId());
            if (device != null) {
                booking.setDeviceName(device.getDeviceName());
                booking.setDeviceNo(device.getDeviceNo());
            }
        }
        if (booking.getReplaceDeviceId() != null) {
            DeviceInfo rep = deviceInfoMapper.selectById(booking.getReplaceDeviceId());
            if (rep != null) {
                booking.setReplaceDeviceName(rep.getDeviceName());
            }
        }
        if (booking.getUserId() != null) {
            SysUser user = sysUserService.getById(booking.getUserId());
            if (user != null) {
                booking.setUserName(user.getRealName() != null && !user.getRealName().isEmpty()
                        ? user.getRealName() : user.getUsername());
            }
        }
    }

    /**
     * 创建预约
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody BookingOrder bookingOrder) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(error);
        }
        SysUser applicant = sysUserService.getById(userId);
        String applicantName = (applicant != null && applicant.getRealName() != null)
            ? applicant.getRealName() : (applicant != null ? applicant.getUsername() : "未知用户");

        bookingOrder.setUserId(userId);

        // 控制器层防御性检查：提前量 + 日期合法性（独立于 Service 层，避免被绕过）
        try {
            String startTimeStr = bookingOrder.getStartTime();
            if (bookingOrder.getBookingDate() == null) {
                throw new RuntimeException("预约日期不能为空");
            }
            if (startTimeStr == null || startTimeStr.isEmpty()) {
                throw new RuntimeException("请选择开始时间");
            }
            if (bookingOrder.getEndTime() == null || bookingOrder.getEndTime().isEmpty()) {
                throw new RuntimeException("请选择结束时间");
            }

            // 读取提前量配置
            String basicJson = sysConfigService.getConfigValue("booking.global.basic");
            int minAdvanceHours = 24;
            if (basicJson != null && !basicJson.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> basic = om.readValue(basicJson, java.util.Map.class);
                    Object v = basic.get("minAdvanceHours");
                    if (v != null) {
                        minAdvanceHours = ((Number) v).intValue();
                    }
                } catch (Exception ignored) {}
            }

            // 自动审核模式且未勾选「符合提前申请时间」时，不做提前量硬拦截
            boolean enforceAdvanceTime = true;
            try {
                String auditMode = sysConfigService.getConfigValue("booking.audit.mode");
                java.util.List<String> autoConditions = sysConfigService.getJsonArray("booking.audit.autoConditions");
                boolean isAuto = "AUTO".equalsIgnoreCase(auditMode);
                boolean requireAdvance = autoConditions != null && autoConditions.contains("advanceTime");
                enforceAdvanceTime = !isAuto || requireAdvance;
            } catch (Exception ignored) {}

            // 计算预约开始时刻
            String[] parts = startTimeStr.trim().split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            java.time.LocalDateTime bookingStart = bookingOrder.getBookingDate().toLocalDate().atTime(hour, minute);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (enforceAdvanceTime && bookingStart.isBefore(now.plusHours(minAdvanceHours))) {
                throw new RuntimeException("距预约开始时间不足 " + minAdvanceHours + " 小时，不符合提前申请要求");
            }
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }

        try {
            bookingOrderService.createBooking(bookingOrder);
            Map<String, Object> result = new HashMap<>();
            // status=0: pending review; status=1: auto-approved. Neither is an error.
            result.put("message", "预约成功");
            result.put("status", bookingOrder.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 我的预约列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> myBookings(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(error);
        }

        Page<BookingOrder> page = bookingOrderService.pageUserBookings(userId, pageNum, pageSize, status);
        for (BookingOrder b : page.getRecords()) {
            enrichBookingOrder(b);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }



    /**
     * 导出预约列表 CSV（系统管理员全量；其他角色须数据权限 EXPORT，且按 global-list 同等范围过滤）
     */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportGlobalBookings(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String auditStatus) {
        if (!hasBookingExportDataPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        DataScopeContext exportCtx = dataScopeService.getCurrentDataScope();
        Page<BookingOrder> page;
        if (exportCtx != null && exportCtx.isSystemAdmin()) {
            page = bookingOrderService.pageGlobalBookings(
                    1, 10_000, orderNo, deviceName, userName, status, auditStatus);
        } else {
            page = pageGlobalListQuery(1, 10_000, orderNo, deviceName, userName, status, auditStatus);
        }
        for (BookingOrder b : page.getRecords()) {
            enrichBookingOrder(b);
        }
        StringBuilder csv = new StringBuilder("\uFEFF预约单号,设备名称,申请人,预约日期,开始,结束,实验项目,状态\n");
        for (BookingOrder b : page.getRecords()) {
            csv.append(escapeCsv(b.getOrderNo())).append(',')
                    .append(escapeCsv(b.getDeviceName())).append(',')
                    .append(escapeCsv(b.getUserName())).append(',')
                    .append(b.getBookingDate() != null ? b.getBookingDate().toLocalDate() : "").append(',')
                    .append(escapeCsv(b.getStartTime())).append(',')
                    .append(escapeCsv(b.getEndTime())).append(',')
                    .append(escapeCsv(b.getExperimentProject())).append(',')
                    .append(b.getStatus() == null ? "" : b.getStatus()).append('\n');
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "bookings_" + java.time.LocalDate.now() + ".csv";
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                            + URLEncoder.encode(filename, "UTF-8"))
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(bytes);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeCsv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * 管理员取消他人预约（固定路径，避免 DELETE /{id}/admin-cancel 在部分网关/代理下 404）
     * 须数据权限 CANCEL；与 {@link #adminCancelBookingByPath} 等价
     */
    @PutMapping("/admin/cancel")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> adminCancelBookingByQuery(
            @RequestParam("id") Long id,
            @RequestParam(required = false) String reason) {
        if (!hasBookingCancelOthersDataPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无取消他人预约的数据权限");
        }
        Long operatorId = getCurrentUserId();
        if (operatorId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            bookingOrderService.adminCancelBooking(id, operatorId, reason);
            return ResponseEntity.ok("取消成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 强制关闭预约（系统管理员）
     */
    @PutMapping("/{id}/force-close")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> forceCloseBooking(@PathVariable Long id) {
        Map<String, Object> body = new HashMap<>();
        try {
            bookingOrderService.adminForceCloseBooking(id);
            body.put("message", "已强制关闭");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    /**
     * 管理员：标记预约已完成（正常结束使用）
     * 使用固定路径 + 查询参数 id，避免部分网关/代理对 /{id}/admin-complete 返回 404（与 admin/cancel 一致）
     */
    @PutMapping("/admin/complete")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> adminCompleteBooking(@RequestParam("id") Long id) {
        Map<String, Object> body = new HashMap<>();
        try {
            Long operatorId = getCurrentUserId();
            if (operatorId == null) {
                body.put("message", "用户未登录");
                return ResponseEntity.badRequest().body(body);
            }
            if (!hasBookingCancelOthersDataPermission()) {
                body.put("message", "无操作权限");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
            }
            bookingOrderService.adminMarkComplete(id, operatorId);
            body.put("message", "已标记完成");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    /**
     * 管理员：标记预约爽约（累加用户爽约计数）
     * 使用固定路径 + 查询参数 id，避免部分网关对 /{id}/admin-no-show 返回 404
     */
    @PutMapping("/admin/no-show")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> adminNoShowBooking(@RequestParam("id") Long id) {
        Map<String, Object> body = new HashMap<>();
        try {
            Long operatorId = getCurrentUserId();
            if (operatorId == null) {
                body.put("message", "用户未登录");
                return ResponseEntity.badRequest().body(body);
            }
            if (!hasBookingCancelOthersDataPermission()) {
                body.put("message", "无操作权限");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
            }
            bookingOrderService.adminMarkNoShow(id, operatorId);
            body.put("message", "已标记爽约");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    /**
     * 获取可替换的设备列表（须写在 /{id} 详情之前，避免 replaceable-devices 被误路由到详情接口）
     */
    @GetMapping("/{id}/replaceable-devices")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> getReplaceableDevices(@PathVariable Long id) {
        BookingOrder booking = bookingOrderService.getById(id);
        if (booking == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "预约不存在");
            return ResponseEntity.badRequest().body(error);
        }
        List<DeviceInfo> devices = bookingOrderService.getReplaceableDevices(
                booking.getDeviceId(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime()
        );
        Map<String, Object> result = new HashMap<>();
        result.put("devices", devices);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取签到状态（须写在 /{id} 之前）
     */
    @GetMapping("/{id}/checkin-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCheckInStatus(@PathVariable Long id) {
        BookingOrder booking = bookingOrderService.getById(id);
        if (booking == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "预约不存在");
            return ResponseEntity.badRequest().body(error);
        }
        enrichBookingOrder(booking);
        Map<String, Object> result = new HashMap<>();
        result.put("booking", booking);
        result.put("checkedIn", booking.getActualStartTime() != null);
        result.put("checkedOut", booking.getActualEndTime() != null);
        return ResponseEntity.ok(result);
    }

    /**
     * 签到（须写在 /{id} 之前）
     */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> checkIn(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            bookingOrderService.checkInBooking(id, userId);
            return ResponseEntity.ok("签到成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 签退（须写在 /{id} 之前）
     */
    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> checkOut(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        String evaluation = body != null ? String.valueOf(body.get("evaluation")) : null;
        try {
            bookingOrderService.checkOutBooking(id, userId, evaluation);
            return ResponseEntity.ok("签退成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 预约详情（必须写在所有 /{id}/xxx 之后）
     * 数据权限：本人可查看自己的预约；管理员/教师可查看其数据权限范围内的预约
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> getBooking(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接使用Mapper查询，绕过Service层的数据权限过滤
            BookingOrder booking = bookingOrderMapper.selectById(id);
            if (booking == null) {
                result.put("success", false);
                result.put("message", "预约不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }

            // 获取当前用户ID
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "无法获取用户信息，请重新登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }

            // 数据权限检查：允许本人查看自己的预约
            if (booking.getUserId() != null && currentUserId.equals(booking.getUserId())) {
                enrichBookingOrder(booking);
                result.put("success", true);
                result.put("data", booking);
                return ResponseEntity.ok(result);
            }

            // 非本人：检查是否有管理权限
            if (hasBookingViewPermission()) {
                enrichBookingOrder(booking);
                result.put("success", true);
                result.put("data", booking);
                return ResponseEntity.ok(result);
            }

            // 拒绝访问
            result.put("success", false);
            result.put("message", "无权查看该预约详情");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 检查是否有查看预约详情的数据权限
     */
    private boolean hasBookingViewPermission() {
        try {
            DataScopeContext ctx = dataScopeService.getCurrentDataScope();
            if (ctx == null) {
                return false;
            }
            // 系统管理员、实验室管理员可查看所有/范围内的预约
            if (ctx.isSystemAdmin() || ctx.hasAllScope()) {
                return true;
            }
            // 教师角色可查看学生的预约
            if ("TEACHER".equals(ctx.getUserType())) {
                return true;
            }
            // 实验室管理员
            if ("LAB_ADMIN".equals(ctx.getUserType())) {
                return true;
            }
            // 检查是否有预约查看权限
            if (ctx.canViewBooking()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            // 出错时默认不允许
            return false;
        }
    }

    /**
     * 取消预约
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            bookingOrderService.cancelBooking(id, userId);
            return ResponseEntity.ok("取消成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 管理员取消他人预约（路径变量写法，兼容旧客户端）
     */
    @RequestMapping(value = "/{id}/admin-cancel", method = {RequestMethod.PUT, RequestMethod.DELETE})
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> adminCancelBookingByPath(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        if (!hasBookingCancelOthersDataPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无取消他人预约的数据权限");
        }
        Long operatorId = getCurrentUserId();
        if (operatorId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            bookingOrderService.adminCancelBooking(id, operatorId, reason);
            return ResponseEntity.ok("取消成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 使用完成（预约人本人将已通过预约标记为已完成，并释放设备占用）
     */
    @PutMapping("/{id}/complete-use")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<String> completeBookingUse(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            bookingOrderService.completeBooking(id, userId);
            return ResponseEntity.ok("已标记使用完成");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 审核预约（管理员/教师）
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking-audit') or hasAuthority('booking:audit')")
    public ResponseEntity<String> auditBooking(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String opinion) {
        if (!hasBookingAuditDataPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无预约审核数据权限（请在「权限分配」中为该角色勾选「审核预约申请」）");
        }
        Long auditorId = getCurrentUserId();
        if (auditorId == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        try {
            BookingOrder booking = bookingOrderService.getById(id);
            bookingOrderService.auditBooking(id, auditorId, status, opinion);
            return ResponseEntity.ok("审核成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/batch-audit")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking-audit') or hasAuthority('booking:audit')")
    public ResponseEntity<Map<String, Object>> batchAudit(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        if (!hasBookingAuditDataPermission()) {
            res.put("message", "无预约审核数据权限（请在「权限分配」中为该角色勾选「审核预约申请」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        Object idsRaw = body.get("ids");
        Object statusRaw = body.get("status");
        if (!(idsRaw instanceof List) || statusRaw == null) {
            res.put("message", "参数不完整");
            return ResponseEntity.badRequest().body(res);
        }
        Integer status;
        try {
            status = Integer.valueOf(String.valueOf(statusRaw));
        } catch (Exception e) {
            res.put("message", "审核状态非法");
            return ResponseEntity.badRequest().body(res);
        }
        if (status == null || (status != 1 && status != 2)) {
            res.put("message", "审核状态仅支持1(通过)或2(拒绝)");
            return ResponseEntity.badRequest().body(res);
        }
        Long auditorId = getCurrentUserId();
        if (auditorId == null) {
            res.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(res);
        }
        int success = 0;
        int failed = 0;
        for (Object idRaw : (List<?>) idsRaw) {
            if (idRaw == null) {
                failed++;
                continue;
            }
            try {
                Long id = Long.valueOf(String.valueOf(idRaw));
                bookingOrderService.auditBooking(id, auditorId, status, null);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        res.put("success", success);
        res.put("failed", failed);
        res.put("message", "批量审核完成");
        return ResponseEntity.ok(res);
    }

    /**
     * 管理员调整预约时间（管理员直接修改已通过预约的时间）
     */
    @PutMapping("/{id}/adjust")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking-audit') or hasAuthority('booking:audit')")
    public ResponseEntity<Map<String, Object>> adjustBooking(
            @PathVariable Long id,
            @RequestParam String newDate,
            @RequestParam String newStartTime,
            @RequestParam String newEndTime,
            @RequestParam(required = false) String reason) {
        Map<String, Object> res = new HashMap<>();
        if (!hasBookingAuditDataPermission()) {
            res.put("message", "无预约审核/调度数据权限（需勾选「审核预约申请」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        BookingOrder booking = bookingOrderService.getById(id);
        if (booking == null) {
            res.put("message", "预约不存在");
            return ResponseEntity.badRequest().body(res);
        }
        if (booking.getStatus() == null || (booking.getStatus() != 0 && booking.getStatus() != 1)) {
            res.put("message", "仅待审核或已通过的预约可调整");
            return ResponseEntity.badRequest().body(res);
        }
        LocalDateTime bookingDate;
        try {
            bookingDate = parseAdjustBookingDate(newDate);
        } catch (Exception e) {
            res.put("message", "预约日期格式错误");
            return ResponseEntity.badRequest().body(res);
        }
        if (newStartTime == null || newStartTime.trim().isEmpty() || newEndTime == null || newEndTime.trim().isEmpty()) {
            res.put("message", "调整时间不能为空");
            return ResponseEntity.badRequest().body(res);
        }
        QueryWrapper<BookingOrder> conflictWrapper = new QueryWrapper<>();
        conflictWrapper.eq("device_id", booking.getDeviceId());
        conflictWrapper.apply("DATE(booking_date) = {0}", bookingDate.toLocalDate().toString());
        conflictWrapper.in("status", 0, 1);
        conflictWrapper.ne("id", id);
        conflictWrapper.and(w -> w.le("start_time", newEndTime).ge("end_time", newStartTime));
        boolean hasConflict = bookingOrderService.count(conflictWrapper) > 0;
        if (hasConflict) {
            res.put("message", "调整后时段存在冲突，请选择其他时间");
            return ResponseEntity.badRequest().body(res);
        }
        BookingOrder probe = new BookingOrder();
        probe.setUserId(booking.getUserId());
        probe.setDeviceId(booking.getDeviceId());
        probe.setBookingDate(bookingDate);
        probe.setStartTime(newStartTime.trim());
        probe.setEndTime(newEndTime.trim());
        try {
            int sm = parseAdjustHm(newStartTime);
            int em = parseAdjustHm(newEndTime);
            if (em <= sm) {
                res.put("message", "结束时间必须晚于开始时间");
                return ResponseEntity.badRequest().body(res);
            }
            probe.setDuration((em - sm) / 60.0);
        } catch (Exception e) {
            res.put("message", "时间格式错误");
            return ResponseEntity.badRequest().body(res);
        }
        try {
            bookingOrderService.validateGlobalBookingConstraints(probe, id);
        } catch (RuntimeException e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        booking.setBookingDate(bookingDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setDuration(probe.getDuration());
        if (reason != null && !reason.trim().isEmpty()) {
            String oldOpinion = booking.getAuditOpinion() != null ? booking.getAuditOpinion() : "";
            String append = "【时间调整】" + reason.trim();
            booking.setAuditOpinion(oldOpinion.isEmpty() ? append : (oldOpinion + "；" + append));
        }
        bookingOrderService.updateById(booking);
        res.put("message", "预约调整成功");
        return ResponseEntity.ok(res);
    }

    /**
     * 学生直接调整预约时间（适用于待审核的预约，直接修改时间并保持待审核状态）
     */
    @PutMapping("/{id}/student-adjust")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> studentAdjustBooking(
            @PathVariable Long id,
            @RequestParam String newDate,
            @RequestParam String newStartTime,
            @RequestParam String newEndTime,
            @RequestParam(required = false) String reason) {
        Map<String, Object> res = new HashMap<>();

        Long userId = getCurrentUserId();
        if (userId == null) {
            res.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(res);
        }

        BookingOrder booking = bookingOrderService.getById(id);
        if (booking == null) {
            res.put("message", "预约不存在");
            return ResponseEntity.badRequest().body(res);
        }

        // 检查是否是本人的预约
        if (!userId.equals(booking.getUserId())) {
            res.put("message", "无权修改他人的预约");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }

        // 只有待审核的预约可以直接调整
        if (booking.getStatus() == null || booking.getStatus() != 0) {
            res.put("message", "只有待审核的预约可以直接调整时间");
            return ResponseEntity.badRequest().body(res);
        }

        LocalDateTime bookingDate;
        try {
            bookingDate = parseAdjustBookingDate(newDate);
        } catch (Exception e) {
            res.put("message", "预约日期格式错误");
            return ResponseEntity.badRequest().body(res);
        }

        if (newStartTime == null || newStartTime.trim().isEmpty() || newEndTime == null || newEndTime.trim().isEmpty()) {
            res.put("message", "调整时间不能为空");
            return ResponseEntity.badRequest().body(res);
        }

        // 检查时间冲突
        QueryWrapper<BookingOrder> conflictWrapper = new QueryWrapper<>();
        conflictWrapper.eq("device_id", booking.getDeviceId());
        conflictWrapper.apply("DATE(booking_date) = {0}", bookingDate.toLocalDate().toString());
        conflictWrapper.in("status", 0, 1);
        conflictWrapper.ne("id", id);
        conflictWrapper.and(w -> w.le("start_time", newEndTime).ge("end_time", newStartTime));
        boolean hasConflict = bookingOrderService.count(conflictWrapper) > 0;
        if (hasConflict) {
            res.put("message", "调整后时段存在冲突，请选择其他时间");
            return ResponseEntity.badRequest().body(res);
        }

        BookingOrder probe = new BookingOrder();
        probe.setUserId(booking.getUserId());
        probe.setDeviceId(booking.getDeviceId());
        probe.setBookingDate(bookingDate);
        probe.setStartTime(newStartTime.trim());
        probe.setEndTime(newEndTime.trim());
        try {
            int sm = parseAdjustHm(newStartTime);
            int em = parseAdjustHm(newEndTime);
            if (em <= sm) {
                res.put("message", "结束时间必须晚于开始时间");
                return ResponseEntity.badRequest().body(res);
            }
            probe.setDuration((em - sm) / 60.0);
        } catch (Exception e) {
            res.put("message", "时间格式错误");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            bookingOrderService.validateGlobalBookingConstraints(probe, id);
        } catch (RuntimeException e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }

        // 更新预约信息，保持待审核状态
        booking.setBookingDate(bookingDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setDuration(probe.getDuration());
        if (reason != null && !reason.trim().isEmpty()) {
            String oldOpinion = booking.getAuditOpinion() != null ? booking.getAuditOpinion() : "";
            String append = "【学生调整】" + reason.trim();
            booking.setAuditOpinion(oldOpinion.isEmpty() ? append : (oldOpinion + "；" + append));
        }
        // 待审核状态不变，重新提交审核
        booking.setStatus(0);
        bookingOrderService.updateById(booking);

        res.put("message", "预约时间已调整，重新提交审核");
        return ResponseEntity.ok(res);
    }

    /**
     * 待审核列表（管理员/教师）- 带数据权限过滤
     * - 系统管理员：可查看所有待审核预约
     * - 实验室管理员：可查看所有待审核预约（符合论文"可查看所有预约"）
     * - 教师：可查看本部门的待审核预约
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAnyRole('LAB_ADMIN','TEACHER') or hasAuthority('booking-audit') or hasAuthority('booking:audit')")
    public ResponseEntity<Map<String, Object>> pendingAudits(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!hasBookingAuditDataPermission()) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "无预约审核数据权限（请在「权限分配」中为该角色勾选「审核预约申请」）");
            err.put("list", Collections.emptyList());
            err.put("total", 0);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        // 获取数据权限上下文
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        
        // 构建查询条件
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0); // 待审核
        
        // 应用数据权限过滤（LAB_ADMIN可查看所有待审核预约，符合论文要求）
        if (scopeContext != null && !scopeContext.isSystemAdmin() && !scopeContext.hasAllScope()
                && !"LAB_ADMIN".equals(scopeContext.getUserType())) {
            if ("TEACHER".equals(scopeContext.getUserType())) {
                applyTeacherLabFilter(wrapper, scopeContext);
            } else {
                applyBookingDataScope(wrapper, scopeContext);
            }
        }
        
        wrapper.orderByDesc("create_time");
        
        Page<BookingOrder> page = new Page<>(pageNum, pageSize);
        Page<BookingOrder> result = bookingOrderService.page(page, wrapper);
        
        // 填充设备名称等信息
        for (BookingOrder b : result.getRecords()) {
            enrichBookingOrder(b);
        }
        
        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        return ResponseEntity.ok(res);
    }
    
    /**
     * 预约全局列表（审核/调度用）
     * - 系统管理员：全部预约
     * - 实验室管理员：按数据权限（通常为本实验室）
     * - 教师：仅查看学生提交的预约（用于学生预约审核页）
     */
    @GetMapping("/global-list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER') or hasAuthority('booking-audit') or hasAuthority('booking:audit') or hasAuthority('booking-list')")
    public ResponseEntity<Map<String, Object>> globalBookingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String auditStatus) {
        Page<BookingOrder> result = pageGlobalListQuery(pageNum, pageSize, orderNo, deviceName, userName, status, auditStatus);
        for (BookingOrder b : result.getRecords()) {
            enrichBookingOrder(b);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        return ResponseEntity.ok(res);
    }

    /**
     * 与 global-list 相同的过滤与数据范围（供导出等复用）
     */
    private Page<BookingOrder> pageGlobalListQuery(
            int pageNum,
            int pageSize,
            String orderNo,
            String deviceName,
            String userName,
            Integer status,
            String auditStatus) {
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like("order_no", orderNo);
        }
        if (deviceName != null && !deviceName.isEmpty()) {
            wrapper.like("device_id IN (SELECT id FROM device_info WHERE device_name LIKE '%" + deviceName + "%')", null);
        }
        if (userName != null && !userName.isEmpty()) {
            wrapper.like("user_id IN (SELECT id FROM sys_user WHERE real_name LIKE '%" + userName + "%' OR username LIKE '%" + userName + "%')", null);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (auditStatus != null) {
            wrapper.eq("status", auditStatus);
        }
        if (scopeContext != null && "TEACHER".equals(scopeContext.getUserType())) {
            // 教师按设备所属实验室过滤（而非学生部门），更符合实验室管理场景
            applyTeacherLabFilter(wrapper, scopeContext);
        } else if (scopeContext != null && !scopeContext.isSystemAdmin() && !scopeContext.hasAllScope()
                && !"LAB_ADMIN".equals(scopeContext.getUserType())) {
            applyBookingDataScope(wrapper, scopeContext);
        }
        wrapper.orderByDesc("create_time");
        Page<BookingOrder> page = new Page<>(pageNum, pageSize);
        return bookingOrderService.page(page, wrapper);
    }

    /** 与「权限分配」中预约数据权限 AUDIT 勾选一致 */
    private boolean hasBookingAuditDataPermission() {
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        return ctx != null && ctx.canAuditBooking();
    }

    /** 与「取消预约」数据权限勾选一致（管理员代取消他人预约） */
    private boolean hasBookingCancelOthersDataPermission() {
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        return ctx != null && ctx.canCancelBooking();
    }

    /** 与「导出预约数据」数据权限勾选一致 */
    private boolean hasBookingExportDataPermission() {
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        return ctx != null && ctx.canExportBooking();
    }
    
    /**
     * 解析调整预约的日期参数：支持 yyyy-MM-dd，以及前端常见的 ISO 串（如 2026-03-29T00:00:00）
     */
    private static LocalDateTime parseAdjustBookingDate(String newDate) {
        if (newDate == null) {
            throw new IllegalArgumentException("empty");
        }
        String s = newDate.trim();
        int t = s.indexOf('T');
        if (t > 0) {
            s = s.substring(0, t);
        }
        if (s.length() > 10) {
            s = s.substring(0, 10);
        }
        LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        return d.atStartOfDay();
    }

    /**
     * 教师专用过滤：按设备所属实验室过滤预约（而非按学生部门）
     * 教师属于某个实验室 → 可审核该实验室所有设备的预约，不管学生属于哪个部门
     */
    private void applyTeacherLabFilter(QueryWrapper<BookingOrder> wrapper, DataScopeContext context) {
        if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
            // 按设备所属实验室过滤：只显示该实验室设备的预约
            wrapper.exists("SELECT 1 FROM device_info d WHERE d.id = booking_order.device_id AND d.laboratory = '" + context.getLaboratory() + "'");
        } else {
            // 如果教师没有设置实验室，则只能看到自己的预约（安全降级）
            wrapper.eq("user_id", context.getUserId());
        }
    }

    /**
     * 应用预约数据权限过滤
     */
    private void applyBookingDataScope(QueryWrapper<BookingOrder> wrapper, DataScopeContext context) {
        if (!context.isSystemAdmin() && !context.hasAllScope() && !context.canViewBooking()) {
            wrapper.eq("1", 0);
            return;
        }
        String scopeType = context.getScopeType();

        switch (scopeType) {
            case "DEPT":
                // 部门级：通过设备所属实验室过滤（device_info 无 department 列，与 DataScopeService 一致）
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.exists("SELECT 1 FROM device_info d WHERE d.id = booking_order.device_id AND d.laboratory = '" + context.getLaboratory() + "'");
                } else {
                    wrapper.eq("user_id", context.getUserId());
                }
                break;
                
            case "SELF":
                // 自助级：只能查看自己的预约
                wrapper.eq("user_id", context.getUserId());
                break;
                
            case "CUSTOM":
                // 自定义：查看指定实验室的预约
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().isEmpty()) {
                    String[] labIds = context.getCustomLabIds().split(",");
                    StringBuilder sb = new StringBuilder("SELECT 1 FROM device_info d WHERE d.id = booking_order.device_id AND d.laboratory IN (");
                    for (int i = 0; i < labIds.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append("'").append(labIds[i].trim()).append("'");
                    }
                    sb.append(")");
                    wrapper.exists(sb.toString());
                }
                break;
                
            default:
                // 未知类型，默认只能看自己的
                wrapper.eq("user_id", context.getUserId());
                break;
        }
    }

    /**
     * 检测预约冲突
     */
    @GetMapping("/check-conflict")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> checkConflict(
            @RequestParam Long deviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime bookingDate,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        boolean hasConflict = bookingOrderService.checkConflict(deviceId, bookingDate, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", hasConflict);
        return ResponseEntity.ok(result);
    }

    /**
     * 检测预约冲突（增强版，返回详细冲突信息和替代设备建议）
     */
    @GetMapping("/check-conflict-detail")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> checkConflictWithDetails(
            @RequestParam Long deviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime bookingDate,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        Map<String, Object> result = bookingOrderService.checkConflictWithDetails(deviceId, bookingDate, startTime, endTime);
        return ResponseEntity.ok(result);
    }

    /**
     * 申请设备替换
     */
    @PutMapping("/{id}/replace-device")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER','STUDENT','MAINTAINER') or hasAuthority('booking') or hasAuthority('booking-list') or hasAuthority('booking:add') or hasAuthority('booking:cancel')")
    public ResponseEntity<Map<String, Object>> applyDeviceReplace(
            @PathVariable Long id,
            @RequestParam Long newDeviceId,
            @RequestParam(required = false) String reason) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            bookingOrderService.applyDeviceReplace(id, newDeviceId, reason, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "设备替换申请成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 将 HH:mm 转为当日从 0 点起的分钟数 */
    private static int parseAdjustHm(String timeStr) {
        String[] p = timeStr.trim().split(":");
        int h = Integer.parseInt(p[0].trim());
        int m = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
        return h * 60 + m;
    }

}
