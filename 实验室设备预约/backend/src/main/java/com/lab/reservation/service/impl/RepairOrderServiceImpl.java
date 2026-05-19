package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.RepairOrder;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.RepairOrderMapper;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.RepairOrderService;
import com.lab.reservation.service.SysMessageService;
import com.lab.reservation.service.SysUserService;
import com.lab.reservation.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 维修工单Service实现类
 */
@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    private static final Logger log = LoggerFactory.getLogger(RepairOrderServiceImpl.class);

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private SysMessageService sysMessageService;

    @Override
    @Transactional
    public boolean createRepairOrder(RepairOrder order) {
        if (order.getDeviceId() == null) {
            throw new RuntimeException("请选择设备");
        }
        DeviceInfo device = deviceInfoService.getById(order.getDeviceId());
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        order.setOrderNo(CommonUtil.generateWorkOrderNo());
        order.setStatus(0);
        if (order.getReportTime() == null) {
            order.setReportTime(LocalDateTime.now());
        }

        // 【新增】自动分配维护人员策略：优先分配当前待处理工单数最少的维护人员
        Long autoAssignedHandlerId = autoAssignMaintainer(device);
        if (autoAssignedHandlerId != null) {
            order.setHandlerId(autoAssignedHandlerId);
            order.setStatus(1); // 已被分配，直接进入处理中状态
            order.setHandleStartTime(LocalDateTime.now());
        }

        boolean saved = save(order);
        if (saved) {
            // 更新设备状态为维修中
            deviceInfoService.updateDeviceStatus(order.getDeviceId(), 2);

            // 【新增】自动分配成功后发送通知
            if (autoAssignedHandlerId != null) {
                notifyAutoAssignedHandler(order, device, autoAssignedHandlerId);
            }
        }
        return saved;
    }

    /**
     * 自动分配维护人员策略（轮询负载均衡）
     * 分配规则：
     * 1. 统计每个维护人员当前的活跃工单数（待处理+处理中）
     * 2. 优先分配给活跃工单数最少的维护人员
     * 3. 如果多个维护人员工单数相同，按ID轮流分配
     * 4. 如果没有可用的维护人员，返回null
     */
    private Long autoAssignMaintainer(DeviceInfo device) {
        log.info("[autoAssignMaintainer] 开始自动分配维护人员");

        QueryWrapper<SysUser> maintainerWrapper = new QueryWrapper<>();
        maintainerWrapper.eq("user_type", "MAINTAINER");
        maintainerWrapper.eq("status", 1);
        List<SysUser> maintainers = sysUserService.list(maintainerWrapper);

        if (maintainers == null || maintainers.isEmpty()) {
            log.warn("[autoAssignMaintainer] 未找到可用的维护人员");
            return null;
        }

        log.info("[autoAssignMaintainer] 找到 {} 个可用维护人员", maintainers.size());

        // 统计每个维护人员当前的活跃工单数（status=0待处理 + status=1处理中）
        Map<Long, Long> workloadMap = new HashMap<>();
        for (SysUser m : maintainers) {
            if (m.getId() != null) {
                QueryWrapper<RepairOrder> orderWrapper = new QueryWrapper<>();
                orderWrapper.eq("handler_id", m.getId());
                orderWrapper.in("status", 0, 1);
                long activeCount = count(orderWrapper);
                workloadMap.put(m.getId(), activeCount);
                log.info("[autoAssignMaintainer] 维护人员 {}(ID={}) 当前活跃工单数: {}", 
                    m.getRealName() != null ? m.getRealName() : m.getUsername(), m.getId(), activeCount);
            }
        }

        // 找到最小工单数
        long minWorkload = Long.MAX_VALUE;
        for (Long workload : workloadMap.values()) {
            if (workload < minWorkload) {
                minWorkload = workload;
            }
        }

        // 收集所有工单数最少的维护人员，按ID排序保证轮询顺序稳定
        final long finalMinWorkload = minWorkload;
        List<Long> candidates = maintainers.stream()
                .filter(m -> m.getId() != null && workloadMap.getOrDefault(m.getId(), 0L) == finalMinWorkload)
                .map(SysUser::getId)
                .sorted()
                .collect(Collectors.toList());

        log.info("[autoAssignMaintainer] 最小工单数: {}, 候选维护人员: {}", minWorkload, candidates);

        if (candidates.isEmpty()) {
            return null;
        }

        // 轮询策略：查询最近一次分配的维护人员，选择下一个
        Long selectedHandlerId;
        if (candidates.size() == 1) {
            selectedHandlerId = candidates.get(0);
        } else {
            // 查询最近一条已分配的工单，获取上次分配的维护人员ID
            QueryWrapper<RepairOrder> lastOrderWrapper = new QueryWrapper<>();
            lastOrderWrapper.isNotNull("handler_id");
            lastOrderWrapper.orderByDesc("create_time");
            lastOrderWrapper.last("LIMIT 1");
            RepairOrder lastOrder = getOne(lastOrderWrapper, false);

            Long lastHandlerId = lastOrder != null ? lastOrder.getHandlerId() : null;
            log.info("[autoAssignMaintainer] 上次分配的维护人员ID: {}", lastHandlerId);

            // 找到上次分配的维护人员在候选列表中的位置，选择下一个
            int nextIndex = 0;
            if (lastHandlerId != null) {
                int lastIndex = candidates.indexOf(lastHandlerId);
                if (lastIndex >= 0) {
                    nextIndex = (lastIndex + 1) % candidates.size();
                }
            }

            selectedHandlerId = candidates.get(nextIndex);
        }

        log.info("[autoAssignMaintainer] 最终选择维护人员ID: {}, 当前活跃工单数: {}", 
            selectedHandlerId, workloadMap.getOrDefault(selectedHandlerId, 0L));
        return selectedHandlerId;
    }

    /**
     * 【新增】通知被自动分配的维护人员
     */
    private void notifyAutoAssignedHandler(RepairOrder order, DeviceInfo device, Long handlerId) {
        if (handlerId == null) return;

        SysUser handler = sysUserService.getById(handlerId);
        SysUser reporter = sysUserService.getById(order.getReporterId());
        String deviceName = device != null ? device.getDeviceName() : "";
        String reporterName = reporter != null
                ? (reporter.getRealName() != null && !reporter.getRealName().isEmpty() ? reporter.getRealName() : reporter.getUsername())
                : "";

        String title = "系统自动分配工单";
        String content = String.format(
                "系统自动分配给您一项维修工单，请及时处理。\n" +
                "设备：%s\n" +
                "报修人：%s\n" +
                "故障描述：%s\n" +
                "工单编号：%s",
                deviceName,
                reporterName,
                order.getFaultDescription() != null ? order.getFaultDescription().substring(0, Math.min(50, order.getFaultDescription().length())) : "",
                order.getOrderNo()
        );

        sysMessageService.sendMessage(handlerId, "REPAIR_ASSIGN", title, content, order.getId(), "repair_order");
    }

    @Override
    @Transactional
    public boolean handleOrder(Long id, Long handlerId) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("仅待处理工单可分配");
        }
        order.setHandlerId(handlerId);
        order.setStatus(1);
        order.setHandleStartTime(LocalDateTime.now());
        boolean updated = updateById(order);
        if (updated) {
            // 发送工单分配通知给维护人员
            SysUser handler = sysUserService.getById(handlerId);
            SysUser reporter = sysUserService.getById(order.getReporterId());
            DeviceInfo device = deviceInfoService.getById(order.getDeviceId());
            String deviceName = device != null ? device.getDeviceName() : "";
            String reporterName = reporter != null
                    ? (reporter.getRealName() != null && !reporter.getRealName().isEmpty() ? reporter.getRealName() : reporter.getUsername())
                    : "";
            String title = "工单分配通知";
            String content = "您有一项新的工单待处理。设备：" + deviceName + "，报修人：" + reporterName;
            sysMessageService.sendMessage(handlerId, "REPAIR_ASSIGN", title, content, order.getId(), "repair_order");
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean acceptOrder(Long id, Long maintainerUserId) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("仅待处理工单可接单");
        }
        if (order.getHandlerId() != null) {
            throw new RuntimeException("该工单已由管理员分配，请等待指派或联系管理员");
        }
        order.setHandlerId(maintainerUserId);
        order.setStatus(1);
        order.setHandleStartTime(LocalDateTime.now());
        boolean updated = updateById(order);
        if (updated) {
            // 发送工单接单通知
            SysUser reporter = sysUserService.getById(order.getReporterId());
            DeviceInfo device = deviceInfoService.getById(order.getDeviceId());
            String deviceName = device != null ? device.getDeviceName() : "";
            String reporterName = reporter != null
                    ? (reporter.getRealName() != null && !reporter.getRealName().isEmpty() ? reporter.getRealName() : reporter.getUsername())
                    : "";
            String title = "工单已被接单";
            String content = "您报修的设备（" + deviceName + "）已被维护人员接单，请等待处理。";
            if (order.getReporterId() != null) {
                sysMessageService.sendMessage(order.getReporterId(), "REPAIR_ASSIGN", title, content, order.getId(), "repair_order");
            }
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean completeOrder(Long id, String faultCause, String repairSolution, Double cost,
                                 Long operatorUserId, String operatorUserType) {
        RepairOrder order = getById(id);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("仅处理中的工单可完成");
        }
        if ("MAINTAINER".equals(operatorUserType) && operatorUserId != null) {
            if (order.getHandlerId() == null || !order.getHandlerId().equals(operatorUserId)) {
                throw new RuntimeException("仅能完成本人负责的工单");
            }
        }
        order.setFaultCause(faultCause);
        order.setRepairSolution(repairSolution);
        order.setRepairCost(cost);
        order.setStatus(2);
        order.setHandleEndTime(LocalDateTime.now());
        boolean ok = updateById(order);
        if (ok && order.getDeviceId() != null) {
            bookingOrderService.syncDeviceStatusWithBookings(order.getDeviceId());
            // 发送工单完成通知给报修人
            if (order.getReporterId() != null) {
                DeviceInfo device = deviceInfoService.getById(order.getDeviceId());
                String deviceName = device != null ? device.getDeviceName() : "";
                String title = "工单处理完成";
                String content = "您报修的设备（" + deviceName + "）已维修完成，请前往确认。故障原因：" + faultCause + "，解决方案：" + repairSolution;
                sysMessageService.sendMessage(order.getReporterId(), "REPAIR_ASSIGN", title, content, order.getId(), "repair_order");
            }
        }
        return ok;
    }

    @Override
    public Page<RepairOrder> pageOrders(Integer pageNum, Integer pageSize, Integer status, Long deviceId,
                                        Long viewerUserId, String viewerUserType) {
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        QueryWrapper<RepairOrder> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (deviceId != null) {
            wrapper.eq("device_id", deviceId);
        }
        if ("MAINTAINER".equals(viewerUserType) && viewerUserId != null) {
            wrapper.eq("handler_id", viewerUserId);
        }
        wrapper.orderByDesc("create_time");
        Page<RepairOrder> result = page(page, wrapper);
        for (RepairOrder r : result.getRecords()) {
            DeviceInfo d = deviceInfoService.getById(r.getDeviceId());
            if (d != null) {
                r.setDeviceName(d.getDeviceName());
                r.setDeviceNo(d.getDeviceNo());
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
        }
        return result;
    }

    @Override
    public Page<RepairOrder> pageRepairRecords(Integer pageNum, Integer pageSize, Integer status,
                                               String orderNoKeyword, String deviceNameKeyword,
                                               LocalDate reportDateFrom, LocalDate reportDateTo,
                                               Long viewerUserId, String viewerUserType) {
        Page<RepairOrder> page = new Page<>(pageNum, pageSize);
        QueryWrapper<RepairOrder> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        } else {
            wrapper.in("status", 2, 3);
        }
        if (StringUtils.hasText(orderNoKeyword)) {
            wrapper.like("order_no", orderNoKeyword.trim());
        }
        if (StringUtils.hasText(deviceNameKeyword)) {
            wrapper.apply("device_id IN (SELECT id FROM device_info WHERE deleted = 0 AND device_name LIKE {0})",
                    "%" + deviceNameKeyword.trim() + "%");
        }
        if (reportDateFrom != null) {
            wrapper.ge("report_time", reportDateFrom.atStartOfDay());
        }
        if (reportDateTo != null) {
            wrapper.le("report_time", reportDateTo.atTime(23, 59, 59));
        }
        if ("MAINTAINER".equals(viewerUserType) && viewerUserId != null) {
            wrapper.eq("handler_id", viewerUserId);
        }
        wrapper.orderByDesc("handle_end_time");
        wrapper.orderByDesc("create_time");
        Page<RepairOrder> result = page(page, wrapper);
        for (RepairOrder r : result.getRecords()) {
            DeviceInfo d = deviceInfoService.getById(r.getDeviceId());
            if (d != null) {
                r.setDeviceName(d.getDeviceName());
                r.setDeviceNo(d.getDeviceNo());
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
        }
        return result;
    }
}
