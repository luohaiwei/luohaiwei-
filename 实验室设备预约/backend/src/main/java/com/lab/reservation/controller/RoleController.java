package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.entity.SysRole;
import com.lab.reservation.entity.SysUserRole;
import com.lab.reservation.mapper.SysRoleMapper;
import com.lab.reservation.mapper.SysRolePermissionMapper;
import com.lab.reservation.mapper.SysUserRoleMapper;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 角色管理（与前端 /role/* 对齐）
 */
@RestController
@RequestMapping("/role")
@CrossOrigin
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private static final String[] BUILTIN_CODES = {
            "SYSTEM_ADMIN", "LAB_ADMIN", "TEACHER", "STUDENT", "MAINTAINER"
    };

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role')")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysRole> w = new QueryWrapper<>();
        if (roleName != null && !roleName.trim().isEmpty()) {
            w.like("role_name", roleName.trim());
        }
        w.orderByAsc("sort").orderByDesc("create_time");
        Page<SysRole> result = sysRoleMapper.selectPage(page, w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysRole r : result.getRecords()) {
            rows.add(toRoleRow(r));
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", rows);
        res.put("total", result.getTotal());
        return res;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:add')")
    public ResponseEntity<Map<String, Object>> add(@RequestBody SysRole body) {
        log.info("[角色管理] 新增角色请求: roleCode={}, roleName={}", body.getRoleCode(), body.getRoleName());
        Map<String, Object> res = new HashMap<>();
        try {
            if (body.getRoleCode() == null || body.getRoleCode().trim().isEmpty()) {
                res.put("message", "角色编码不能为空");
                return ResponseEntity.badRequest().body(res);
            }
            if (body.getRoleName() == null || body.getRoleName().trim().isEmpty()) {
                res.put("message", "角色名称不能为空");
                return ResponseEntity.badRequest().body(res);
            }
            SysRole dup = sysRoleMapper.selectOne(new QueryWrapper<SysRole>()
                    .eq("role_code", body.getRoleCode().trim()));
            if (dup != null) {
                res.put("message", "角色编码已存在");
                return ResponseEntity.badRequest().body(res);
            }
            SysRole r = new SysRole();
            r.setRoleCode(body.getRoleCode().trim());
            r.setRoleName(body.getRoleName().trim());
            r.setDescription(body.getDescription());
            r.setSort(body.getSort() != null ? body.getSort() : 99);
            r.setStatus(body.getStatus() != null ? body.getStatus() : 1);
            sysRoleMapper.insert(r);
            savePermAudit("ROLE_CREATE", r.getRoleCode(),
                    "新增角色: " + r.getRoleName() + " (编码:" + r.getRoleCode() + ")");
            res.put("message", "添加成功");
            log.info("[角色管理] 新增角色成功: id={}", r.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("[角色管理] 新增角色异常", e);
            res.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PutMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:edit')")
    public ResponseEntity<Map<String, Object>> update(@RequestBody SysRole body) {
        log.info("[角色管理] 更新角色请求: id={}, roleName={}", body.getId(), body.getRoleName());
        Map<String, Object> res = new HashMap<>();
        try {
            if (body.getId() == null) {
                res.put("message", "缺少角色ID");
                return ResponseEntity.badRequest().body(res);
            }
            SysRole exist = sysRoleMapper.selectById(body.getId());
            if (exist == null) {
                res.put("message", "角色不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            if (isBuiltin(exist)) {
                String rn = body.getRoleName() != null ? body.getRoleName().trim() : exist.getRoleName();
                sysRoleMapper.update(null, new UpdateWrapper<SysRole>()
                        .eq("id", exist.getId())
                        .set("role_name", rn)
                        .set("description", body.getDescription()));
            } else {
                exist.setRoleName(body.getRoleName() != null ? body.getRoleName().trim() : exist.getRoleName());
                exist.setDescription(body.getDescription());
                if (body.getSort() != null) {
                    exist.setSort(body.getSort());
                }
                if (body.getStatus() != null) {
                    exist.setStatus(body.getStatus());
                }
                sysRoleMapper.updateById(exist);
            }
            savePermAudit("ROLE_UPDATE", exist.getRoleCode(),
                    "修改角色: " + exist.getRoleName() + " (编码:" + exist.getRoleCode() + ")");
            res.put("message", "更新成功");
            log.info("[角色管理] 更新角色成功: id={}", body.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("[角色管理] 更新角色异常", e);
            res.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:delete')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        SysRole exist = sysRoleMapper.selectById(id);
        if (exist == null) {
            res.put("message", "角色不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        if (isBuiltin(exist)) {
            res.put("message", "内置角色不可删除");
            return ResponseEntity.badRequest().body(res);
        }
        long userCount = sysUserRoleMapper.selectCount(new QueryWrapper<SysUserRole>().eq("role_id", id));
        if (userCount > 0) {
            res.put("message", "该角色下仍有用户，请先调整用户角色后再删除");
            return ResponseEntity.badRequest().body(res);
        }
            sysRolePermissionMapper.deleteByRoleId(id);
            sysRoleMapper.deleteById(id);
            savePermAudit("ROLE_DELETE", exist.getRoleCode(),
                    "删除角色: " + exist.getRoleName() + " (编码:" + exist.getRoleCode() + ")");
            res.put("message", "删除成功");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public List<Long> getRolePermissions(@PathVariable Long roleId) {
        if (roleId == null || roleId <= 0) {
            return new ArrayList<>();
        }
        List<Long> ids = sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
        return ids != null ? ids : new ArrayList<>();
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public ResponseEntity<Map<String, Object>> saveRolePermissions(
            @PathVariable Long roleId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        if (roleId == null || roleId <= 0) {
            res.put("message", "请先选择要配置的角色");
            return ResponseEntity.badRequest().body(res);
        }
        Long rid = roleId;
        SysRole role = sysRoleMapper.selectById(rid);
        if (role == null) {
            res.put("message", "角色不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }
        if (isBuiltin(role)) {
            res.put("message", "内置角色权限不可修改");
            return ResponseEntity.badRequest().body(res);
        }
        @SuppressWarnings("unchecked")
        List<Object> raw = body != null ? (List<Object>) body.get("permIds") : null;
        List<Long> permIds = new ArrayList<>();
        if (raw != null) {
            for (Object o : raw) {
                if (o == null) {
                    continue;
                }
                permIds.add(o instanceof Number ? ((Number) o).longValue() : Long.parseLong(String.valueOf(o)));
            }
        }
        sysRolePermissionMapper.deleteByRoleId(rid);
        for (Long pid : permIds) {
            if (pid != null) {
                sysRolePermissionMapper.insert(rid, pid);
            }
        }
        savePermAudit("PERM_ASSIGN", role.getRoleCode(),
                "角色 [" + role.getRoleName() + "] 权限配置变更，共分配 " + permIds.size() + " 项权限");
        res.put("message", "保存成功");
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> toRoleRow(SysRole r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("roleCode", r.getRoleCode());
        m.put("roleName", r.getRoleName());
        m.put("description", r.getDescription());
        m.put("sort", r.getSort());
        m.put("status", r.getStatus());
        m.put("createTime", r.getCreateTime());
        m.put("updateTime", r.getUpdateTime());
        long cnt = sysUserRoleMapper.selectCount(new QueryWrapper<SysUserRole>().eq("role_id", r.getId()));
        m.put("userCount", cnt);
        m.put("isSystem", isBuiltin(r) ? 1 : 0);
        return m;
    }

    private boolean isBuiltin(SysRole r) {
        if (r.getRoleCode() == null) {
            return false;
        }
        String code = r.getRoleCode().trim();
        for (String c : BUILTIN_CODES) {
            if (c.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 记录权限变更审计日志（写入 sys_log）
     */
    private void savePermAudit(String changeType, String targetRole, String detail) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String operator = auth != null ? auth.getName() : "unknown";
            SysUser opUser = null;
            try {
                opUser = sysUserService.getByUsername(operator);
            } catch (Exception ignored) {
            }
            SysLog audit = new SysLog();
            audit.setUsername(operator);
            if (opUser != null) {
                audit.setUserId(opUser.getId());
                audit.setUserType(opUser.getUserType());
            }
            audit.setModule("权限管理");
            audit.setOperation(changeType);  // ROLE_CREATE / ROLE_UPDATE / PERM_ASSIGN / ...
            audit.setRequestUrl("/role");
            audit.setRequestParams(detail);
            audit.setIpAddress(resolveCurrentIp());
            audit.setStatus(1);
            audit.setCreateTime(java.time.LocalDateTime.now());
            sysLogService.save(audit);
        } catch (Exception e) {
            log.warn("[权限审计] 记录日志失败: {}", e.getMessage());
        }
    }

    private String resolveCurrentIp() {
        try {
            javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                    .resolveReference(javax.servlet.http.HttpServletRequest.class.getName());
            if (req == null) return "unknown";
            String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
            for (String h : headers) {
                String ip = req.getHeader(h);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.split(",")[0].trim();
                }
            }
            return req.getRemoteAddr();
        } catch (Exception ignored) {}
        return "unknown";
    }
}
