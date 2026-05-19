package com.lab.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.DataScopeContext;
import com.lab.reservation.entity.RoleDataScope;
import com.lab.reservation.entity.SysRole;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.RoleDataScopeMapper;
import com.lab.reservation.mapper.SysRoleMapper;
import com.lab.reservation.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据权限服务
 * 用于获取和管理当前用户的数据权限范围
 */
@Service
public class DataScopeService {

    private static final Logger log = LoggerFactory.getLogger(DataScopeService.class);

    private static final Set<String> BUILTIN_ROLE_CODES = new HashSet<>(Arrays.asList(
            "SYSTEM_ADMIN", "LAB_ADMIN", "TEACHER", "STUDENT", "MAINTAINER"));

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RoleDataScopeMapper roleDataScopeMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 获取当前用户的数据权限上下文
     * 合并逻辑（优先级从高到低）：
     *   ALL > CUSTOM > DEPT > SELF（默认）
     * 各维度权限（VIEW/EDIT 等）取各角色的并集。
     * 若无任何角色或无任何数据范围记录 → 最严格的 SELF。
     */
    public DataScopeContext getCurrentDataScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        String username = auth.getName();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            log.warn("[DataScope] 未找到用户: {}", username);
            return null;
        }

        DataScopeContext context = new DataScopeContext();
        context.setUserId(user.getId());
        context.setUserType(user.getUserType());
        context.setDepartment(user.getDepartment());
        context.setLaboratory(user.getLaboratory());

        // 系统管理员：不合并角色数据权限，始终全量（避免无 role_data_scope 时被默认成仅 VIEW）
        if ("SYSTEM_ADMIN".equals(user.getUserType())) {
            context.setScopeType("ALL");
            context.setCustomLabIds("");
            context.setDeviceScope("VIEW,EDIT,DELETE,STATUS");
            context.setBookingScope("VIEW,AUDIT,CANCEL,EXPORT");
            context.setUserScope("VIEW,EDIT,ROLE,DELETE");
            return context;
        }

        List<String> roleCodes = sysUserMapper.selectUserRoles(user.getId());
        log.debug("[DataScope] 用户 {} (id={}) 角色列表: {}", username, user.getId(), roleCodes);

        if (roleCodes == null || roleCodes.isEmpty()) {
            log.debug("[DataScope] 用户 {} 无角色 → SELF", username);
            applyNarrowDefaults(context);
            return context;
        }

        // 合并变量
        boolean anyAll = false;
        boolean anyDept = false;
        boolean anyCustom = false;
        boolean anySelf = false;
        final Set<String> mergedLabs = new LinkedHashSet<>();
        String mergedDevice = null;
        String mergedBooking = null;
        String mergedUser = null;
        boolean anyDataScopeRow = false;

        for (String roleCode : roleCodes) {
            if (roleCode == null || roleCode.trim().isEmpty()) {
                continue;
            }
            String rc = roleCode.trim().toUpperCase(Locale.ROOT);
            SysRole role = sysRoleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", rc));
            if (role == null) {
                log.debug("[DataScope] 角色 {} 在 sys_role 表中不存在，跳过", rc);
                continue;
            }

            RoleDataScope ds = roleDataScopeMapper.selectOne(
                    new QueryWrapper<RoleDataScope>().eq("role_id", role.getId()));

            if (ds == null) {
                log.debug("[DataScope] 角色 {} (id={}) 无 role_data_scope 记录 → 视为 SELF（最严格）", rc, role.getId());
                // 内置角色 + 无记录 = 视为 SELF，不影响 anyAll/anyDept/anyCustom 标记
                // 但 mergedScope 字段可能为空，下面用 applyNarrowDefaults 兜底
                anySelf = true;
                continue;
            }

            anyDataScopeRow = true;

            String st = (ds.getScopeType() != null ? ds.getScopeType().trim().toUpperCase(Locale.ROOT) : "SELF");
            log.debug("[DataScope] 角色 {} → scopeType={}, bookingScope={}, deviceScope={}",
                    rc, st, ds.getBookingScope(), ds.getDeviceScope());

            switch (st) {
                case "ALL":
                    anyAll = true;
                    break;
                case "DEPT":
                    anyDept = true;
                    break;
                case "CUSTOM":
                    anyCustom = true;
                    if (ds.getCustomLabIds() != null && !ds.getCustomLabIds().isEmpty()) {
                        mergedLabs.addAll(parseLabIds(ds.getCustomLabIds()));
                    }
                    break;
                default:
                    // 包括 SELF 和空串
                    anySelf = true;
                    break;
            }

            // 权限维度：各角色取并集
            mergedDevice = unionCsv(mergedDevice, ds.getDeviceScope());
            mergedBooking = unionCsv(mergedBooking, ds.getBookingScope());
            mergedUser = unionCsv(mergedUser, ds.getUserScope());
        }

        // 确定 scopeType（优先级：ALL > CUSTOM > DEPT > SELF）
        // 注意：只要角色配置了 scope_type=CUSTOM，就必须保留为 CUSTOM，即使 custom_lab_ids 暂时为空
        // （否则会被误判为 SELF，设备列表走 default 分支仅按 status=0 过滤，出现「全实验室空闲设备」）
        String finalScopeType = "SELF";
        if (anyAll) {
            finalScopeType = "ALL";
        } else if (anyCustom) {
            finalScopeType = "CUSTOM";
        } else if (anyDept) {
            finalScopeType = "DEPT";
        }
        if ("CUSTOM".equals(finalScopeType)) {
            context.setCustomLabIds(String.join(",", mergedLabs));
        } else {
            context.setCustomLabIds("");
        }

        context.setScopeType(finalScopeType);
        log.debug("[DataScope] 用户 {} 最终 scopeType={}, mergedBooking={}", username, finalScopeType, mergedBooking);

        // 各维度权限：只要存在 role_data_scope 行，空串表示管理员明确取消勾选，不得默认回填 VIEW
        if (anyDataScopeRow) {
            context.setDeviceScope(mergedDevice != null && !mergedDevice.trim().isEmpty() ? mergedDevice.trim() : "");
            context.setBookingScope(mergedBooking != null && !mergedBooking.trim().isEmpty() ? mergedBooking.trim() : "");
            context.setUserScope(mergedUser != null && !mergedUser.trim().isEmpty() ? mergedUser.trim() : "");
        } else {
            // 无任何角色的数据范围配置：实验室管理员默认可管理设备；其余默认仅 VIEW
            String deviceDef = "VIEW";
            if ("LAB_ADMIN".equals(user.getUserType())) {
                deviceDef = "VIEW,EDIT,DELETE,STATUS";
            }
            context.setDeviceScope(mergedDevice != null && !mergedDevice.trim().isEmpty() ? mergedDevice.trim() : deviceDef);
            // 预置实验室管理员/教师：未单独配置 role_data_scope 时，预约具备审核/取消/导出能力（与任务书一致）
            String bookingDef = "VIEW";
            if ("LAB_ADMIN".equals(user.getUserType()) || "TEACHER".equals(user.getUserType())) {
                bookingDef = "VIEW,AUDIT,CANCEL,EXPORT";
            }
            context.setBookingScope(mergedBooking != null && !mergedBooking.trim().isEmpty() ? mergedBooking.trim() : bookingDef);
            context.setUserScope(mergedUser != null && !mergedUser.trim().isEmpty() ? mergedUser.trim() : "VIEW");
        }

        return context;
    }

    private static boolean isBuiltinRoleCode(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return BUILTIN_ROLE_CODES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private static void applyNarrowDefaults(DataScopeContext context) {
        context.setScopeType("SELF");
        context.setCustomLabIds("");
        context.setDeviceScope("VIEW");
        context.setBookingScope("VIEW");
        context.setUserScope("VIEW");
    }

    private static String firstNonBlank(String v, String def) {
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        return def;
    }

    /**
     * 合并逗号分隔的权限标记（去重保序）
     */
    private static String unionCsv(String a, String b) {
        if (b == null || b.trim().isEmpty()) {
            return a;
        }
        Set<String> set = new LinkedHashSet<>();
        if (a != null && !a.trim().isEmpty()) {
            for (String p : a.split(",")) {
                if (!p.trim().isEmpty()) {
                    set.add(p.trim());
                }
            }
        }
        for (String p : b.split(",")) {
            if (!p.trim().isEmpty()) {
                set.add(p.trim());
            }
        }
        if (set.isEmpty()) {
            return a;
        }
        return String.join(",", set);
    }

    /**
     * 根据数据权限上下文构建设备查询条件
     * @param context 数据权限上下文
     * @return QueryWrapper，如果返回null表示无权限查询
     */
    public QueryWrapper<?> buildDeviceScopeWrapper(DataScopeContext context) {
        if (context == null) {
            return null;
        }

        // 系统管理员可查看所有设备（不按数据范围过滤实验室）
        if (context.isSystemAdmin()) {
            return new QueryWrapper<>();
        }

        QueryWrapper<?> wrapper = new QueryWrapper<>();

        // 根据 scopeType 添加不同的过滤条件（scopeType=ALL 时由调用方结合 canViewDevice 处理，此处不按实验室过滤）
        switch (context.getScopeType() != null ? context.getScopeType().trim().toUpperCase(Locale.ROOT) : "SELF") {
            case "ALL":
                break;
            case "DEPT":
                // 部门级：只能查看自己实验室的设备（通过 laboratory 字段精确匹配）
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else {
                    // 如果没有实验室信息，只能看自己的
                    wrapper.eq("1", 0); // 无数据
                }
                break;

            case "SELF":
                // 仅本人：可见「本人所属实验室」下的设备（与 DeviceController 一致）
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else {
                    wrapper.eq("1", 0);
                }
                break;

            case "CUSTOM":
                // 自定义：只能查看指定实验室的设备
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().isEmpty()) {
                    wrapper.in("laboratory", parseLabIds(context.getCustomLabIds()));
                } else {
                    wrapper.eq("1", 0);
                }
                break;

            default:
                // 未知类型，默认无权限
                wrapper.eq("1", 0);
                break;
        }

        return wrapper;
    }

    /**
     * 根据数据权限上下文构建预约查询条件
     * @param context 数据权限上下文
     * @return QueryWrapper
     */
    public QueryWrapper<?> buildBookingScopeWrapper(DataScopeContext context) {
        if (context == null) {
            return null;
        }

        // 系统管理员可查看所有预约
        if (context.isSystemAdmin() || context.hasAllScope()) {
            return new QueryWrapper<>();
        }

        QueryWrapper<?> wrapper = new QueryWrapper<>();

        switch (context.getScopeType()) {
            case "DEPT":
                // 部门级：查看自己实验室的预约（通过设备关联的实验室，精确匹配）
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.exists("SELECT 1 FROM device_info d WHERE d.id = booking_order.device_id AND d.laboratory = '" + context.getLaboratory() + "'");
                } else {
                    // 如果没有实验室信息，只能看自己的
                    wrapper.eq("user_id", context.getUserId());
                }
                break;

            case "SELF":
                // 自助级：只能查看自己的预约
                wrapper.eq("user_id", context.getUserId());
                break;

            case "CUSTOM":
                // 自定义：查看指定实验室的预约
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().isEmpty()) {
                    // 需要关联设备表查询实验室
                    String labs = context.getCustomLabIds();
                    wrapper.exists("SELECT 1 FROM device_info d WHERE d.id = booking_order.device_id AND d.laboratory IN (" +
                            parseLabNames(labs) + ")");
                } else {
                    wrapper.eq("user_id", context.getUserId());
                }
                break;

            default:
                // 未知类型，默认只能看自己的
                wrapper.eq("user_id", context.getUserId());
                break;
        }

        return wrapper;
    }

    /**
     * 根据数据权限上下文构建用户查询条件
     * @param context 数据权限上下文
     * @return QueryWrapper
     */
    public QueryWrapper<?> buildUserScopeWrapper(DataScopeContext context) {
        if (context == null) {
            return null;
        }

        // 系统管理员可查看所有用户
        if (context.isSystemAdmin() || context.hasAllScope()) {
            return new QueryWrapper<>();
        }

        QueryWrapper<?> wrapper = new QueryWrapper<>();

        switch (context.getScopeType()) {
            case "DEPT":
                // 部门级：查看同实验室的用户
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else {
                    wrapper.eq("id", context.getUserId());
                }
                break;

            case "SELF":
                // 自助级：只能查看自己
                wrapper.eq("id", context.getUserId());
                break;

            case "CUSTOM":
                // 自定义：所属实验室在允许列表内的用户（与 device_info / 用户表 laboratory 一致）
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().isEmpty()) {
                    Set<String> labs = parseLabIds(context.getCustomLabIds());
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

        return wrapper;
    }

    /**
     * 检查当前用户是否可以查看某个设备
     * @param deviceLab 设备所属实验室
     * @return true-可以查看，false-不能查看
     */
    public boolean canViewDevice(String deviceLab) {
        DataScopeContext context = getCurrentDataScope();
        if (context == null) {
            return false;
        }

        // 系统管理员、设备维护员：与列表接口一致，不按数据范围限制查看
        if (context.isSystemAdmin() || "MAINTAINER".equals(context.getUserType())) {
            return true;
        }

        // 学生/教师/实验室管理员预约浏览；其余角色须勾选 VIEW
        if (!context.canBrowseReservableDevices()) {
            return false;
        }

        return isDeviceLabInScope(context, deviceLab);
    }

    /**
     * 是否可在数据范围内对指定实验室设备执行编辑类操作（新增/修改设备信息）
     */
    public boolean canEditDeviceInLab(String deviceLab) {
        DataScopeContext context = getCurrentDataScope();
        if (context == null) {
            return false;
        }
        if (context.isSystemAdmin()) {
            return true;
        }
        if (!context.canEditDevice()) {
            return false;
        }
        return isDeviceLabInScope(context, deviceLab);
    }

    /**
     * 是否可删除指定实验室下的设备
     */
    public boolean canDeleteDeviceInLab(String deviceLab) {
        DataScopeContext context = getCurrentDataScope();
        if (context == null) {
            return false;
        }
        if (context.isSystemAdmin()) {
            return true;
        }
        if (!context.canDeleteDevice()) {
            return false;
        }
        return isDeviceLabInScope(context, deviceLab);
    }

    /**
     * 是否可变更指定实验室下设备的状态
     */
    public boolean canChangeDeviceStatusInLab(String deviceLab) {
        DataScopeContext context = getCurrentDataScope();
        if (context == null) {
            return false;
        }
        if (context.isSystemAdmin()) {
            return true;
        }
        if ("MAINTAINER".equals(context.getUserType())) {
            return true;
        }
        if (!context.canChangeDeviceStatusFlag()) {
            return false;
        }
        return isDeviceLabInScope(context, deviceLab);
    }

    /**
     * 设备所属实验室是否在用户数据范围（ALL/DEPT/SELF/CUSTOM）内
     * 与 DeviceController#applyDeviceDataScope 保持一致：
     * - 学生/教师在 laboratory 未维护或 CUSTOM 未配置实验室时，放宽为可浏览（避免列表可见但详情403）
     */
    private boolean isDeviceLabInScope(DataScopeContext context, String deviceLab) {
        if (deviceLab == null) {
            return false;
        }
        String lab = deviceLab.trim();
        String st = context.getScopeType() != null ? context.getScopeType().trim().toUpperCase(Locale.ROOT) : "SELF";
        boolean isStudentOrTeacher = "STUDENT".equals(context.getUserType()) || "TEACHER".equals(context.getUserType());
        switch (st) {
            case "ALL":
                return true;
            case "DEPT":
            case "SELF":
                if (context.getLaboratory() != null && !context.getLaboratory().trim().isEmpty()) {
                    return lab.equals(context.getLaboratory().trim());
                }
                // 与列表逻辑一致：学生/教师未维护 laboratory 时放宽
                return isStudentOrTeacher;
            case "CUSTOM":
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().trim().isEmpty()) {
                    Set<String> allowedLabs = parseLabIds(context.getCustomLabIds());
                    if (allowedLabs.isEmpty()) {
                        return isStudentOrTeacher;
                    }
                    return allowedLabs.contains(lab);
                }
                // 与列表逻辑一致：学生/教师 CUSTOM 未配置实验室时放宽
                return isStudentOrTeacher;
            default:
                return false;
        }
    }

    /**
     * 检查当前用户是否可以查看某个预约
     * @param userId 预约申请人ID
     * @param deviceLab 设备所属实验室
     * @return true-可以查看，false-不能查看
     */
    public boolean canViewBooking(Long userId, String deviceLab) {
        DataScopeContext context = getCurrentDataScope();
        if (context == null) {
            return false;
        }

        // 系统管理员可查看所有
        if (context.isSystemAdmin() || context.hasAllScope()) {
            return true;
        }

        // 必须有 VIEW 预约权限
        if (!context.canViewBooking()) {
            return false;
        }

        // 自己的预约总是可以查看
        if (context.getUserId().equals(userId)) {
            return true;
        }

        // 根据 scopeType 检查
        switch (context.getScopeType()) {
            case "DEPT":
                return context.getLaboratory() != null && deviceLab != null &&
                        deviceLab.equals(context.getLaboratory());

            case "SELF":
                return false;

            case "CUSTOM":
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().isEmpty()) {
                    Set<String> allowedLabs = parseLabIds(context.getCustomLabIds());
                    return deviceLab != null && allowedLabs.contains(deviceLab.trim());
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * 解析实验室ID列表
     */
    private Set<String> parseLabIds(String labIds) {
        if (labIds == null || labIds.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(labIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 解析实验室名称列表（用于SQL IN查询）
     */
    private String parseLabNames(String labIds) {
        Set<String> labs = parseLabIds(labIds);
        return labs.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(","));
    }
}
