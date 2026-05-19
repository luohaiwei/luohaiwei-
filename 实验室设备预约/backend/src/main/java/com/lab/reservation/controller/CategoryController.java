package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DeviceCategory;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.service.DeviceCategoryService;
import com.lab.reservation.service.DeviceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备分类控制器
 */
@RestController
@RequestMapping("/category")
@CrossOrigin
public class CategoryController {

    @Autowired
    private DeviceCategoryService categoryService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    /**
     * 获取分类列表（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<DeviceCategory> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DeviceCategory> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByDesc("create_time");
        Page<DeviceCategory> result = categoryService.page(page, wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return ResponseEntity.ok(map);
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceCategory>> getAll() {
        QueryWrapper<DeviceCategory> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByDesc("create_time");
        List<DeviceCategory> list = categoryService.list(wrapper);
        return ResponseEntity.ok(list);
    }

    /**
     * 添加分类
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('category')")
    public ResponseEntity<String> add(@RequestBody DeviceCategory category) {
        categoryService.save(category);
        return ResponseEntity.ok("添加成功");
    }

    /**
     * 更新分类
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('category')")
    public ResponseEntity<String> update(@RequestBody DeviceCategory category) {
        categoryService.updateById(category);
        return ResponseEntity.ok("更新成功");
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('category')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        // 检查是否有子分类
        QueryWrapper<DeviceCategory> childCheck = new QueryWrapper<>();
        childCheck.eq("parent_id", id);
        if (categoryService.count(childCheck) > 0) {
            return ResponseEntity.badRequest().body("该分类下存在子分类，请先删除子分类");
        }
        // 检查是否有设备使用此分类
        QueryWrapper<DeviceInfo> deviceCheck = new QueryWrapper<>();
        deviceCheck.eq("category_id", id);
        if (deviceInfoService.count(deviceCheck) > 0) {
            return ResponseEntity.badRequest().body("该分类下存在设备，无法删除");
        }
        categoryService.removeById(id);
        return ResponseEntity.ok("删除成功");
    }
}
