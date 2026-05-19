package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lab.reservation.entity.SysPermission;
import com.lab.reservation.entity.RoleDataScope;
import com.lab.reservation.entity.DataScopeContext;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.entity.SysRole;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.SysPermissionMapper;
import com.lab.reservation.mapper.RoleDataScopeMapper;
import com.lab.reservation.mapper.DeviceInfoMapper;
import com.lab.reservation.mapper.LaboratoryMapper;
import com.lab.reservation.mapper.SysRoleMapper;
import com.lab.reservation.mapper.SysRolePermissionMapper;
import com.lab.reservation.service.DataScopeService;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/permission")
@CrossOrigin
public class PermissionController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private RoleDataScopeMapper roleDataScopeMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private LaboratoryMapper laboratoryMapper;
    
    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private SysUserService sysUserService;
    
    /**
     * 归一化全局角色ID
     */
    private Long normalizeGlobalRoleId(Long roleId) {
        // 简单实现，直接返回传入的roleId
        return roleId;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public List<Map<String, Object>> tree() {
        List<SysPermission> all = sysPermissionMapper.selectList(
                new QueryWrapper<SysPermission>()
                    .eq("status", 1)
                    .orderByAsc("sort")
                    .orderByAsc("id"));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        // 去重：按permission_code去重，保留排序靠前的记录
        Map<String, SysPermission> dedupMap = new LinkedHashMap<>();
        for (SysPermission p : all) {
            String code = p.getPermissionCode();
            if (code != null && !code.isEmpty()) {
                if (!dedupMap.containsKey(code)) {
                    dedupMap.put(code, p);
                }
            } else {
                // 无permission_code的记录（按钮类），按id去重
                dedupMap.put("id_" + p.getId(), p);
            }
        }
        List<SysPermission> uniqueList = new ArrayList<>(dedupMap.values());
        // 按sort和id重新排序
        uniqueList.sort((a, b) -> {
            int s = Integer.compare(a.getSort() != null ? a.getSort() : 0,
                                   b.getSort() != null ? b.getSort() : 0);
            if (s != 0) return s;
            return Long.compare(a.getId(), b.getId());
        });

        List<Map<String, Object>> flat = new ArrayList<>();
        for (SysPermission p : uniqueList) {
            flat.add(permToNode(p));
        }
        return buildTree(flat, 0L);
    }

    /**
     * 获取角色已有权限ID列表（仅菜单和按钮类型，过滤API权限）
     */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public List<Long> getRolePermissions(@PathVariable Long roleId) {
        Long rid = normalizeGlobalRoleId(roleId);
        List<Long> ids = sysRolePermissionMapper.selectPermissionIdsByRoleId(rid);
        return ids != null ? ids : new ArrayList<>();
    }

    @GetMapping("/data-scope")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public Map<String, Object> getDataScope(
            @RequestParam(required = false, defaultValue = "0") Long roleId) {
        Long rid = normalizeRoleId(roleId);
        if (rid == null) {
            Map<String, Object> def = new HashMap<>();
            def.put("scopeType", "ALL");
            def.put("customLabIds", new ArrayList<String>());
            def.put("deviceScope", Arrays.asList("VIEW", "EDIT"));
            def.put("bookingScope", Arrays.asList("VIEW"));
            def.put("userScope", Arrays.asList("VIEW"));
            return def;
        }
        RoleDataScope scope = roleDataScopeMapper.selectOne(
                new QueryWrapper<RoleDataScope>().eq("role_id", rid));
        Map<String, Object> m = new HashMap<>();
        if (scope == null) {
            m.put("scopeType", "ALL");
            m.put("customLabIds", new ArrayList<Long>());
            m.put("deviceScope", Arrays.asList("VIEW", "EDIT"));
            m.put("bookingScope", Arrays.asList("VIEW"));
            m.put("userScope", Arrays.asList("VIEW"));
        } else {
            m.put("scopeType", scope.getScopeType() != null ? scope.getScopeType() : "ALL");
            m.put("customLabIds", parseCommaList(scope.getCustomLabIds()));
            m.put("deviceScope", parseCommaList(scope.getDeviceScope()));
            m.put("bookingScope", parseCommaList(scope.getBookingScope()));
            m.put("userScope", parseCommaList(scope.getUserScope()));
        }
        return m;
    }

    @GetMapping("/my-data-scope")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getMyDataScope() {
        DataScopeContext context = dataScopeService.getCurrentDataScope();
        Map<String, Object> m = new HashMap<>();
        if (context == null) {
            m.put("scopeType", "SELF");
            m.put("customLabIds", new ArrayList<String>());
            m.put("deviceScope", Arrays.asList("VIEW"));
            m.put("bookingScope", Arrays.asList("VIEW"));
            m.put("userScope", Arrays.asList("VIEW"));
            return m;
        }
        m.put("scopeType", context.getScopeType() != null ? context.getScopeType() : "SELF");
        m.put("customLabIds", parseCommaList(context.getCustomLabIds()));
        m.put("deviceScope", parseCommaList(context.getDeviceScope()));
        m.put("bookingScope", parseCommaList(context.getBookingScope()));
        m.put("userScope", parseCommaList(context.getUserScope()));
        return m;
    }

    @PutMapping("/data-scope")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('role:assign-perm')")
    public ResponseEntity<Map<String, Object>> saveDataScope(
            @RequestParam(required = false, defaultValue = "0") Long roleId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        Long rid = normalizeRoleId(roleId);
        if (rid == null) {
            res.put("message", "请先选择要配置的角色");
            return ResponseEntity.badRequest().body(res);
        }
        // 内置基础角色不可修改数据权限
        if (isBuiltinRoleId(rid)) {
            res.put("message", "系统预置基础角色（管理员/教师/学生/维护人员）的数据权限不可修改。如需差异化配置请新建自定义角色。");
            return ResponseEntity.badRequest().body(res);
        }
        String scopeType = body.get("scopeType") != null ? body.get("scopeType").toString() : "ALL";

        List<String> customLabIds = toStringList(body.get("customLabIds"));
        // 空列表按用户勾选结果落库，不再强制默认 VIEW，以便与「取消勾选」语义一致
        List<String> deviceScope = toStringList(body.get("deviceScope"));
        List<String> bookingScope = toStringList(body.get("bookingScope"));
        List<String> userScope = toStringList(body.get("userScope"));

        RoleDataScope existing = roleDataScopeMapper.selectOne(
                new QueryWrapper<RoleDataScope>().eq("role_id", rid));
        if (existing == null) {
            RoleDataScope ns = new RoleDataScope();
            ns.setRoleId(rid);
            ns.setScopeType(scopeType);
            ns.setCustomLabIds(String.join(",", customLabIds));
            ns.setDeviceScope(String.join(",", deviceScope));
            ns.setBookingScope(String.join(",", bookingScope));
            ns.setUserScope(String.join(",", userScope));
            roleDataScopeMapper.insert(ns);
        } else {
            UpdateWrapper<RoleDataScope> uw = new UpdateWrapper<>();
            uw.eq("role_id", rid);
            uw.set("scope_type", scopeType);
            uw.set("custom_lab_ids", String.join(",", customLabIds));
            uw.set("device_scope", String.join(",", deviceScope));
            uw.set("booking_scope", String.join(",", bookingScope));
            uw.set("user_scope", String.join(",", userScope));
            roleDataScopeMapper.update(null, uw);
        }
        saveDataScopeAudit(rid, scopeType, customLabIds, deviceScope, bookingScope, userScope);
        res.put("message", "数据权限保存成功");
        return ResponseEntity.ok(res);
    }

    /**
     * 数据权限保存写入 sys_log，供「权限变更审计」Tab 展示（operation=DATA_SCOPE_SAVE）
     */
    private void saveDataScopeAudit(Long roleId, String scopeType, List<String> customLabIds,
            List<String> deviceScope, List<String> bookingScope, List<String> userScope) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String operator = auth != null ? auth.getName() : "unknown";
            SysUser opUser = null;
            try {
                opUser = sysUserService.getByUsername(operator);
            } catch (Exception ignored) {
            }
            SysRole role = sysRoleMapper.selectById(roleId);
            String rname = role != null && StringUtils.hasText(role.getRoleName()) ? role.getRoleName() : "未知角色";
            String rcode = role != null && StringUtils.hasText(role.getRoleCode()) ? role.getRoleCode() : String.valueOf(roleId);
            String detail = String.format(
                    "角色 [%s]（%s）数据权限已更新：scopeType=%s；customLabIds=%s；deviceScope=%s；bookingScope=%s；userScope=%s",
                    rname, rcode,
                    scopeType != null ? scopeType : "",
                    String.join(",", customLabIds != null ? customLabIds : Collections.emptyList()),
                    String.join(",", deviceScope != null ? deviceScope : Collections.emptyList()),
                    String.join(",", bookingScope != null ? bookingScope : Collections.emptyList()),
                    String.join(",", userScope != null ? userScope : Collections.emptyList()));
            SysLog audit = new SysLog();
            audit.setUsername(operator);
            if (opUser != null) {
                audit.setUserId(opUser.getId());
                audit.setUserType(opUser.getUserType());
            }
            audit.setModule("权限分配");
            audit.setOperation("DATA_SCOPE_SAVE");
            audit.setRequestUrl("/permission/data-scope");
            audit.setRequestParams(detail);
            audit.setIpAddress(resolveAuditIp());
            audit.setStatus(1);
            audit.setCreateTime(LocalDateTime.now());
            sysLogService.save(audit);
        } catch (Exception ignored) {
        }
    }

    private static String resolveAuditIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            javax.servlet.http.HttpServletRequest req = attrs.getRequest();
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

    @GetMapping("/lab-list")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> labList() {
        // 优先使用实验室主数据，避免“设备里出现过的实验室”与“实验室管理台账”不一致
        List<com.lab.reservation.entity.Laboratory> labs = laboratoryMapper.selectList(
                new QueryWrapper<com.lab.reservation.entity.Laboratory>()
                        .select("lab_name")
                        .eq("status", 1)
                        .isNotNull("lab_name")
                        .ne("lab_name", "")
                        .orderByAsc("lab_name")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (com.lab.reservation.entity.Laboratory lab : labs) {
            if (lab == null || lab.getLabName() == null) {
                continue;
            }
            String labName = lab.getLabName().trim();
            if (labName.isEmpty() || !dedup.add(labName)) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            // id 与 labName 保持同值，兼容前端 customLabIds 的字符串匹配逻辑
            m.put("id", labName);
            m.put("labName", labName);
            result.add(m);
        }

        // 兜底：若实验室台账为空，回退到设备表去重，避免列表全空
        if (result.isEmpty()) {
            List<Map<String, Object>> raw = deviceInfoMapper.selectMaps(
                    new QueryWrapper<com.lab.reservation.entity.DeviceInfo>()
                            .select("laboratory AS labName")
                            .isNotNull("laboratory")
                            .ne("laboratory", "")
                            .eq("deleted", 0)
                            .last("GROUP BY laboratory ORDER BY laboratory ASC"));
            for (Map<String, Object> r : raw) {
                Object nameObj = r.get("labName");
                if (nameObj == null) {
                    continue;
                }
                String labName = nameObj.toString().trim();
                if (labName.isEmpty() || !dedup.add(labName)) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("id", labName);
                m.put("labName", labName);
                result.add(m);
            }
        }

        return result;
    }

    private static Long normalizeRoleId(Long roleId) {
        if (roleId == null || roleId <= 0L) {
            return null;
        }
        return roleId;
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static Map<String, Object> permToNode(SysPermission p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("parentId", p.getParentId() == null ? 0L : p.getParentId());
        m.put("permName", p.getPermissionName());
        String t = p.getPermissionType();
        if (t == null) {
            m.put("permType", "MENU");
        } else if ("button".equalsIgnoreCase(t)) {
            m.put("permType", "BUTTON");
        } else if ("menu".equalsIgnoreCase(t)) {
            m.put("permType", "MENU");
        } else {
            m.put("permType", t.toUpperCase());
        }
        m.put("path", p.getPath());
        m.put("permissionCode", p.getPermissionCode());
        m.put("icon", p.getIcon());
        m.put("sort", p.getSort());
        m.put("children", new ArrayList<Map<String, Object>>());
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> buildTree(List<Map<String, Object>> flat, long parentId) {
        List<Map<String, Object>> roots = flat.stream()
                .filter(n -> {
                    Object pid = n.get("parentId");
                    long p = pid == null ? 0L : ((Number) pid).longValue();
                    return p == parentId;
                })
                .collect(Collectors.toList());
        for (Map<String, Object> node : roots) {
            long id = ((Number) node.get("id")).longValue();
            List<Map<String, Object>> children = buildTree(flat, id);
            node.put("children", children);
        }
        return roots;
    }

    /**
     * 判断角色ID是否为系统预置五大基础角色（id 1~5 对应 SYSTEM_ADMIN/LAB_ADMIN/TEACHER/STUDENT/MAINTAINER，
     * 与 AdminPasswordInitRunner、RoleController.isBuiltin 保持一致）。
     * 自定义角色 ID > 5，不会被误判。
     */
    private boolean isBuiltinRoleId(Long roleId) {
        if (roleId == null) {
            return false;
        }
        if (roleId >= 1L && roleId <= 5L) {
            return true;
        }
        // 双重保险：查库确认（防止 ID 段被意外占用）
        try {
            SysRole r = sysRoleMapper.selectById(roleId);
            if (r != null && r.getRoleCode() != null) {
                String code = r.getRoleCode().trim().toUpperCase();
                return "SYSTEM_ADMIN".equals(code) || "LAB_ADMIN".equals(code)
                        || "TEACHER".equals(code) || "STUDENT".equals(code)
                        || "MAINTAINER".equals(code);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static List<String> parseCommaList(String s) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(s.split(","));
    }
}
