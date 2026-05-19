package com.lab.reservation.controller;

import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.SysUserService;
import com.lab.reservation.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册等认证相关请求
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest,
            HttpServletRequest request) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.status(401).body(error);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "你已被禁用，请联系管理员");
            return ResponseEntity.status(403).body(error);
        }

        // 认证
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(error);
        }

        // 登录成功后更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        sysUserService.updateById(user);

        String effectiveType = sysUserService.resolveEffectiveUserType(user);
        // 生成Token（与前端路由、菜单一致，优先采用角色关联表编码）
        String token = jwtTokenUtil.generateToken(username, user.getId(), effectiveType);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("userType", effectiveType);
        result.put("laboratory", user.getLaboratory());
        result.put("department", user.getDepartment());
        result.put("permissions", sysUserService.listUserPermissionCodes(user.getId()));

        request.setAttribute("logUserType", effectiveType);
        return ResponseEntity.ok(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody SysUser user) {
        try {
            // 设置默认用户类型
            if (user.getUserType() == null || user.getUserType().isEmpty()) {
                user.setUserType("STUDENT"); // 默认学生
            }
            // 仅允许可自注册角色
            if (!"STUDENT".equals(user.getUserType())
                    && !"TEACHER".equals(user.getUserType())
                    && !"MAINTAINER".equals(user.getUserType())) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "用户类型不合法");
                return ResponseEntity.badRequest().body(error);
            }
            sysUserService.register(user);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "注册成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);

        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(error);
        }

        String effectiveType = sysUserService.resolveEffectiveUserType(user);
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("userType", effectiveType);
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("department", user.getDepartment());
        result.put("laboratory", user.getLaboratory());
        result.put("studentStaffNo", user.getStudentStaffNo());
        result.put("gender", user.getGender());
        result.put("experimentType", user.getExperimentType());
        result.put("skillLevel", user.getSkillLevel());
        result.put("status", user.getStatus());
        result.put("missedCount", user.getMissedCount());
        result.put("createTime", user.getCreateTime());
        result.put("lastLoginTime", user.getLastLoginTime());
        result.put("permissions", sysUserService.listUserPermissionCodes(user.getId()));

        return ResponseEntity.ok(result);
    }

    /**
     * 当前登录用户可见菜单（来自角色-权限配置；前端用于自定义角色侧栏）
     */
    @GetMapping("/menus")
    public ResponseEntity<Map<String, Object>> getMenus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(error);
        }
        List<Map<String, Object>> list = sysUserService.listUserMenus(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        return ResponseEntity.ok(result);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> passwordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);

        String oldPassword = passwordRequest.get("oldPassword");
        String newPassword = passwordRequest.get("newPassword");

        try {
            sysUserService.updatePassword(user.getId(), oldPassword, newPassword);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "密码修改成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);

        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            if (profileRequest.containsKey("realName")) {
                user.setRealName((String) profileRequest.get("realName"));
            }
            if (profileRequest.containsKey("phone")) {
                user.setPhone((String) profileRequest.get("phone"));
            }
            if (profileRequest.containsKey("email")) {
                user.setEmail((String) profileRequest.get("email"));
            }
            if (profileRequest.containsKey("department")) {
                user.setDepartment((String) profileRequest.get("department"));
            }
            sysUserService.updateById(user);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "保存成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 通过手机号重置密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> resetRequest) {
        String username = resetRequest.get("username");
        String phone = resetRequest.get("phone");
        String newPassword = resetRequest.get("newPassword");

        try {
            sysUserService.resetPasswordByPhone(username, phone, newPassword);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "密码重置成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
