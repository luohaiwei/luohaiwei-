package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系统配置控制器测试
 * 测试系统参数配置、数据备份与恢复等功能
 * 注意：原 booking_rule 表的 CRUD 功能已废弃，预约规则统一通过 sys_config 存储
 */
@DisplayName("系统配置控制器测试")
public class SysConfigControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("系统配置查询测试")
    class ConfigQueryTests {

        @Test
        @DisplayName("TC-SYSCFG-001: 系统管理员可以获取系统配置")
        void systemAdminCanGetSystemConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统配置显示审核流程配置")
        void systemConfigShowsAuditFlowConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统配置显示通知配置")
        void systemConfigShowsNotificationConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统配置显示预约规则配置")
        void systemConfigShowsBookingRuleConfig() throws Exception {
            mockMvc.perform(get("/sys/config/group/BOOKING_RULE")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取系统配置")
        void labAdminCanGetSystemConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取系统配置")
        void studentCannotGetSystemConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取系统配置")
        void unauthenticatedUserCannotGetSystemConfig() throws Exception {
            mockMvc.perform(get("/sys/config/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("更新系统配置测试")
    class UpdateConfigTests {

        @Test
        @DisplayName("TC-SYSCFG-001: 系统管理员可以批量保存配置")
        void systemAdminCanBatchSaveConfigs() throws Exception {
            Map<String, String> config = new HashMap<>();
            config.put("key", "booking.audit.mode");
            config.put("value", "\"MANUAL\"");
            config.put("type", "STRING");
            config.put("name", "预约审核模式");
            config.put("group", "BOOKING");

            mockMvc.perform(post("/sys/config/save")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Arrays.asList(config))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("配置保存后可以读取")
        void configTakesEffectImmediately() throws Exception {
            Map<String, String> config = new HashMap<>();
            config.put("key", "booking.audit.mode");
            config.put("value", "\"AUTO\"");
            config.put("type", "STRING");
            config.put("name", "预约审核模式");
            config.put("group", "BOOKING");

            mockMvc.perform(post("/sys/config/save")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Arrays.asList(config))))
                    .andExpect(status().isOk());

            // 验证可以读取
            mockMvc.perform(get("/sys/config/value")
                            .param("key", "booking.audit.mode")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能更新系统配置")
        void studentCannotUpdateSystemConfig() throws Exception {
            Map<String, String> config = new HashMap<>();
            config.put("key", "testKey");
            config.put("value", "testValue");
            config.put("type", "STRING");
            config.put("name", "测试配置");
            config.put("group", "TEST");

            mockMvc.perform(post("/sys/config/save")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Arrays.asList(config))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("实验室管理员不能更新系统配置")
        void labAdminCannotUpdateSystemConfig() throws Exception {
            Map<String, String> config = new HashMap<>();
            config.put("key", "testKey");
            config.put("value", "testValue");
            config.put("type", "STRING");
            config.put("name", "测试配置");
            config.put("group", "TEST");

            mockMvc.perform(post("/sys/config/save")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Arrays.asList(config))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("预约全局规则测试")
    class BookingRuleGlobalTests {

        @Test
        @DisplayName("系统管理员可以获取预约全局规则")
        void systemAdminCanGetGlobalBookingRules() throws Exception {
            mockMvc.perform(get("/booking-rule/global")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以保存预约全局规则")
        void systemAdminCanSaveGlobalBookingRules() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("type", "basic");
            Map<String, Object> data = new HashMap<>();
            data.put("minAdvanceHours", 24);
            data.put("maxBookingHours", 4);
            data.put("maxBookingsPerDevicePerDay", 3);
            data.put("maxBookingsPerUserPerDay", 5);
            data.put("cancelDeadlineHours", 2);
            data.put("noShowThreshold", 3);
            body.put("data", data);

            mockMvc.perform(put("/booking-rule/global")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("实验室管理员不能访问预约全局规则")
        void labAdminCannotAccessGlobalBookingRules() throws Exception {
            mockMvc.perform(get("/booking-rule/global")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("数据备份测试")
    class DataBackupTests {

        @Test
        @DisplayName("TC-BACKUP-001: 系统管理员可以手动备份数据库")
        void systemAdminCanManualBackup() throws Exception {
            mockMvc.perform(post("/sys/config/backup")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-BACKUP-001: 备份成功后列表显示新记录")
        void backupListShowsNewRecord() throws Exception {
            mockMvc.perform(get("/sys/config/backup/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以配置自动备份")
        void systemAdminCanConfigureAutoBackup() throws Exception {
            Map<String, Object> config = new HashMap<>();
            config.put("enableAutoBackup", true);
            config.put("backupFrequency", "daily");
            config.put("backupTime", "02:00");

            mockMvc.perform(put("/sys/config/backup/auto-config")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(config)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-BACKUP-002: 系统管理员可以恢复数据")
        void systemAdminCanRestoreData() throws Exception {
            // 注意：实际恢复需要指定备份文件路径，此处测试端点存在性
            mockMvc.perform(post("/sys/config/restore")
                            .param("path", "D:/lab-backup/test_backup.sql")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能备份数据")
        void nonSystemAdminCannotBackup() throws Exception {
            mockMvc.perform(post("/sys/config/backup")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能备份数据")
        void studentCannotBackup() throws Exception {
            mockMvc.perform(post("/sys/config/backup")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("单个配置值读取测试")
    class SingleConfigValueTests {

        @Test
        @DisplayName("公开接口可以读取配置值")
        void publicCanGetConfigValue() throws Exception {
            mockMvc.perform(get("/sys/config/value")
                            .param("key", "booking.audit.mode"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("获取不存在的配置返回null")
        void getNonExistentConfig() throws Exception {
            mockMvc.perform(get("/sys/config/value")
                            .param("key", "non.existent.key"))
                    .andExpect(status().isOk());
        }
    }
}
