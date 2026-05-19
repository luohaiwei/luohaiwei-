package com.lab.reservation.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.*;
import com.lab.reservation.mapper.*;
import com.lab.reservation.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 系统定时任务
 * 功能计划：设备校准流程（自动提醒）、数据备份与恢复（自动备份）
 */
@Component
public class SystemScheduledTasks {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired(required = false)
    private SysConfigService sysConfigService;

    @Autowired(required = false)
    private DatabaseBackupService databaseBackupService;

    @Autowired(required = false)
    private SysMessageService sysMessageService;

    @Autowired(required = false)
    private SysUserService sysUserService;

    @Autowired
    private com.lab.reservation.mapper.SysLogMapper sysLogMapper;

    /**
     * 校准到期提醒
     * 每天凌晨 1 点检查所有设备，发送校准到期提醒
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkCalibrationReminders() {
        if (sysConfigService == null || !sysConfigService.getBoolean("notification.scene.calibrationReminder", true)) {
            return;
        }
        int reminderDays = sysConfigService.getInt("system.calibration.reminderDays", 7);
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(reminderDays);

        // 查询所有设备，找出校准日期在 [today, deadline] 区间的
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        wrapper.le("next_calibration_date", deadline.atStartOfDay())
              .ge("next_calibration_date", today.atStartOfDay())
              .in("status", 0, 1, 2, 3)  // 空闲/使用中/维修中/校准中状态均需提醒
              .eq("deleted", 0);
        List<DeviceInfo> devices = deviceInfoMapper.selectList(wrapper);

        System.out.println("[校准提醒] 发现 " + devices.size() + " 台设备即将到期校准");

        // 获取所有实验室管理员用户
        if (sysMessageService != null && sysUserService != null) {
            List<SysUser> admins = sysUserService.list(new QueryWrapper<SysUser>()
                    .eq("user_type", "LAB_ADMIN")
                    .eq("status", 1)
                    .eq("deleted", 0));

            for (DeviceInfo device : devices) {
                if (device.getNextCalibrationDate() == null) continue;
                LocalDate calDate = device.getNextCalibrationDate().toLocalDate();
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, calDate);

                String title = "设备校准到期提醒";
                String content = String.format("设备「%s」（编号：%s）将于 %d 天后（%s）到期校准，请及时安排校准工作。",
                        device.getDeviceName(),
                        device.getDeviceNo(),
                        daysUntil,
                        calDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                // 向所有实验室管理员发送通知
                for (SysUser admin : admins) {
                    sysMessageService.sendMessage(
                            admin.getId(),
                            "CALIBRATION_REMIND",
                            title,
                            content,
                            device.getId(),
                            "device_info"
                    );
                }
                System.out.println("[校准提醒] 设备: " + device.getDeviceName() + ", 剩余 " + daysUntil + " 天, 已通知 " + admins.size() + " 位管理员");
            }
        } else {
            for (DeviceInfo device : devices) {
                if (device.getNextCalibrationDate() == null) continue;
                LocalDate calDate = device.getNextCalibrationDate().toLocalDate();
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, calDate);
                System.out.println("[校准提醒] 设备: " + device.getDeviceName() + ", 剩余 " + daysUntil + " 天");
            }
        }
    }

    /**
     * 自动数据库备份
     * 每分钟检查一次，仅在命中配置时间时执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoDatabaseBackup() {
        if (sysConfigService == null || databaseBackupService == null) {
            return;
        }
        if (!sysConfigService.getBoolean("backup.auto.enabled", false)) {
            return;
        }
        if (!shouldRunNow()) {
            return;
        }
        try {
            String path = databaseBackupService.backup();
            markLastRun();
            // 清理旧备份（保留最近 N 个）
            int keepCount = sysConfigService.getInt("backup.auto.keepCount", 7);
            cleanupOldBackups(keepCount);
            System.out.println("[自动备份] 执行成功: " + path);
        } catch (Exception e) {
            // 备份失败不影响主流程，仅记录
            System.err.println("[自动备份] 备份失败: " + e.getMessage());
        }
    }

    private boolean shouldRunNow() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        String nowMinute = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // 防止同一分钟内重复执行（例如应用重启）
        String lastRunMinute = safeConfig("backup.auto.lastRunMinute", "");
        if (nowMinute.equals(lastRunMinute)) {
            return false;
        }

        String timeText = safeConfig("backup.auto.time", "02:00");
        LocalTime targetTime = parseTime(timeText);
        if (targetTime == null) {
            return false;
        }
        if (now.getHour() != targetTime.getHour() || now.getMinute() != targetTime.getMinute()) {
            return false;
        }

        String cycle = safeConfig("backup.auto.cycle", "DAILY").toUpperCase();
        if ("WEEKLY".equals(cycle)) {
            String weekDayStr = safeConfig("backup.auto.weekDay", "1");
            int targetWeekDay = parseIntInRange(weekDayStr, 1, 7, 1);
            return now.getDayOfWeek().getValue() == targetWeekDay;
        }
        if ("MONTHLY".equals(cycle)) {
            // 当前页面没有“每月几号”配置，按每月 1 号执行
            return now.getDayOfMonth() == 1;
        }
        // DAILY 或未知值，按每天执行
        return true;
    }

    private void markLastRun() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        String nowMinute = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        sysConfigService.saveConfig(
                "backup.auto.lastRunMinute",
                nowMinute,
                "STRING",
                "自动备份最近执行分钟",
                "BACKUP",
                "用于避免同一分钟重复执行"
        );
    }

    private String safeConfig(String key, String defaultValue) {
        String value = sysConfigService.getConfigValue(key);
        if (value == null) {
            return defaultValue;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private LocalTime parseTime(String timeText) {
        try {
            return LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            System.err.println("[自动备份] 时间格式错误 backup.auto.time=" + timeText + "，应为 HH:mm");
            return null;
        }
    }

    private int parseIntInRange(String value, int min, int max, int defaultValue) {
        try {
            int n = Integer.parseInt(value);
            if (n < min || n > max) {
                return defaultValue;
            }
            return n;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 清理过旧的备份文件
     */
    private void cleanupOldBackups(int keepCount) {
        try {
            List<java.io.File> files = databaseBackupService.listBackupFiles();
            if (files.size() > keepCount) {
                // 按最后修改时间排序（最旧的在前）
                files.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                // 删除最旧的，保留最近的 keepCount 个
                for (int i = 0; i < files.size() - keepCount; i++) {
                    files.get(i).delete();
                }
            }
        } catch (Exception e) {
            System.err.println("[自动备份] 清理旧备份失败: " + e.getMessage());
        }
    }

    /**
     * 日志清理定时任务
     * 每天凌晨2点执行，保留最近的100条日志
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldLogs() {
        try {
            // 获取配置的保留条数，默认为100
            int keepCount = 100;
            if (sysConfigService != null) {
                keepCount = sysConfigService.getInt("log.cleanup.keepCount", 100);
            }
            
            // 查询总日志数
            long totalCount = sysLogMapper.selectCount(null);
            
            if (totalCount <= keepCount) {
                System.out.println("[日志清理] 日志总数 " + totalCount + " 条，未超过保留条数 " + keepCount + "，无需清理");
                return;
            }
            
            // 查询需要保留的最新日志的最小ID
            QueryWrapper<SysLog> wrapper = new QueryWrapper<>();
            wrapper.orderByDesc("id");
            wrapper.last("LIMIT " + keepCount);
            
            List<SysLog> logsToKeep = sysLogMapper.selectList(wrapper);
            
            if (logsToKeep.isEmpty()) {
                System.out.println("[日志清理] 没有需要保留的日志");
                return;
            }
            
            Long minIdToKeep = logsToKeep.get(logsToKeep.size() - 1).getId();
            
            QueryWrapper<SysLog> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.lt("id", minIdToKeep);
            
            int deletedCount = sysLogMapper.delete(deleteWrapper);
            
            System.out.println("[日志清理] 清理完成：总日志 " + totalCount + " 条，保留 " + keepCount + " 条，删除 " + deletedCount + " 条");
            
        } catch (Exception e) {
            System.err.println("[日志清理] 清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
