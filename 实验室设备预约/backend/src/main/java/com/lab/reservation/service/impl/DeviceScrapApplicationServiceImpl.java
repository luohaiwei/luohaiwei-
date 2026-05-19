package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.DeviceScrapApplication;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.DeviceScrapApplicationMapper;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.DeviceScrapApplicationService;
import com.lab.reservation.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class DeviceScrapApplicationServiceImpl
        extends ServiceImpl<DeviceScrapApplicationMapper, DeviceScrapApplication>
        implements DeviceScrapApplicationService {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public Page<DeviceScrapApplication> pageApplications(Integer pageNum, Integer pageSize,
            String deviceName, String deviceNo, Integer status) {
        Page<DeviceScrapApplication> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<DeviceScrapApplication> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (StringUtils.hasText(deviceName)) {
            w.like("device_name", deviceName.trim());
        }
        if (StringUtils.hasText(deviceNo)) {
            w.like("device_no", deviceNo.trim());
        }
        if (status != null) {
            w.eq("status", status);
        }
        w.orderByDesc("create_time");
        Page<DeviceScrapApplication> result = page(page, w);
        // 补充申请人姓名、审批人姓名、设备型号/购买日期/购买价格/使用年限
        for (DeviceScrapApplication app : result.getRecords()) {
            enrich(app);
        }
        return result;
    }

    private void enrich(DeviceScrapApplication app) {
        if (app.getApplicantId() != null) {
            SysUser u = sysUserService.getById(app.getApplicantId());
            if (u != null) {
                app.setApplicant(u.getRealName() != null && !u.getRealName().isEmpty()
                        ? u.getRealName() : u.getUsername());
            }
        }
        if (app.getAuditorId() != null) {
            SysUser u = sysUserService.getById(app.getAuditorId());
            if (u != null) {
                app.setAuditor(u.getRealName() != null && !u.getRealName().isEmpty()
                        ? u.getRealName() : u.getUsername());
            }
        }
        if (app.getDeviceId() != null) {
            DeviceInfo d = deviceInfoService.getById(app.getDeviceId());
            if (d != null) {
                app.setModel(d.getModel());
                app.setPurchaseDate(d.getPurchaseDate() != null ? d.getPurchaseDate().atStartOfDay() : null);
                app.setPurchasePrice(d.getPrice());
                if (d.getPurchaseDate() != null) {
                    long years = ChronoUnit.YEARS.between(d.getPurchaseDate(), LocalDate.now());
                    app.setUsageYears((int) years);
                }
            }
        }
    }

    @Override
    @Transactional
    public void submit(Long deviceId, String scrapReason, Long applicantId) {
        if (deviceId == null) {
            throw new RuntimeException("请选择设备");
        }
        if (!StringUtils.hasText(scrapReason)) {
            throw new RuntimeException("请填写报废原因");
        }
        DeviceInfo device = deviceInfoService.getById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        QueryWrapper<DeviceScrapApplication> pending = new QueryWrapper<>();
        pending.eq("device_id", deviceId);
        pending.eq("status", 0);
        pending.eq("deleted", 0);
        if (count(pending) > 0) {
            throw new RuntimeException("该设备已有待审批的报废申请");
        }
        DeviceScrapApplication app = new DeviceScrapApplication();
        app.setDeviceId(deviceId);
        app.setDeviceName(device.getDeviceName());
        app.setDeviceNo(device.getDeviceNo());
        app.setScrapReason(scrapReason.trim());
        app.setStatus(0);
        app.setApplicantId(applicantId);
        save(app);
    }

    @Override
    @Transactional
    public void approve(Long id) {
        DeviceScrapApplication app = getById(id);
        if (app == null) {
            throw new RuntimeException("记录不存在");
        }
        if (app.getStatus() != null && app.getStatus() != 0) {
            throw new RuntimeException("当前状态不可审批");
        }
        app.setStatus(1);
        app.setAuditorId(getCurrentUserId());
        app.setAuditTime(LocalDateTime.now());
        updateById(app);
        if (app.getDeviceId() != null) {
            deviceInfoService.updateDeviceStatus(app.getDeviceId(), 4);
        }
    }

    @Override
    @Transactional
    public void reject(Long id, String opinion) {
        DeviceScrapApplication app = getById(id);
        if (app == null) {
            throw new RuntimeException("记录不存在");
        }
        if (app.getStatus() != null && app.getStatus() != 0) {
            throw new RuntimeException("当前状态不可拒绝");
        }
        app.setStatus(2);
        app.setAuditOpinion(opinion);
        app.setAuditorId(getCurrentUserId());
        app.setAuditTime(LocalDateTime.now());
        updateById(app);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        DeviceScrapApplication app = getById(id);
        if (app == null) {
            throw new RuntimeException("记录不存在");
        }
        if (app.getStatus() == null || (app.getStatus() != 1 && app.getStatus() != 2)) {
            throw new RuntimeException("仅已通过或已拒绝的记录可归档");
        }
        app.setStatus(3);
        updateById(app);
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null) {
                SysUser user = sysUserService.getByUsername(auth.getName());
                return user != null ? user.getId() : null;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
