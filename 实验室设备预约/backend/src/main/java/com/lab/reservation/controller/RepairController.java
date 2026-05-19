package com.lab.reservation.controller;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.lab.reservation.config.FileUploadConfig;

import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.RepairOrder;

import com.lab.reservation.entity.SysUser;

import com.lab.reservation.service.RepairOrderService;

import com.lab.reservation.service.DeviceInfoService;

import com.lab.reservation.service.SysUserService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;



import java.io.File;
import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.stream.Collectors;

import java.time.LocalDateTime;



/**
 * 维修工单控制器
 */
@RestController
@RequestMapping("/repair")
@CrossOrigin
public class RepairController {



    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private FileUploadConfig fileUploadConfig;



    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return sysUserService.getByUsername(authentication.getName());
    }



    private Long getCurrentUserId() {
        SysUser u = getCurrentUser();
        return u != null ? u.getId() : null;
    }



    /**
     * 上传故障现场图片（可选；与功能计划「上传故障图片」一致）
     */
    @PostMapping("/upload-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadRepairImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> body = new HashMap<>();
        if (file == null || file.isEmpty()) {
            body.put("message", "请选择图片文件");
            return ResponseEntity.badRequest().body(body);
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            body.put("message", "文件名不合法");
            return ResponseEntity.badRequest().body(body);
        }
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            body.put("message", "仅支持 jpg、jpeg、png、gif、webp 图片");
            return ResponseEntity.badRequest().body(body);
        }
        try {
            String base = fileUploadConfig.getUploadPath();
            if (base == null || base.trim().isEmpty()) {
                body.put("message", "文件上传路径未配置");
                return ResponseEntity.badRequest().body(body);
            }
            
            // 处理相对路径，转换为绝对路径
            if (!new File(base).isAbsolute()) {
                // 获取项目根目录
                String projectRoot = System.getProperty("user.dir");
                base = projectRoot + File.separator + base;
            }
            
            // 标准化路径（Windows 路径处理）
            base = base.replace("\\", "/").trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            
            Path uploadDir = Paths.get(base, "repair");
            
            // 确保目录存在
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 检查目录是否可写
            if (!Files.isWritable(uploadDir)) {
                body.put("message", "上传目录无写入权限：" + uploadDir.toString());
                return ResponseEntity.badRequest().body(body);
            }
            
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(name);
            
            // 写入文件
            file.transferTo(target.toFile());
            
            String relative = "/uploads/repair/" + name;
            body.put("path", relative);
            body.put("message", "上传成功");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "保存文件失败：" + e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(body);
        }
    }



    /**
     * 报修
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> create(@RequestBody RepairOrder order) {
        Long reporterId = getCurrentUserId();
        if (reporterId != null) {
            order.setReporterId(reporterId);
        }
        repairOrderService.createRepairOrder(order);
        return ResponseEntity.ok("报修成功");
    }



    /**
     * 工单列表（维护人员：待接单未指派 + 本人处理中/已完成；管理员：全部）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-record')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deviceId) {
        SysUser viewer = getCurrentUser();
        Long viewerId = viewer != null ? viewer.getId() : null;
        String viewerType = viewer != null ? viewer.getUserType() : null;
        Page<RepairOrder> page = repairOrderService.pageOrders(pageNum, pageSize, status, deviceId, viewerId, viewerType);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<Map<String, Object>> pendingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<RepairOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0);
        wrapper.isNull("handler_id");
        wrapper.orderByDesc("create_time");
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        Page<RepairOrder> resultPage = repairOrderService.page(page, wrapper);
        List<RepairOrder> records = resultPage.getRecords();
        for (RepairOrder r : records) {
            if (r.getDeviceId() != null) {
                DeviceInfo d = deviceInfoService.getById(r.getDeviceId());
                if (d != null) {
                    r.setDeviceName(d.getDeviceName());
                    r.setDeviceNo(d.getDeviceNo());
                }
            }
            if (r.getReporterId() != null) {
                SysUser u = sysUserService.getById(r.getReporterId());
                if (u != null) {
                    r.setReporterName(u.getRealName() != null && !u.getRealName().isEmpty()
                            ? u.getRealName() : u.getUsername());
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", resultPage.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/maintainers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<List<Map<String, Object>>> maintainers() {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("user_type", "MAINTAINER");
        wrapper.eq("status", 1);
        List<SysUser> maintainers = sysUserService.list(wrapper);
        List<Map<String, Object>> result = maintainers.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", (u.getRealName() != null && !u.getRealName().isEmpty()) ? u.getRealName() : u.getUsername());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<Map<String, Object>> assign(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Object idRaw = body.get("id");
        Object maintainerIdRaw = body.get("maintainerId");
        if (idRaw == null || maintainerIdRaw == null) {
            result.put("message", "参数不完整");
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Long id = Long.valueOf(String.valueOf(idRaw));
            Long maintainerId = Long.valueOf(String.valueOf(maintainerIdRaw));
            repairOrderService.handleOrder(id, maintainerId);
            result.put("message", "分配成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取工单详情（全局监控用，SYSTEM_ADMIN 可见全部字段）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-record') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "工单不存在");
            return ResponseEntity.status(404).body(err);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("deviceId", order.getDeviceId());
        DeviceInfo d = deviceInfoService.getById(order.getDeviceId());
        if (d != null) {
            result.put("deviceName", d.getDeviceName());
            result.put("deviceNo", d.getDeviceNo());
        }
        result.put("faultDescription", order.getFaultDescription());
        result.put("imagePath", order.getImagePath());
        result.put("status", order.getStatus());
        result.put("priority", order.getPriority() != null ? order.getPriority() : "NORMAL");
        result.put("createTime", order.getCreateTime());
        result.put("reportTime", order.getReportTime());
        // 报修人
        if (order.getReporterId() != null) {
            SysUser reporter = sysUserService.getById(order.getReporterId());
            if (reporter != null) {
                result.put("reporter", reporter.getRealName() != null && !reporter.getRealName().isEmpty()
                        ? reporter.getRealName() : reporter.getUsername());
            }
        }
        // 处理人
        if (order.getHandlerId() != null) {
            SysUser handler = sysUserService.getById(order.getHandlerId());
            if (handler != null) {
                result.put("assignee", handler.getRealName() != null && !handler.getRealName().isEmpty()
                        ? handler.getRealName() : handler.getUsername());
                result.put("assignTime", order.getHandleStartTime());
            }
        }
        result.put("faultCause", order.getFaultCause());
        result.put("repairSolution", order.getRepairSolution());
        result.put("replaceParts", order.getReplaceParts());
        result.put("repairCost", order.getRepairCost());
        result.put("completeTime", order.getHandleEndTime());
        return ResponseEntity.ok(result);
    }

    /**
     * 重新指派工单（可更改处理人或切换待接单池）
     */
    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<String> reassign(@PathVariable Long id,
            @RequestParam(required = false) String assigneeId) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            return ResponseEntity.status(404).body("工单不存在");
        }
        // 前端传 assigneeId=null 时 axios 拼为 "null" 字符串，需兜底处理
        Long handlerId = null;
        if (assigneeId != null && !"null".equalsIgnoreCase(assigneeId) && !assigneeId.trim().isEmpty()) {
            try {
                handlerId = Long.parseLong(assigneeId.trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("无效的处理人ID");
            }
        }
        order.setHandlerId(handlerId);
        if (handlerId != null) {
            order.setStatus(1);
            if (order.getHandleStartTime() == null) {
                order.setHandleStartTime(LocalDateTime.now());
            }
        } else {
            order.setStatus(0);
            order.setHandleStartTime(null);
        }
        repairOrderService.updateById(order);
        return ResponseEntity.ok("指派成功");
    }

    /**
     * 调整工单优先级
     */
    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<String> adjustPriority(@PathVariable Long id, @RequestParam String priority) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            return ResponseEntity.status(404).body("工单不存在");
        }
        if (priority == null || priority.isEmpty()) {
            priority = "NORMAL";
        }
        order.setPriority(priority);
        repairOrderService.updateById(order);
        return ResponseEntity.ok("优先级已更新");
    }

    /**
     * 全局工单列表（系统管理员监控视图，返回全量数据 + 统计）
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('repair-global-monitor') or hasAuthority('repair')")
    public ResponseEntity<Map<String, Object>> globalList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignee) {

        // 第一步：按处理人关键词查出匹配的用户ID列表
        List<Long> matchedUserIds = null;
        if (assignee != null && !assignee.trim().isEmpty()) {
            QueryWrapper<SysUser> userWrapper = new QueryWrapper<>();
            userWrapper.and(w -> w.like("real_name", assignee.trim())
                    .or().like("username", assignee.trim()))
                    .eq("user_type", "MAINTAINER");
            List<SysUser> matchedUsers = sysUserService.list(userWrapper);
            matchedUserIds = matchedUsers.stream().map(u -> u.getId()).collect(Collectors.toList());
        }

        // 第二步：查出所有工单用于后续过滤（数据量可控，维修工单通常不过千）
        QueryWrapper<RepairOrder> allWrapper = new QueryWrapper<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            allWrapper.like("order_no", orderNo.trim());
        }
        if (status != null) {
            allWrapper.eq("status", status);
        }
        if (priority != null && !priority.trim().isEmpty()) {
            allWrapper.eq("priority", priority.trim());
        }
        if (matchedUserIds != null && !matchedUserIds.isEmpty()) {
            allWrapper.in("handler_id", matchedUserIds);
        } else if (matchedUserIds != null) {
            allWrapper.eq("handler_id", -1L);
        }
        allWrapper.orderByDesc("create_time");
        List<RepairOrder> allOrders = repairOrderService.list(allWrapper);

        // 第三步：前端关联数据并做内存过滤（设备名称）
        List<RepairOrder> filtered = new java.util.ArrayList<>();
        for (RepairOrder r : allOrders) {
            if (r.getDeviceId() != null) {
                DeviceInfo d = deviceInfoService.getById(r.getDeviceId());
                if (d != null) {
                    r.setDeviceName(d.getDeviceName());
                    r.setDeviceNo(d.getDeviceNo());
                    if (deviceName != null && !deviceName.trim().isEmpty()
                            && !d.getDeviceName().contains(deviceName.trim())) {
                        continue;
                    }
                }
            }
            if (r.getReporterId() != null) {
                SysUser u = sysUserService.getById(r.getReporterId());
                if (u != null) {
                    r.setReporterName(u.getRealName() != null && !u.getRealName().isEmpty()
                            ? u.getRealName() : u.getUsername());
                }
            }
            if (r.getHandlerId() != null) {
                SysUser u = sysUserService.getById(r.getHandlerId());
                if (u != null) {
                    r.setHandlerName(u.getRealName() != null && !u.getRealName().isEmpty()
                            ? u.getRealName() : u.getUsername());
                }
            }
            // 全局监控「剩余时间」：无库表字段时按 SLA 推算截止时间
            if (r.getStatus() != null && (r.getStatus() == 0 || r.getStatus() == 1)) {
                LocalDateTime base;
                if (r.getStatus() == 0) {
                    base = r.getCreateTime() != null ? r.getCreateTime() : r.getReportTime();
                } else {
                    base = r.getHandleStartTime() != null ? r.getHandleStartTime()
                            : (r.getCreateTime() != null ? r.getCreateTime() : r.getReportTime());
                }
                if (base != null) {
                    int slaHours = r.getStatus() == 0 ? 48 : 72;
                    r.setDeadline(base.plusHours(slaHours));
                }
            } else {
                r.setDeadline(null);
            }
            filtered.add(r);
        }

        // 第四步：分页
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<RepairOrder> paged = fromIndex < total ? filtered.subList(fromIndex, toIndex) : java.util.Collections.emptyList();

        // 第五步：统计
        LocalDateTime now = LocalDateTime.now();

        // 超时工单数：待接单超过48h，或处理中超过72h（处理开始时间起算；无开始时间则从创建时间起算）
        int overdueCount = 0;
        // 查所有未完成工单（数量可控）
        List<RepairOrder> activeOrders = repairOrderService.list(
            new QueryWrapper<RepairOrder>().in("status", 0, 1)
        );
        for (RepairOrder o : activeOrders) {
            LocalDateTime base = null;
            int slaHours;
            if (o.getStatus() == 0) {
                base = o.getCreateTime() != null ? o.getCreateTime()
                        : (o.getReportTime() != null ? o.getReportTime() : null);
                slaHours = 48;
            } else {
                base = o.getHandleStartTime() != null ? o.getHandleStartTime()
                        : (o.getCreateTime() != null ? o.getCreateTime()
                        : (o.getReportTime() != null ? o.getReportTime() : null));
                slaHours = 72;
            }
            if (base != null && base.plusHours(slaHours).isBefore(now)) {
                overdueCount++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repairOrderService.count());
        stats.put("pending", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("status", 0)));
        stats.put("processing", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("status", 1)));
        stats.put("completed", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("status", 2)));
        stats.put("overdue", overdueCount);

        Map<String, Object> response = new HashMap<>();
        response.put("list", paged);
        response.put("total", total);
        response.put("stats", stats);
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员分配维护人员
     */
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-global-monitor')")
    public ResponseEntity<String> handle(@PathVariable Long id, @RequestParam Long handlerId) {
        repairOrderService.handleOrder(id, handlerId);
        return ResponseEntity.ok("已分配处理人");
    }

    /**
     * 维护人员接单（待处理且未分配时；与测试用例 RM-002 一致）
     */
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('MAINTAINER') or hasAuthority('repair:handle')")
    public ResponseEntity<String> accept(@PathVariable Long id) {
        Long uid = getCurrentUserId();
        if (uid == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        repairOrderService.acceptOrder(id, uid);
        return ResponseEntity.ok("接单成功");
    }

    /**
     * 完成工单
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair:complete')")
    public ResponseEntity<String> complete(
            @PathVariable Long id,
            @RequestParam String faultCause,
            @RequestParam String repairSolution,
            @RequestParam(required = false) Double cost) {
        SysUser u = getCurrentUser();
        if (u == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }
        repairOrderService.completeOrder(id, faultCause, repairSolution, cost, u.getId(), u.getUserType());
        return ResponseEntity.ok("工单已完成");
    }
}
