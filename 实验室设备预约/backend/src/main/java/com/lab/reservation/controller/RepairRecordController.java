package com.lab.reservation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.RepairOrder;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.RepairOrderService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 维修记录查询（独立路径，避免与 /repair/{id} 等模式冲突；RM-004）
 */
@RestController
@RequestMapping("/repair-record")
@CrossOrigin
public class RepairRecordController {

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private SysUserService sysUserService;

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return sysUserService.getByUsername(authentication.getName());
    }

    /**
     * 分页查询维修记录（已完成/已关闭为主，支持条件筛选）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('repair') or hasAuthority('repair-list') or hasAuthority('repair-record')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reportDateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reportDateTo) {
        SysUser viewer = getCurrentUser();
        Long viewerId = viewer != null ? viewer.getId() : null;
        String viewerType = viewer != null ? viewer.getUserType() : null;
        Page<RepairOrder> page = repairOrderService.pageRepairRecords(
                pageNum, pageSize, status, orderNo, deviceName, reportDateFrom, reportDateTo, viewerId, viewerType);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }
}
