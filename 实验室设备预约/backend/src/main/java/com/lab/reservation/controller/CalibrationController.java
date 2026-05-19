package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.CalibrationRecord;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.CalibrationRecordService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校准管理控制器（实验室管理员、设备维护人员）
 */
@RestController
@RequestMapping("/calibration")
@CrossOrigin
public class CalibrationController {

    @Autowired
    private CalibrationRecordService calibrationRecordService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserService sysUserService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SysUser user = sysUserService.getByUsername(auth.getName());
        return user != null ? user.getId() : null;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long deviceId) {
        Page<CalibrationRecord> page = new Page<>(pageNum, pageSize);
        QueryWrapper<CalibrationRecord> wrapper = new QueryWrapper<>();
        if (deviceId != null) wrapper.eq("device_id", deviceId);
        wrapper.orderByDesc("calibration_date");
        Page<CalibrationRecord> result = calibrationRecordService.page(page, wrapper);
        for (CalibrationRecord r : result.getRecords()) {
            DeviceInfo d = deviceInfoService.getById(r.getDeviceId());
            if (d != null) {
                r.setDeviceName(d.getDeviceName());
                r.setDeviceNo(d.getDeviceNo());
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        return res;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> add(@RequestBody CalibrationRecord record) {
        Map<String, Object> res = new HashMap<>();
        try {
            record.setCalibratorId(getCurrentUserId());
            record.setCalibrationDate(LocalDateTime.now());
            calibrationRecordService.save(record);
            DeviceInfo device = deviceInfoService.getById(record.getDeviceId());
            if (device != null && record.getNextCalibrationDate() != null) {
                device.setNextCalibrationDate(record.getNextCalibrationDate());
                deviceInfoService.updateById(device);
            }
            res.put("message", "校准记录添加成功");
        } catch (Exception e) {
            res.put("code", 400);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> update(@RequestBody CalibrationRecord record) {
        Map<String, Object> res = new HashMap<>();
        try {
            calibrationRecordService.updateById(record);
            res.put("message", "更新成功");
        } catch (Exception e) {
            res.put("code", 400);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            calibrationRecordService.removeById(id);
            res.put("message", "删除成功");
        } catch (Exception e) {
            res.put("code", 400);
            res.put("message", e.getMessage());
        }
        return res;
    }

    /**
     * 获取单条校准记录详情（校准报告）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> getReport(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        CalibrationRecord record = calibrationRecordService.getById(id);
        if (record == null) {
            res.put("code", 404);
            res.put("message", "记录不存在");
            return res;
        }
        DeviceInfo device = deviceInfoService.getById(record.getDeviceId());
        SysUser calibrator = record.getCalibratorId() != null ? sysUserService.getById(record.getCalibratorId()) : null;
        res.put("record", record);
        if (device != null) {
            res.put("deviceName", device.getDeviceName());
            res.put("deviceNo", device.getDeviceNo());
            res.put("deviceModel", device.getModel());
            res.put("manufacturer", device.getManufacturer());
            res.put("precisionLevel", device.getPrecisionLevel());
        }
        if (calibrator != null) {
            res.put("calibratorName", calibrator.getRealName() != null ? calibrator.getRealName() : calibrator.getUsername());
        }
        return res;
    }

    /**
     * 即将到期校准的设备列表（供前端提醒看板用）
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER')")
    public Map<String, Object> upcomingDevices(
            @RequestParam(defaultValue = "30") Integer days) {
        Map<String, Object> res = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        wrapper.le("next_calibration_date", deadline.atStartOfDay())
               .ge("next_calibration_date", today.atStartOfDay())
               .eq("deleted", 0)
               .orderByAsc("next_calibration_date");
        List<DeviceInfo> devices = deviceInfoService.list(wrapper);
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (DeviceInfo d : devices) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("deviceName", d.getDeviceName());
            item.put("deviceNo", d.getDeviceNo());
            item.put("nextCalibrationDate", d.getNextCalibrationDate());
            item.put("status", d.getStatus());
            if (d.getNextCalibrationDate() != null) {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, d.getNextCalibrationDate().toLocalDate());
                item.put("daysLeft", daysLeft);
            }
            list.add(item);
        }
        res.put("list", list);
        res.put("total", list.size());
        return res;
    }
}
