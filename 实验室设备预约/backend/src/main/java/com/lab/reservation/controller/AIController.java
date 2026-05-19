package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.AiChatLog;
import com.lab.reservation.entity.KnowledgeBase;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.AiChatLogMapper;
import com.lab.reservation.service.KnowledgeBaseService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI实验助手控制器
 * 处理智能问答相关请求
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AiChatLogMapper aiChatLogMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);
        return user != null ? user.getId() : null;
    }

    /**
     * 智能问答
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String category = request.get("category");
        Long userId = getCurrentUserId();

        Map<String, Object> answerResult = knowledgeBaseService.smartQueryWithDangerLevel(question, userId, category);
        return ResponseEntity.ok(answerResult);
    }

    /**
     * 获取当前用户的AI聊天历史
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam(defaultValue = "50") Integer limit) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "用户未登录");
            return ResponseEntity.status(401).body(err);
        }
        
        List<AiChatLog> history = aiChatLogMapper.selectRecentByUserId(userId, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("list", history);
        result.put("total", history.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取知识库分类
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(Arrays.asList("设备操作手册", "实验流程", "故障排查", "安全规范", "常见问题"));
    }

    /**
     * 根据分类获取知识列表
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeBase>> getByCategory(@PathVariable String category) {
        List<KnowledgeBase> list = knowledgeBaseService.listByCategory(category);
        return ResponseEntity.ok(list);
    }

    /**
     * 获取危险操作提醒
     */
    @GetMapping("/danger-alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeBase>> getDangerAlerts() {
        List<KnowledgeBase> list = knowledgeBaseService.getDangerAlerts();
        return ResponseEntity.ok(list);
    }

    /**
     * 搜索知识
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeBase>> search(@RequestParam String keyword) {
        List<KnowledgeBase> list = knowledgeBaseService.searchByKeyword(keyword);
        return ResponseEntity.ok(list);
    }

    /**
     * 根据设备ID获取关联知识（设备详情页展示）
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeBase>> getByDevice(@PathVariable Long deviceId) {
        List<KnowledgeBase> list = knowledgeBaseService.listByDeviceId(deviceId);
        return ResponseEntity.ok(list);
    }

    // ==================== 知识库管理（管理员） ====================

    /**
     * 知识库列表（分页，支持分类/关键词筛选）
     */
    @GetMapping("/manage/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('knowledge') or hasAuthority('knowledge-list') or hasAuthority('knowledge:add') or hasAuthority('knowledge:edit') or hasAuthority('knowledge:delete')")
    public ResponseEntity<Map<String, Object>> manageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        IPage<KnowledgeBase> page = knowledgeBaseService.pageManage(pageNum, pageSize, category, keyword, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取知识详情
     */
    @GetMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('knowledge') or hasAuthority('knowledge-list') or hasAuthority('knowledge:add') or hasAuthority('knowledge:edit') or hasAuthority('knowledge:delete')")
    public ResponseEntity<KnowledgeBase> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.getById(id));
    }

    /**
     * 新增知识条目
     */
    @PostMapping("/manage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('knowledge') or hasAuthority('knowledge-list') or hasAuthority('knowledge:add') or hasAuthority('knowledge:edit') or hasAuthority('knowledge:delete')")
    public ResponseEntity<Map<String, Object>> addKnowledge(@RequestBody KnowledgeBase kb) {
        knowledgeBaseService.save(kb);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "添加成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 更新知识条目
     */
    @PutMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('knowledge') or hasAuthority('knowledge-list') or hasAuthority('knowledge:add') or hasAuthority('knowledge:edit') or hasAuthority('knowledge:delete')")
    public ResponseEntity<Map<String, Object>> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeBase kb) {
        kb.setId(id);
        knowledgeBaseService.updateById(kb);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除知识条目
     */
    @DeleteMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('knowledge') or hasAuthority('knowledge-list') or hasAuthority('knowledge:add') or hasAuthority('knowledge:edit') or hasAuthority('knowledge:delete')")
    public ResponseEntity<Map<String, Object>> deleteKnowledge(@PathVariable Long id) {
        knowledgeBaseService.removeById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    // ==================== AI 交互日志审计（系统管理员） ====================

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> auditLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) String quality,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseDt(startTime);
        LocalDateTime et = parseDt(endTime);
        String kbCategory = sessionTypeToKbCategory(sessionType);
        Page<AiChatLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        Page<AiChatLog> result = (Page<AiChatLog>) aiChatLogMapper.selectAuditPage(
                page, username, userType, kbCategory, quality, st, et);
        for (AiChatLog log : result.getRecords()) {
            applySafetyFields(log);
        }
        Map<String, Object> statsRaw = aiChatLogMapper.selectAuditStats(
                username, userType, kbCategory, quality, st, et);
        Map<String, Object> stats = new HashMap<>();
        if (statsRaw != null) {
            stats.put("normal", toLong(statsRaw.get("normal")));
            stats.put("abnormal", toLong(statsRaw.get("abnormal")));
            stats.put("lowQuality", toLong(statsRaw.get("lowQuality")));
            stats.put("safetyAlert", toLong(statsRaw.get("safetyAlert")));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("list", result.getRecords());
        body.put("total", result.getTotal());
        body.put("stats", stats);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<AiChatLog> auditLogDetail(@PathVariable Long id) {
        AiChatLog log = aiChatLogMapper.selectById(id);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        if (log.getUserId() != null) {
            SysUser u = sysUserService.getById(log.getUserId());
            if (u != null) {
                log.setUsername(u.getUsername());
                log.setUserType(u.getUserType());
            }
        }
        if (log.getKnowledgeId() != null) {
            KnowledgeBase kb = knowledgeBaseService.getById(log.getKnowledgeId());
            if (kb != null) {
                log.setSessionType(sessionTypeToKbCategoryInverse(kb.getCategory()));
            }
        }
        if (log.getKnowledgeId() == null) {
            log.setQuality("ABNORMAL");
        } else if (log.getConfidence() != null && log.getConfidence() < 0.3) {
            log.setQuality("LOW_QUALITY");
        } else {
            log.setQuality("NORMAL");
        }
        applySafetyFields(log);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/logs/export")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) String quality,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) throws Exception {
        LocalDateTime st = parseDt(startTime);
        LocalDateTime et = parseDt(endTime);
        String kbCategory = sessionTypeToKbCategory(sessionType);
        Page<AiChatLog> page = new Page<>(1, 5000);
        Page<AiChatLog> result = (Page<AiChatLog>) aiChatLogMapper.selectAuditPage(
                page, username, userType, kbCategory, quality, st, et);
        StringBuilder csv = new StringBuilder("\uFEFF用户,角色,类型,问题摘要,质量,响应ms,时间\n");
        for (AiChatLog log : result.getRecords()) {
            applySafetyFields(log);
            String q = log.getQuestion() == null ? "" : log.getQuestion();
            if (q.length() > 80) {
                q = q.substring(0, 80) + "...";
            }
            csv.append(escCsv(log.getUsername())).append(',')
                    .append(escCsv(log.getUserType())).append(',')
                    .append(escCsv(log.getSessionType())).append(',')
                    .append(escCsv(q)).append(',')
                    .append(escCsv(log.getQuality())).append(',')
                    .append(log.getResponseTime() == null ? "" : log.getResponseTime()).append(',')
                    .append(escCsv(log.getCreateTime())).append('\n');
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "ai_chat_logs_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                        + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private static void applySafetyFields(AiChatLog log) {
        boolean safe = log.getAnswer() != null
                && (log.getAnswer().contains("⚠️") || log.getAnswer().contains("安全警告"));
        log.setSafetyAlert(safe);
        log.setSafetyAlertText(safe ? "安全提示" : null);
    }

    private static String sessionTypeToKbCategory(String sessionType) {
        if (sessionType == null || sessionType.isEmpty()) {
            return null;
        }
        switch (sessionType) {
            case "DEVICE_OPERATION":
                return "设备操作手册";
            case "EXPERIMENT_PROCESS":
                return "实验流程";
            case "SAFETY":
                return "安全规范";
            case "TROUBLESHOOTING":
                return "故障排查";
            default:
                return null;
        }
    }

    private static String sessionTypeToKbCategoryInverse(String category) {
        if (category == null) {
            return "DEVICE_OPERATION";
        }
        switch (category) {
            case "设备操作手册":
                return "DEVICE_OPERATION";
            case "实验流程":
                return "EXPERIMENT_PROCESS";
            case "安全规范":
                return "SAFETY";
            case "故障排查":
                return "TROUBLESHOOTING";
            default:
                return "DEVICE_OPERATION";
        }
    }

    private static LocalDateTime parseDt(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String escCsv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
