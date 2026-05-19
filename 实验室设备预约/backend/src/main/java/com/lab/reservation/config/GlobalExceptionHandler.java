package com.lab.reservation.config;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一返回格式：{ success: false, message: "..." }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== Spring Security 异常 =====
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        System.err.println("[AccessDenied] " + req.getMethod() + " " + req.getRequestURI() + " → " + e.getMessage());
        return json(HttpStatus.FORBIDDEN, "没有权限执行此操作");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException e, HttpServletRequest req) {
        System.err.println("[AuthException] " + req.getMethod() + " " + req.getRequestURI() + " → " + e.getMessage());
        return json(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "认证失败，请重新登录");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return json(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }

    // ===== 数据完整性：唯一约束、外键等 =====
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException e) {
        String raw = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        System.err.println("[DataIntegrityViolation] " + raw);
        return bad(friendlyConstraintMessage(raw));
    }

    // ===== MyBatis / JDBC 异常 =====
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, Object>> handlePersistence(PersistenceException e) {
        String msg = collectCauseMessages(e);
        System.err.println("[PersistenceException] " + msg);
        return bad(friendlyConstraintMessage(msg));
    }

    // ===== Spring Validation（@Valid 验证失败） =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("；"));
        System.err.println("[ValidationError] " + msg);
        return bad(msg.isEmpty() ? "参数验证失败" : msg);
    }

    // ===== 请求体解析失败（如 JSON 格式错误/缺失） =====
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        String msg = "请求数据格式错误";
        Throwable cause = e.getCause();
        if (cause != null) {
            String cm = cause.getMessage() != null ? cause.getMessage() : "";
            if (cm.contains("Required request body is missing")) {
                msg = "请求体不能为空";
            } else if (cm.contains("JSON parse error")) {
                msg = "JSON 格式错误";
            }
        }
        Throwable walk = cause;
        while (walk != null) {
            if (walk instanceof InvalidFormatException) {
                InvalidFormatException ife = (InvalidFormatException) walk;
                String field = "请求字段";
                if (ife.getPath() != null && !ife.getPath().isEmpty()) {
                    field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
                    if (field == null || field.isEmpty()) {
                        field = "请求字段";
                    }
                }
                msg = "字段「" + field + "」格式不正确（例如日期需为 yyyy-MM-dd，数字字段勿填文字）";
                break;
            }
            walk = walk.getCause();
        }
        System.err.println("[HttpMessageNotReadable] " + msg + " | cause: " + e.getMessage());
        return bad(msg);
    }

    // ===== 普通绑定/参数错误 =====
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() != null ? err.getDefaultMessage() : "无效值"))
                .collect(Collectors.joining("；"));
        return bad(msg.isEmpty() ? "请求参数格式错误" : msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return bad("缺少请求参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expected = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "合法值";
        return bad("参数类型错误: " + e.getName() + " 应为 " + expected);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException e) {
        return json(HttpStatus.NOT_FOUND, "请求的资源不存在");
    }

    // ===== 业务层 RuntimeException =====
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e, HttpServletRequest req) {
        System.err.println("[RuntimeException] " + req.getMethod() + " " + req.getRequestURI() + " → " + e.getMessage());
        e.printStackTrace();
        // 返回与业务接口一致的格式 { success: false, message: "..." }
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            m.put("message", e.getMessage());
        } else {
            m.put("message", "操作失败，请稍后重试");
        }
        return ResponseEntity.badRequest().body(m);
    }

    // ===== 兜底 =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e, HttpServletRequest req) {
        System.err.println("[" + e.getClass().getSimpleName() + "] " + req.getMethod() + " " + req.getRequestURI() + " → " + e.getMessage());
        e.printStackTrace();
        // 返回与业务接口一致的格式 { success: false, message: "..." }
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            m.put("message", "服务器内部错误: " + e.getMessage());
        } else {
            m.put("message", "服务器内部错误: " + e.getClass().getSimpleName());
        }
        return ResponseEntity.badRequest().body(m);
    }

    // ===== 内部工具方法 =====
    private static ResponseEntity<Map<String, Object>> bad(String message) {
        return json(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<Map<String, Object>> json(HttpStatus status, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", message);
        return ResponseEntity.status(status).body(m);
    }

    private static String collectCauseMessages(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable t = e;
        while (t != null) {
            if (t.getMessage() != null && !t.getMessage().equals(t.getClass().getName())) {
                if (sb.length() > 0) sb.append(" ← ");
                sb.append(t.getMessage());
            }
            t = t.getCause();
        }
        return sb.length() > 0 ? sb.toString() : (e.getMessage() != null ? e.getMessage() : "");
    }

    private static String friendlyConstraintMessage(String raw) {
        if (raw == null) return "数据保存失败：存在重复或冲突的记录";
        String r = raw.toLowerCase();
        if (r.contains("duplicate entry") || r.contains("unique constraint") || r.contains("unique index")) {
            if (r.contains("username") || r.contains("user_name")) return "用户名已存在";
            if (r.contains("role_code") || r.contains("rolecode")) return "角色编码已存在";
            if (r.contains("device_no") || r.contains("deviceno")) return "设备编号已存在";
            if (r.contains("phone")) return "手机号已被注册";
            if (r.contains("email")) return "邮箱已被注册";
            return "数据重复：唯一字段（编码/编号/用户名）已被占用";
        }
        if (r.contains("foreign key") || r.contains("child row")) {
            return "操作失败：关联数据被占用，请先删除子记录";
        }
        return "数据保存失败：" + raw;
    }
}
