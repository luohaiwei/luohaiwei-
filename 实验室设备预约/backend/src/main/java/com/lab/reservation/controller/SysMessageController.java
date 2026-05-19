package com.lab.reservation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.SysMessage;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.SysMessageService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统消息（通知）控制器
 */
@RestController
@RequestMapping("/notification")
@CrossOrigin
public class SysMessageController {

    @Autowired
    private SysMessageService sysMessageService;

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
     * 分页查询当前用户的消息列表
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyMessages(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        SysUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "未登录");
            return ResponseEntity.status(401).body(body);
        }

        int pn = page != null ? page : pageNum;
        int ps = size != null ? size : pageSize;

        Page<SysMessage> pageResult = sysMessageService.pageMyMessages(
                currentUser.getId(), pn, ps, type, isRead, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("unreadCount", sysMessageService.getUnreadCount(currentUser.getId()));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前用户未读消息数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        SysUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        int count = sysMessageService.getUnreadCount(currentUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("unreadCount", count);
        return ResponseEntity.ok(result);
    }

    /**
     * 标记单条消息为已读
     */
    @PutMapping("/read/{id}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        SysUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        boolean success = sysMessageService.markAsRead(id, currentUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        if (!success) {
            result.put("message", "消息不存在或无权操作");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        SysUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        sysMessageService.markAllAsRead(currentUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("message", "操作成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        SysUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        boolean success = sysMessageService.deleteMessage(id, currentUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        if (!success) {
            result.put("message", "消息不存在或无权操作");
        }
        return ResponseEntity.ok(result);
    }
}
