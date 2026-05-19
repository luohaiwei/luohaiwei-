package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.DeviceStatusLog;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.DeviceInfoMapper;
import com.lab.reservation.mapper.DeviceStatusLogMapper;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备Service实现类
 */
@Service
public class DeviceInfoServiceImpl extends ServiceImpl<DeviceInfoMapper, DeviceInfo> implements DeviceInfoService {

    private static final Logger log = LoggerFactory.getLogger(DeviceInfoServiceImpl.class);

    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 设备编号全局唯一（与 device_info.device_no UNIQUE 一致）
     */
    private void assertDeviceNoUnique(DeviceInfo entity) {
        if (entity == null || entity.getDeviceNo() == null || entity.getDeviceNo().trim().isEmpty()) {
            return;
        }
        String no = entity.getDeviceNo().trim();
        QueryWrapper<DeviceInfo> w = new QueryWrapper<>();
        w.eq("device_no", no);
        if (entity.getId() != null) {
            w.ne("id", entity.getId());
        }
        if (count(w) > 0) {
            throw new RuntimeException("设备编号已存在，请更换其他编号");
        }
    }

    @Override
    public boolean save(DeviceInfo entity) {
        assertDeviceNoUnique(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(DeviceInfo entity) {
        // 仅当本次提交包含编号时才校验（避免仅改状态等局部更新误伤）
        if (entity != null && entity.getDeviceNo() != null && !entity.getDeviceNo().trim().isEmpty()) {
            assertDeviceNoUnique(entity);
        }
        Integer newStatus = entity != null ? entity.getStatus() : null;
        Long id = entity != null ? entity.getId() : null;
        // 仅当请求体显式携带 status 时，才在更新后比对并写设备状态变更日志（与 PUT /device/{id}/status 一致）
        DeviceInfo before = (id != null && newStatus != null) ? getById(id) : null;
        boolean ok = super.updateById(entity);
        if (ok && before != null && newStatus != null) {
            Integer oldStatus = before.getStatus();
            if (oldStatus != null && !oldStatus.equals(newStatus)) {
                DeviceInfo after = getById(id);
                saveStatusLog(id, after != null ? after : before, oldStatus, newStatus, null);
            }
        }
        return ok;
    }

    @Override
    public Page<DeviceInfo> pageDevices(Integer pageNum, Integer pageSize, DeviceInfo deviceInfo) {
        Page<DeviceInfo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        if (deviceInfo != null) {
            if (deviceInfo.getDeviceName() != null) {
                wrapper.like("device_name", deviceInfo.getDeviceName());
            }
            if (deviceInfo.getCategoryId() != null) {
                wrapper.eq("category_id", deviceInfo.getCategoryId());
            }
            if (deviceInfo.getStatus() != null) {
                wrapper.eq("status", deviceInfo.getStatus());
            }
            if (deviceInfo.getLaboratory() != null) {
                wrapper.like("laboratory", deviceInfo.getLaboratory());
            }
        }
        wrapper.orderByDesc("create_time");
        return page(page, wrapper);
    }

    @Override
    public List<DeviceInfo> listDevicesWithCategory() {
        return baseMapper.selectDeviceWithCategory();
    }

    @Override
    public DeviceInfo getDeviceDetail(Long id) {
        return baseMapper.selectDeviceWithCategoryById(id);
    }

    @Override
    public boolean updateDeviceStatus(Long id, Integer status) {
        DeviceInfo device = new DeviceInfo();
        device.setId(id);
        device.setStatus(status);
        // 日志由 updateById 在状态实际变化时统一写入，避免重复插入
        return updateById(device);
    }

    /**
     * 记录设备状态变更日志
     */
    private void saveStatusLog(Long deviceId, DeviceInfo device, Integer oldStatus, Integer newStatus, String reason) {
        try {
            DeviceStatusLog log = new DeviceStatusLog();
            log.setDeviceId(deviceId);
            if (device != null) {
                log.setDeviceNo(device.getDeviceNo());
                log.setDeviceName(device.getDeviceName());
            }
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setChangeReason(reason);
            log.setOperatorIp(resolveHttpClientIp());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                SysUser user = sysUserService.getByUsername(auth.getName());
                if (user != null) {
                    log.setOperatorId(user.getId());
                    log.setOperatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
            deviceStatusLogMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程（如库表缺列等），便于排查
            log.warn("写入设备状态变更日志失败: deviceId={}", deviceId, e);
        }
    }

    @Override
    public long countByStatus(Integer status) {
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        return count(wrapper);
    }

    @Override
    public double calculateUsageRate() {
        long total = count();
        if (total == 0) return 0;
        long using = countByStatus(1); // 使用中
        return (double) using / total * 100;
    }

    @Override
    public List<Map<String, Object>> getDeviceTypeDistribution() {
        return baseMapper.selectDeviceTypeDistribution();
    }

    @Override
    public List<DeviceStatusLog> getStatusLogs(Long deviceId) {
        if (deviceId == null) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper<DeviceStatusLog> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        wrapper.orderByDesc("create_time");
        return deviceStatusLogMapper.selectList(wrapper);
    }

    private static String statusToText(Integer s) {
        if (s == null) {
            return "-";
        }
        String[] names = {"空闲", "使用中", "维修中", "校准中", "报废"};
        return s >= 0 && s < names.length ? names[s] : String.valueOf(s);
    }

    private static String resolveHttpClientIp() {
        try {
            javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                    .resolveReference(javax.servlet.http.HttpServletRequest.class.getName());
            if (req == null) {
                return null;
            }
            String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
            for (String h : headers) {
                String ip = req.getHeader(h);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.split(",")[0].trim();
                }
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private static String newStatusToChangeType(Integer newStatus) {
        if (newStatus == null) {
            return "";
        }
        switch (newStatus) {
            case 0: return "IDLE";
            case 1: return "IN_USE";
            case 2: return "MAINTAINING";
            case 3: return "CALIBRATING";
            case 4: return "SCRAPPED";
            default: return "";
        }
    }

    private static Integer changeTypeToNewStatus(String changeType) {
        if (!StringUtils.hasText(changeType)) {
            return null;
        }
        switch (changeType.trim()) {
            case "IDLE": return 0;
            case "IN_USE": return 1;
            case "MAINTAINING": return 2;
            case "CALIBRATING": return 3;
            case "SCRAPPED": return 4;
            default: return null;
        }
    }

    @Override
    public Map<String, Object> pageGlobalStatusLogs(Integer pageNum, Integer pageSize,
            String deviceName, String deviceNo, String changeType, String operator,
            LocalDate startDate, LocalDate endDate, String laboratoryFilter) {
        QueryWrapper<DeviceStatusLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(deviceName)) {
            wrapper.like("device_name", deviceName.trim());
        }
        if (StringUtils.hasText(deviceNo)) {
            wrapper.like("device_no", deviceNo.trim());
        }
        Integer ns = changeTypeToNewStatus(changeType);
        if (ns != null) {
            wrapper.eq("new_status", ns);
        }
        if (StringUtils.hasText(operator)) {
            wrapper.like("operator_name", operator.trim());
        }
        if (startDate != null) {
            wrapper.ge("create_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("create_time", endDate.atTime(23, 59, 59));
        }

        // 数据权限过滤：LAB_ADMIN等角色只显示本实验室设备的日志
        if (laboratoryFilter != null && !laboratoryFilter.isEmpty()) {
            wrapper.inSql("device_id", "SELECT id FROM device_info WHERE laboratory = '" + laboratoryFilter + "' AND deleted = 0");
        }

        wrapper.orderByDesc("create_time");

        Page<DeviceStatusLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        Page<DeviceStatusLog> result = deviceStatusLogMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (DeviceStatusLog log : result.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", log.getId());
            row.put("deviceName", log.getDeviceName());
            row.put("deviceNo", log.getDeviceNo());
            row.put("changeType", newStatusToChangeType(log.getNewStatus()));
            row.put("fromStatus", statusToText(log.getOldStatus()));
            row.put("toStatus", statusToText(log.getNewStatus()));
            row.put("fromStatusText", statusToText(log.getOldStatus()));
            row.put("toStatusText", statusToText(log.getNewStatus()));
            row.put("operator", log.getOperatorName());
            row.put("remark", log.getChangeReason());
            row.put("ipAddress", log.getOperatorIp());
            row.put("createTime", log.getCreateTime());
            list.add(row);
        }

        QueryWrapper<DeviceStatusLog> all = new QueryWrapper<>();
        if (StringUtils.hasText(deviceName)) {
            all.like("device_name", deviceName.trim());
        }
        if (StringUtils.hasText(deviceNo)) {
            all.like("device_no", deviceNo.trim());
        }
        if (ns != null) {
            all.eq("new_status", ns);
        }
        if (StringUtils.hasText(operator)) {
            all.like("operator_name", operator.trim());
        }
        if (startDate != null) {
            all.ge("create_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            all.le("create_time", endDate.atTime(23, 59, 59));
        }

        // 数据权限过滤：LAB_ADMIN等角色只显示本实验室设备的日志（与分页查询保持一致）
        if (laboratoryFilter != null && !laboratoryFilter.isEmpty()) {
            all.inSql("device_id", "SELECT id FROM device_info WHERE laboratory = '" + laboratoryFilter + "' AND deleted = 0");
        }
        // 统计变更历史中的去重设备数（按最新状态统计）
        // 先查出所有符合条件的状态变更日志，按设备分组取最新的那条
        List<DeviceStatusLog> forStats = deviceStatusLogMapper.selectList(all);
        // 使用LinkedHashMap保持插入顺序，用于去重
        java.util.LinkedHashMap<Long, DeviceStatusLog> latestByDevice = new java.util.LinkedHashMap<>();
        for (DeviceStatusLog log : forStats) {
            if (log.getDeviceId() != null) {
                // 保持第一个遇到的设备的最新记录（因为日志已按时间倒序）
                latestByDevice.putIfAbsent(log.getDeviceId(), log);
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("idle", 0);
        stats.put("inUse", 0);
        stats.put("maintaining", 0);
        stats.put("calibrating", 0);
        stats.put("scrapped", 0);
        for (DeviceStatusLog log : latestByDevice.values()) {
            Integer n = log.getNewStatus();
            if (n == null) {
                continue;
            }
            switch (n) {
                case 0: stats.put("idle", (Integer) stats.get("idle") + 1); break;
                case 1: stats.put("inUse", (Integer) stats.get("inUse") + 1); break;
                case 2: stats.put("maintaining", (Integer) stats.get("maintaining") + 1); break;
                case 3: stats.put("calibrating", (Integer) stats.get("calibrating") + 1); break;
                case 4: stats.put("scrapped", (Integer) stats.get("scrapped") + 1); break;
                default: break;
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("list", list);
        out.put("total", result.getTotal());
        out.put("stats", stats);
        return out;
    }

    @Override
    public List<DeviceInfo> searchByKeyword(String keyword) {
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.like("device_name", keyword)
            .or().like("device_no", keyword)
            .or().like("laboratory", keyword)
            .or().like("description", keyword)
            .or().like("model", keyword)
            .or().like("manufacturer", keyword));
        wrapper.eq("deleted", 0);
        wrapper.last("LIMIT 20");
        return baseMapper.selectList(wrapper);
    }
}
