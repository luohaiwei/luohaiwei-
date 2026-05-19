package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.Laboratory;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.LaboratoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实验室管理控制器
 * 处理实验室信息管理相关请求
 */
@RestController
@RequestMapping("/laboratory")
@CrossOrigin
public class LaboratoryController {

    @Autowired
    private LaboratoryService laboratoryService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    /**
     * 获取实验室列表（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String labType,
            @RequestParam(required = false) Integer status) {

        Page<Laboratory> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Laboratory> wrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("lab_code", keyword)
                    .or().like("lab_name", keyword)
                    .or().like("location", keyword));
        }
        if (labType != null && !labType.isEmpty()) {
            wrapper.eq("lab_type", labType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("create_time");
        Page<Laboratory> result = laboratoryService.page(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pageNum", result.getCurrent());
        data.put("pageSize", result.getSize());

        return ResponseEntity.ok(data);
    }

    /**
     * 获取所有实验室（用于下拉选择）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<List<Laboratory>> getAll() {
        QueryWrapper<Laboratory> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByAsc("lab_name");
        return ResponseEntity.ok(laboratoryService.list(wrapper));
    }

    /**
     * 获取实验室详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Laboratory> getById(@PathVariable Long id) {
        Laboratory lab = laboratoryService.getById(id);
        if (lab == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lab);
    }

    /**
     * 新增实验室
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Laboratory laboratory) {
        Map<String, Object> result = new HashMap<>();

        // 设置初始设备数量为0
        if (laboratory.getEquipmentCount() == null) {
            laboratory.setEquipmentCount(0);
        }
        // 设置默认状态为正常
        if (laboratory.getStatus() == null) {
            laboratory.setStatus(1);
        }

        boolean success = laboratoryService.save(laboratory);
        if (success) {
            result.put("message", "添加成功");
            return ResponseEntity.ok(result);
        } else {
            result.put("message", "添加失败");
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 更新实验室
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Laboratory laboratory) {
        Map<String, Object> result = new HashMap<>();
        Laboratory existing = laboratoryService.getById(id);
        if (existing == null) {
            result.put("message", "实验室不存在");
            return ResponseEntity.badRequest().body(result);
        }

        laboratory.setId(id);
        boolean success = laboratoryService.updateById(laboratory);
        if (success) {
            result.put("message", "更新成功");
            return ResponseEntity.ok(result);
        } else {
            result.put("message", "更新失败");
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 删除实验室
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Laboratory lab = laboratoryService.getById(id);
        if (lab == null) {
            result.put("message", "实验室不存在");
            return ResponseEntity.badRequest().body(result);
        }

        String labName = lab.getLabName();
        if (labName != null && !labName.trim().isEmpty()) {
            long relatedCount = deviceInfoService.count(new QueryWrapper<DeviceInfo>()
                    .eq("laboratory", labName.trim()));
            if (relatedCount > 0) {
                result.put("message", "删除失败：该实验室下仍有关联设备（" + relatedCount + "台），请先迁移或删除设备");
                return ResponseEntity.badRequest().body(result);
            }
        }

        boolean success = laboratoryService.removeById(id);
        if (success) {
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } else {
            result.put("message", "删除失败");
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取实验室统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> statistics() {
        QueryWrapper<Laboratory> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        long activeCount = laboratoryService.count(wrapper);

        QueryWrapper<Laboratory> disabledWrapper = new QueryWrapper<>();
        disabledWrapper.eq("status", 0);
        long disabledCount = laboratoryService.count(disabledWrapper);

        QueryWrapper<Laboratory> capacityWrapper = new QueryWrapper<>();
        capacityWrapper.eq("status", 1);
        capacityWrapper.isNotNull("capacity");
        List<Laboratory> labs = laboratoryService.list(capacityWrapper);
        int totalCapacity = labs.stream()
                .filter(l -> l.getCapacity() != null)
                .mapToInt(Laboratory::getCapacity)
                .sum();

        Map<String, Long> typeStats = new HashMap<>();
        QueryWrapper<Laboratory> typeWrapper = new QueryWrapper<>();
        typeWrapper.eq("status", 1);
        List<Laboratory> allLabs = laboratoryService.list(typeWrapper);
        for (Laboratory lab : allLabs) {
            String type = lab.getLabType();
            typeStats.merge(type != null ? type : "其他", 1L, Long::sum);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", activeCount);
        stats.put("disabledCount", disabledCount);
        stats.put("totalCapacity", totalCapacity);
        stats.put("typeStats", typeStats);

        return ResponseEntity.ok(stats);
    }
}
