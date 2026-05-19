package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DataScopeContext;
import com.lab.reservation.entity.SysRole;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.entity.SysUserRole;
import com.lab.reservation.mapper.SysRoleMapper;
import com.lab.reservation.mapper.SysUserRoleMapper;
import com.lab.reservation.service.DataScopeService;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器（系统管理员）
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SysLogService sysLogService;

    /**
     * 用户列表（分页）- 带数据权限过滤
     * - 系统管理员：可查看所有用户
     * - 实验室管理员：可查看本实验室的用户
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('user')")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType) {
        
        // 获取数据权限上下文
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        
        // 关键词：同时匹配登录用户名、真实姓名
        if (username != null && !username.isEmpty()) {
            String kw = username.trim();
            wrapper.and(w -> w.like("username", kw).or().like("real_name", kw));
        }
        if (userType != null && !userType.isEmpty()) {
            wrapper.eq("user_type", userType);
        }
        
        // 应用数据权限过滤
        if (scopeContext != null && !scopeContext.isSystemAdmin() && !scopeContext.hasAllScope()) {
            if (!scopeContext.canViewUser()) {
                wrapper.eq("1", 0);
            } else {
                applyUserDataScope(wrapper, scopeContext);
            }
        }
        
        wrapper.orderByDesc("create_time");
        Page<SysUser> result = sysUserService.page(page, wrapper);
        
        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        return res;
    }
    
    /**
     * 应用用户数据权限过滤
     */
    private void applyUserDataScope(QueryWrapper<SysUser> wrapper, DataScopeContext context) {
        String scopeType = context.getScopeType();
        
        switch (scopeType) {
            case "DEPT":
                // 部门级：查看同实验室的用户（按 laboratory 字段精确匹配，与设备权限一致）
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else {
                    wrapper.eq("id", context.getUserId()); // 只能看自己
                }
                break;
                
            case "SELF":
                // 自助级：只能查看自己
                wrapper.eq("id", context.getUserId());
                break;
                
            case "CUSTOM":
                // 自定义：用户「所属实验室」在角色配置的实验室列表内（与设备表 laboratory 字符串一致）
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().trim().isEmpty()) {
                    List<String> labs = Arrays.stream(context.getCustomLabIds().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    if (labs.isEmpty()) {
                        wrapper.eq("id", context.getUserId());
                    } else {
                        wrapper.in("laboratory", labs);
                    }
                } else {
                    wrapper.eq("id", context.getUserId());
                }
                break;
                
            default:
                wrapper.eq("id", context.getUserId());
                break;
        }
    }

    /**
     * 导出用户列表（Excel，与前端「导出」一致；条件与 /list 相同，不分页）
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:edit')")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType) throws IOException {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            String kw = username.trim();
            wrapper.and(w -> w.like("username", kw).or().like("real_name", kw));
        }
        if (userType != null && !userType.isEmpty()) {
            wrapper.eq("user_type", userType);
        }
        // 与 /list 一致：按数据权限过滤，避免自定义角色导出全部用户
        DataScopeContext exportScope = dataScopeService.getCurrentDataScope();
        if (exportScope != null && !exportScope.isSystemAdmin() && !exportScope.hasAllScope()) {
            if (!exportScope.canViewUser()) {
                wrapper.eq("1", 0);
            } else {
                applyUserDataScope(wrapper, exportScope);
            }
        }
        wrapper.orderByDesc("create_time");
        List<SysUser> users = sysUserService.list(wrapper);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("用户列表");
            String[] headers = {"用户名", "姓名", "学号/工号", "角色", "性别", "手机", "邮箱", "部门/班级", "状态", "最后登录", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int r = 1;
            for (SysUser u : users) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nvl(u.getUsername()));
                row.createCell(1).setCellValue(nvl(u.getRealName()));
                row.createCell(2).setCellValue(nvl(u.getStudentStaffNo()));
                row.createCell(3).setCellValue(userTypeLabel(u.getUserType()));
                row.createCell(4).setCellValue(genderLabel(u.getGender()));
                row.createCell(5).setCellValue(nvl(u.getPhone()));
                row.createCell(6).setCellValue(nvl(u.getEmail()));
                row.createCell(7).setCellValue(nvl(u.getDepartment()));
                row.createCell(8).setCellValue(u.getStatus() != null && u.getStatus() == 1 ? "正常" : "禁用");
                row.createCell(9).setCellValue(
                        u.getLastLoginTime() != null ? u.getLastLoginTime().format(dtf) : "");
                row.createCell(10).setCellValue(
                        u.getCreateTime() != null ? u.getCreateTime().format(dtf) : "");
            }
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(bos);
        }

        byte[] bytes = bos.toByteArray();
        String filename = "用户列表_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String userTypeLabel(String t) {
        if (t == null) {
            return "";
        }
        switch (t) {
            case "SYSTEM_ADMIN":
                return "系统管理员";
            case "LAB_ADMIN":
                return "实验室管理员";
            case "TEACHER":
                return "教师";
            case "STUDENT":
                return "学生";
            case "MAINTAINER":
                return "设备维护人员";
            default:
                return t;
        }
    }

    private static String genderLabel(Integer g) {
        if (g == null) return "";
        switch (g) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 新增用户
     */
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:add')")
    public Map<String, Object> add(@RequestBody SysUser user) {
        Map<String, Object> res = new HashMap<>();
        try {
            if ("SYSTEM_ADMIN".equals(user.getUserType())) {
                res.put("message", "不可通过用户管理创建系统管理员账号");
                return res;
            }
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            user.setStatus(1);
            sysUserService.save(user);
            if (user.getUserType() != null) {
                Long roleId = getRoleIdByUserType(user.getUserType());
                if (roleId != null) {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(roleId);
                    sysUserRoleMapper.insert(ur);
                }
            }
            res.put("message", "添加成功");
        } catch (Exception e) {
            res.put("message", e.getMessage());
        }
        return res;
    }

    /**
     * 更新用户（须数据权限「编辑用户」）
     */
    @PutMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:edit')")
    public ResponseEntity<Map<String, Object>> update(@RequestBody SysUser user) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canEditUser()) {
            res.put("message", "无编辑用户数据权限（请在「权限分配」中为该角色勾选「编辑用户」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            SysUser exist = sysUserService.getById(user.getId());
            if (exist == null) {
                res.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            if (isBuiltInAdmin(exist)) {
                res.put("message", "内置管理员账号不可修改");
                return ResponseEntity.badRequest().body(res);
            }
            user.setPassword(null);
            sysUserService.updateById(user);
            res.put("message", "更新成功");
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 批量删除用户（须数据权限「删除用户」）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:delete')")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canDeleteUser()) {
            res.put("message", "无删除用户数据权限（请在「权限分配」中为该角色勾选「删除用户」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        Object raw = body != null ? body.get("ids") : null;
        if (!(raw instanceof List<?>)) {
            res.put("message", "请传入 ids 数组");
            return ResponseEntity.badRequest().body(res);
        }
        List<?> idList = (List<?>) raw;
        if (idList.isEmpty()) {
            res.put("message", "未选择要删除的用户");
            return ResponseEntity.badRequest().body(res);
        }
        int deleted = 0;
        List<String> skipReasons = new ArrayList<>();
        for (Object o : idList) {
            if (o == null) {
                continue;
            }
            long id;
            try {
                id = o instanceof Number ? ((Number) o).longValue() : Long.parseLong(String.valueOf(o).trim());
            } catch (NumberFormatException e) {
                skipReasons.add("非法ID: " + o);
                continue;
            }
            SysUser target = sysUserService.getById(id);
            if (target == null) {
                skipReasons.add("ID " + id + " 不存在");
                continue;
            }
            if (isBuiltInAdmin(target)) {
                skipReasons.add("内置管理员不可删除");
                continue;
            }
            sysUserService.removeById(id);
            sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", id));
            deleted++;
        }
        res.put("deleted", deleted);
        if (deleted > 0) {
            res.put("message", "已删除 " + deleted + " 位用户");
        } else {
            res.put("message", skipReasons.isEmpty() ? "未删除任何用户" : String.join("；", skipReasons));
        }
        if (!skipReasons.isEmpty()) {
            res.put("skipped", skipReasons);
        }
        return deleted > 0 ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    /**
     * 删除用户（须数据权限「删除用户」）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:delete')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canDeleteUser()) {
            res.put("message", "无删除用户数据权限（请在「权限分配」中为该角色勾选「删除用户」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            SysUser target = sysUserService.getById(id);
            if (isBuiltInAdmin(target)) {
                res.put("message", "内置管理员账号不可删除");
                return ResponseEntity.badRequest().body(res);
            }
            sysUserService.removeById(id);
            sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", id));
            res.put("message", "删除成功");
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 分配角色（须数据权限「分配用户角色」）
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:assign-role')")
    public ResponseEntity<Map<String, Object>> assignRole(@PathVariable Long id, @RequestParam String userType) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canAssignRole()) {
            res.put("message", "无分配用户角色数据权限（请在「权限分配」中为该角色勾选「分配用户角色」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                res.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            if (isBuiltInAdmin(user)) {
                res.put("message", "内置管理员角色不可变更");
                return ResponseEntity.badRequest().body(res);
            }
            if ("SYSTEM_ADMIN".equalsIgnoreCase(userType)) {
                res.put("message", "不可通过用户管理分配系统管理员角色");
                return ResponseEntity.badRequest().body(res);
            }
            user.setUserType(userType);
            sysUserService.updateById(user);
            sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", id));
            Long roleId = getRoleIdByUserType(userType);
            if (roleId == null) {
                res.put("message", "无效的角色编码，请先在角色管理中维护该角色");
                return ResponseEntity.badRequest().body(res);
            }
            SysUserRole ur = new SysUserRole();
            ur.setUserId(id);
            ur.setRoleId(roleId);
            sysUserRoleMapper.insert(ur);
            String oldUserType = user.getUserType(); // 保存旧的角色类型用于提示
            res.put("message", "角色分配成功");
            res.put("needReLogin", true);
            res.put("notice", "用户需重新登录后新角色菜单才会生效");
            res.put("oldUserType", oldUserType);
            res.put("newUserType", userType);
            saveUserRoleAssignAudit(user, userType, roleId);
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 管理员重置用户密码为系统默认初始密码（须数据权限「编辑用户」）
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:edit')")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canEditUser()) {
            res.put("message", "无编辑用户数据权限（请在「权限分配」中为该角色勾选「编辑用户」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            sysUserService.resetPasswordToDefaultByAdmin(id);
            res.put("message", "密码已重置为系统默认初始密码，请告知用户登录后尽快修改");
            res.put("defaultPasswordPlain", "123456");
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 管理员清零指定用户的爽约次数（解除禁止预约状态）
     * 使用固定路径 + 查询参数 id，避免部分网关/代理对 /{id}/reset-missed-count 返回 404
     */
    @PutMapping("/admin/reset-missed-count")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:edit')")
    public ResponseEntity<Map<String, Object>> resetMissedCount(@RequestParam("id") Long id) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canEditUser()) {
            res.put("message", "无编辑用户数据权限");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            sysUserService.resetMissedCount(id);
            res.put("message", "爽约次数已清零，该用户现已解除预约限制");
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 启用/禁用（须数据权限「编辑用户」）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('user:edit')")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Map<String, Object> res = new HashMap<>();
        DataScopeContext ctx = dataScopeService.getCurrentDataScope();
        if (ctx != null && !ctx.isSystemAdmin() && !ctx.hasAllScope() && !ctx.canEditUser()) {
            res.put("message", "无编辑用户数据权限（请在「权限分配」中为该角色勾选「编辑用户」）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }
        try {
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                res.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            if (isBuiltInAdmin(user)) {
                res.put("message", "内置管理员账号不可禁用");
                return ResponseEntity.badRequest().body(res);
            }
            user.setStatus(status);
            sysUserService.updateById(user);
            res.put("message", "操作成功");
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 按类型获取用户列表（用于分配维护人员等下拉）
     */
    @GetMapping("/by-type")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('user')")
    public Object listByType(@RequestParam String userType) {
        return sysUserService.list(new QueryWrapper<SysUser>()
                .eq("user_type", userType)
                .eq("status", 1)
                .orderByAsc("real_name"));
    }

    /**
     * 按角色编码解析 sys_role.id（支持自定义角色；内置五种在库中也有记录，走同一查询即可）
     */
    private Long getRoleIdByUserType(String userType) {
        if (userType == null || userType.isEmpty()) {
            return null;
        }
        SysRole role = sysRoleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", userType));
        return role != null ? role.getId() : null;
    }

    /** 用户角色分配写入权限审计（sys_log.operation=USER_ROLE_CHANGE） */
    private void saveUserRoleAssignAudit(SysUser target, String newUserType, Long assignedRoleId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String operator = auth != null ? auth.getName() : "unknown";
            SysUser op = sysUserService.getByUsername(operator);
            String display = target.getRealName() != null && !target.getRealName().isEmpty()
                    ? target.getRealName() : target.getUsername();
            String detail = String.format("目标用户: %s (%s), 新角色编码: %s, roleId=%s",
                    display, target.getUsername(), newUserType,
                    assignedRoleId != null ? assignedRoleId.toString() : "-");
            SysLog audit = new SysLog();
            audit.setUsername(operator);
            if (op != null) {
                audit.setUserId(op.getId());
                audit.setUserType(op.getUserType());
            }
            audit.setModule("用户管理");
            audit.setOperation("USER_ROLE_CHANGE");
            audit.setRequestUrl("/user/" + target.getId() + "/role");
            audit.setRequestParams(detail);
            audit.setIpAddress(resolveUserAuditIp());
            audit.setStatus(1);
            audit.setCreateTime(LocalDateTime.now());
            sysLogService.save(audit);
        } catch (Exception ignored) {
        }
    }

    private static String resolveUserAuditIp() {
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

    private static boolean isBuiltInAdmin(SysUser user) {
        return user != null && user.getUsername() != null && "admin".equalsIgnoreCase(user.getUsername());
    }
}
