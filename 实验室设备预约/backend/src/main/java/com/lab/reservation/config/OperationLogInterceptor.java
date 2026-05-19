package com.lab.reservation.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.entity.SysLog;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统操作日志拦截器
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {
    private final SysLogService sysLogService;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;

    public OperationLogInterceptor(SysLogService sysLogService, SysUserService sysUserService, ObjectMapper objectMapper) {
        this.sysLogService = sysLogService;
        this.sysUserService = sysUserService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("logStartTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (uri == null || uri.startsWith("/sys/log")) {
            return;
        }
        // 跳过多媒体上传，避免记录大体积请求体
        if (uri.contains("/upload-image")) {
            return;
        }

        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        Long start = (Long) request.getAttribute("logStartTime");
        long duration = start == null ? 0L : (System.currentTimeMillis() - start);

        String username = getCurrentUsername();
        SysUser currentUser = resolveCurrentUser(username);
        Long userId = currentUser != null ? currentUser.getId() : null;

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String controllerName = handlerMethod.getBeanType().getSimpleName();
        String methodName = handlerMethod.getMethod().getName();

        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(currentUser != null ? currentUser.getUserType() : null);
        log.setModule(resolveModule(controllerName));
        log.setOperation(resolveOperation(request, request.getMethod(), methodName));
        log.setMethod(request.getMethod());
        log.setRequestUrl(uri);
        log.setRequestParams(truncate(serializeObject(buildRequestParams(request)), 2000));
        log.setIpAddress(resolveIp(request));
        log.setDuration(duration);
        log.setStatus(ex == null ? 1 : 0);
        log.setErrorMsg(ex == null ? null : truncate(ex.getMessage(), 1000));
        log.setCreateTime(LocalDateTime.now());

        try {
            sysLogService.save(log);
        } catch (Exception ignored) {
            // 记录失败不影响业务流程
        }
    }

    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                if (username != null && username.length() > 0 && !"anonymousUser".equalsIgnoreCase(username)) {
                    return username;
                }
            }
        } catch (Exception ignored) {
        }
        return "anonymous";
    }

    private SysUser resolveCurrentUser(String username) {
        if (username == null || "anonymous".equalsIgnoreCase(username)) {
            return null;
        }
        try {
            return sysUserService.getByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveModule(String controllerName) {
        if (controllerName == null) {
            return "系统";
        }
        String name = controllerName.endsWith("Controller")
                ? controllerName.substring(0, controllerName.length() - "Controller".length())
                : controllerName;
        switch (name) {
            case "Auth":
                return "认证登录";
            case "User":
                return "用户管理";
            case "Device":
                return "设备管理";
            case "Category":
                return "设备分类";
            case "Booking":
                return "预约管理";
            case "BookingRule":
                return "预约规则";
            case "Repair":
                return "维修工单";
            case "Calibration":
                return "校准管理";
            case "HealthRecord":
                return "健康档案";
            case "Statistics":
                return "数据统计";
            case "SysConfig":
                return "系统配置";
            case "Role":
                return "角色管理";
            case "Permission":
                return "权限分配";
            case "SysMessage":
                return "系统消息";
            case "Laboratory":
                return "实验室管理";
            case "AI":
            case "AiAssistant":
                return "AI助手";
            case "SysLog":
                return "系统日志";
            case "Feedback":
                return "教学反馈";
            case "KnowledgeBase":
                return "知识库";
            case "ExperimentProject":
                return "实验项目";
            default:
                return name;
        }
    }

    private String resolveOperation(HttpServletRequest request, String httpMethod, String methodName) {
        String uri = request.getRequestURI();
        if (uri != null && (uri.contains("/auth/login") || uri.contains("/user/login"))) {
            return "LOGIN";
        }
        if (uri != null && (uri.contains("/auth/logout") || uri.contains("/user/logout"))) {
            return "LOGOUT";
        }
        if ("GET".equalsIgnoreCase(httpMethod)) return "QUERY";
        if ("POST".equalsIgnoreCase(httpMethod)) return "CREATE";
        if ("PUT".equalsIgnoreCase(httpMethod)) return "UPDATE";
        if ("DELETE".equalsIgnoreCase(httpMethod)) return "DELETE";
        return methodName;
    }

    private String resolveIp(HttpServletRequest request) {
        String[] headers = new String[]{"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private Map<String, Object> buildRequestParams(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        try {
            Enumeration<String> names = request.getParameterNames();
            if (names == null) {
                return Collections.emptyMap();
            }
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                map.put(name, request.getParameter(name));
            }
            String ua = request.getHeader("User-Agent");
            if (ua != null && !ua.isEmpty()) {
                map.put("_userAgent", ua);
            }
            // 登录接口由 AuthController 传入 userType
            Object ut = request.getAttribute("logUserType");
            if (ut != null) {
                map.put("userType", ut.toString());
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private String serializeObject(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
