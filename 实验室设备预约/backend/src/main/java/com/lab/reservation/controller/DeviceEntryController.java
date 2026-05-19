package com.lab.reservation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DeviceEntry;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.DeviceEntryService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device-entry")
@CrossOrigin
public class DeviceEntryController {

    @Autowired
    private DeviceEntryService deviceEntryService;

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<DeviceEntry> page = deviceEntryService.pageEntries(pageNum, pageSize, keyword, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> add(@RequestBody DeviceEntry entry) {
        Map<String, Object> res = new HashMap<>();
        Long applicantId = getCurrentUserId();
        deviceEntryService.addEntry(entry, applicantId);
        res.put("message", "添加成功");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/labs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> labs() {
        java.util.Set<String> labSet = new java.util.LinkedHashSet<>();
        labSet.add("A栋101");
        labSet.add("A栋102");
        labSet.add("B栋201");
        labSet.add("B栋202");
        labSet.add("C栋301");
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        int i = 1;
        for (String labName : labSet) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i++);
            m.put("labName", labName);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            SysUser user = sysUserService.getByUsername(auth.getName());
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
