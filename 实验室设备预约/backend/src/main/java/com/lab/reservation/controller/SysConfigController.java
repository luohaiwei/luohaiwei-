package com.lab.reservation.controller;

import com.lab.reservation.entity.SysConfig;
import com.lab.reservation.mapper.SysConfigMapper;
import com.lab.reservation.service.DatabaseBackupService;
import com.lab.reservation.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 * 数据备份与恢复 + 系统配置管理
 */
@RestController
@RequestMapping("/sys/config")
@CrossOrigin
public class SysConfigController {

    private static final Logger log = LoggerFactory.getLogger(SysConfigController.class);

    @Autowired
    private DatabaseBackupService databaseBackupService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    // ==================== 系统配置 ====================

    /**
     * 获取所有配置
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        List<Map<String, Object>> configs = sysConfigService.getAllConfigs();
        Map<String, Object> result = new HashMap<>();
        result.put("list", configs);
        return ResponseEntity.ok(result);
    }

    /**
     * 按分组获取配置
     */
    @GetMapping("/group/{group}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> getConfigsByGroup(@PathVariable String group) {
        List<Map<String, Object>> configs = sysConfigService.getConfigsByGroup(group);
        Map<String, Object> result = new HashMap<>();
        result.put("list", configs);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量保存配置
     */
    @PostMapping("/save")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> saveConfigs(@RequestBody List<Map<String, String>> configs) {
        try {
            sysConfigService.saveConfigs(configs);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "配置保存成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 获取单个配置值（供其他服务读取）
     */
    @GetMapping("/value")
    public ResponseEntity<Map<String, Object>> getConfigValue(@RequestParam String key) {
        String value = sysConfigService.getConfigValue(key);
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        return ResponseEntity.ok(result);
    }

    /**
     * 调试接口：查看配置原始存储值（绕过 parseValue，直接返回数据库原始字符串）
     * 用于排查配置存储/读取异常
     */
    @GetMapping("/raw")
    public ResponseEntity<Map<String, Object>> getRawConfigValue(@RequestParam String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        try {
            SysConfig rawValue = sysConfigMapper.selectByKey(key);
            if (rawValue != null) {
                result.put("configType", rawValue.getConfigType());
                result.put("rawStoredValue", rawValue.getConfigValue());
                result.put("parsedValue", sysConfigService.getConfigValue(key));
            } else {
                result.put("rawStoredValue", "NOT FOUND IN DB");
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // ==================== 数据备份 ====================

    /**
     * 手动备份数据库
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> backup() {
        try {
            String path = databaseBackupService.backup();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "备份成功");
            result.put("path", path);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "备份失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/backup/download")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<byte[]> downloadBackup(@RequestParam String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            byte[] data = Files.readAllBytes(file.toPath());
            String filename = file.getName();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取备份文件列表
     */
    @GetMapping("/backup/list")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> listBackups() {
        List<File> files = databaseBackupService.listBackupFiles();
        List<Map<String, Object>> list = files.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("path", f.getAbsolutePath());
            m.put("name", f.getName());
            m.put("size", f.length());
            m.put("lastModified", f.lastModified());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        return ResponseEntity.ok(result);
    }

    /**
     * 从备份恢复
     */
    @PostMapping("/restore")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> restore(@RequestParam String path) {
        try {
            databaseBackupService.restore(path);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "恢复成功，请重新登录");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "恢复失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 上传备份文件并恢复
     */
    @PostMapping("/restore/upload")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> restoreFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            String tempPath = System.getProperty("java.io.tmpdir") + "/lab_restore_" + System.currentTimeMillis() + ".sql";
            File tempFile = new File(tempPath);
            file.transferTo(tempFile);
            databaseBackupService.restore(tempPath);
            tempFile.delete();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "恢复成功，请重新登录");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "恢复失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 自动备份配置（与前端 BackupRestore 页对齐）
     */
    @GetMapping("/backup/auto-config")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> getAutoBackupConfig() {
        Map<String, Object> m = new HashMap<>();
        boolean enabled = sysConfigService.getBoolean("backup.auto.enabled", false);
        m.put("enabled", enabled);
        String cycle = sysConfigService.getConfigValue("backup.auto.cycle");
        String cycleVal = cycle != null && !cycle.isEmpty() ? cycle.replace("\"", "") : "DAILY";
        m.put("cycle", cycleVal);
        String time = sysConfigService.getConfigValue("backup.auto.time");
        m.put("time", time != null && !time.isEmpty() ? time.replace("\"", "") : "02:00");
        String weekDay = sysConfigService.getConfigValue("backup.auto.weekDay");
        m.put("weekDay", weekDay != null && !weekDay.isEmpty() ? weekDay.replace("\"", "") : "1");

        // keepCount: 从配置读取原始值，强制转整数并记录日志
        String rawKeepCount = sysConfigService.getConfigValue("backup.auto.keepCount");
        int keepCount;
        try {
            keepCount = Integer.parseInt(rawKeepCount);
        } catch (Exception e) {
            keepCount = 7; // 默认值
        }
        log.info("[自动备份配置] rawKeepCount={}, parsed={}", rawKeepCount, keepCount);
        m.put("keepCount", keepCount);
        return ResponseEntity.ok(m);
    }

    @PutMapping("/backup/auto-config")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> saveAutoBackupConfig(@RequestBody Map<String, Object> body) {
        try {
            log.info("[保存自动备份配置] 收到请求: {}", body);
            // 如果前端没传 keepCount（null），使用合理默认值而不覆盖数据库
            Object keepCountRaw = body.get("keepCount");
            if (keepCountRaw == null) {
                // 前端没改动保留数量，读取数据库现有值，不覆盖
                String existingKeepCount = sysConfigService.getConfigValue("backup.auto.keepCount");
                if (existingKeepCount != null && !existingKeepCount.isEmpty()) {
                    body.put("keepCount", existingKeepCount);
                }
            }

            if (body.get("enabled") != null) {
                sysConfigService.saveConfig("backup.auto.enabled", String.valueOf(body.get("enabled")),
                        "BOOLEAN", "启用自动备份", "BACKUP", "");
            }
            if (body.get("cycle") != null) {
                sysConfigService.saveConfig("backup.auto.cycle", "\"" + body.get("cycle") + "\"",
                        "STRING", "备份周期", "BACKUP", "");
            }
            if (body.get("time") != null) {
                sysConfigService.saveConfig("backup.auto.time", "\"" + body.get("time") + "\"",
                        "STRING", "每日备份时间", "BACKUP", "");
            }
            if (body.get("weekDay") != null) {
                sysConfigService.saveConfig("backup.auto.weekDay", "\"" + body.get("weekDay") + "\"",
                        "STRING", "每周备份星期", "BACKUP", "");
            }
            if (body.get("keepCount") != null) {
                // 强制限定范围：1~30
                int keepCountVal;
                try {
                    keepCountVal = Integer.parseInt(String.valueOf(body.get("keepCount")));
                } catch (NumberFormatException e) {
                    keepCountVal = 7;
                }
                if (keepCountVal < 1) keepCountVal = 1;
                if (keepCountVal > 30) keepCountVal = 30;
                sysConfigService.saveConfig("backup.auto.keepCount", String.valueOf(keepCountVal),
                        "INT", "保留备份数量", "BACKUP", "");
            }
            Map<String, Object> ok = new HashMap<>();
            ok.put("message", "保存成功");
            return ResponseEntity.ok(ok);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 手动触发自动备份逻辑（用于测试）
     * 调用此接口会立即执行一次自动备份流程（包括清理旧备份）
     */
    @PostMapping("/backup/test-auto")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> testAutoBackup() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 手动触发定时任务的逻辑
            if (!sysConfigService.getBoolean("backup.auto.enabled", false)) {
                result.put("message", "自动备份未启用，请先在配置中启用");
                result.put("success", false);
                return ResponseEntity.ok(result);
            }
            String path = databaseBackupService.backup();
            int keepCount = sysConfigService.getInt("backup.auto.keepCount", 7);
            // 调用清理逻辑
            List<File> files = databaseBackupService.listBackupFiles();
            if (files.size() > keepCount) {
                files.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (int i = 0; i < files.size() - keepCount; i++) {
                    files.get(i).delete();
                }
            }
            result.put("message", "测试备份成功");
            result.put("path", path);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", "测试备份失败: " + e.getMessage());
            result.put("success", false);
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 删除服务器上的备份文件
     */
    @DeleteMapping("/backup/delete")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('config')")
    public ResponseEntity<Map<String, Object>> deleteBackupFile(@RequestParam String path) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (path == null || path.isEmpty() || path.contains("..")) {
                res.put("message", "路径不合法");
                return ResponseEntity.badRequest().body(res);
            }
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                res.put("message", "文件不存在");
                return ResponseEntity.status(404).body(res);
            }
            if (!file.delete()) {
                res.put("message", "删除失败");
                return ResponseEntity.badRequest().body(res);
            }
            res.put("message", "删除成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}
