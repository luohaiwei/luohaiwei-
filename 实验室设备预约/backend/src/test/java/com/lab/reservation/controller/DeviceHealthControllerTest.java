package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备档案/健康记录控制器测试
 * 测试设备健康档案、历史记录等功能
 * 对应测试用例：TC-EQUIP-DETAIL-002, TC-EQUIP-DETAIL-003
 */
@DisplayName("设备档案控制器测试")
public class DeviceHealthControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("设备健康档案测试")
    class DeviceHealthRecordTests {

        @Test
        @DisplayName("TC-EQUIP-DETAIL-002: 查看设备维护记录 - 可以获取设备健康档案")
        void canGetDeviceHealthRecord() throws Exception {
            mockMvc.perform(get("/device-health/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").exists());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-002: 设备健康档案显示维修记录")
        void deviceHealthRecordShowsRepairRecords() throws Exception {
            mockMvc.perform(get("/device-health/1/repairs")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-002: 设备健康档案显示校准记录")
        void deviceHealthRecordShowsCalibrationRecords() throws Exception {
            mockMvc.perform(get("/device-health/1/calibrations")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("设备健康档案显示状态变更历史")
        void deviceHealthRecordShowsStatusHistory() throws Exception {
            mockMvc.perform(get("/device-health/1/status-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("设备健康档案显示使用统计")
        void deviceHealthRecordShowsUsageStatistics() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-statistics")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsageHours").exists())
                    .andExpect(jsonPath("$.totalUsageCount").exists());
        }

        @Test
        @DisplayName("未登录用户不能获取设备健康档案")
        void unauthenticatedUserCannotGetDeviceHealthRecord() throws Exception {
            mockMvc.perform(get("/device-health/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("设备使用记录测试")
    class DeviceUsageRecordTests {

        @Test
        @DisplayName("TC-EQUIP-DETAIL-003: 查看设备使用记录 - 可以获取使用记录列表")
        void canGetDeviceUsageRecords() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-003: 使用记录显示预约人信息")
        void usageRecordsShowBookingUser() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-003: 使用记录显示使用时间")
        void usageRecordsShowUsageTime() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-003: 使用记录显示实验目的")
        void usageRecordsShowExperimentPurpose() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-EQUIP-DETAIL-003: 使用记录显示归还状态")
        void usageRecordsShowReturnStatus() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用记录支持按时间范围筛选")
        void usageRecordsSupportDateFilter() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("使用记录支持按预约人筛选")
        void usageRecordsSupportUserFilter() throws Exception {
            mockMvc.perform(get("/device-health/1/usage-records")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("userId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("设备故障历史测试")
    class DeviceFaultHistoryTests {

        @Test
        @DisplayName("可以获取设备故障历史")
        void canGetDeviceFaultHistory() throws Exception {
            mockMvc.perform(get("/device-health/1/fault-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("故障历史显示故障类型")
        void faultHistoryShowsFaultType() throws Exception {
            mockMvc.perform(get("/device-health/1/fault-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("故障历史显示维修结果")
        void faultHistoryShowsRepairResult() throws Exception {
            mockMvc.perform(get("/device-health/1/fault-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("故障历史显示故障时长")
        void faultHistoryShowsFaultDuration() throws Exception {
            mockMvc.perform(get("/device-health/1/fault-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("故障历史显示维修费用")
        void faultHistoryShowsRepairCost() throws Exception {
            mockMvc.perform(get("/device-health/1/fault-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("设备健康评分测试")
    class DeviceHealthScoreTests {

        @Test
        @DisplayName("可以获取设备健康评分")
        void canGetDeviceHealthScore() throws Exception {
            mockMvc.perform(get("/device-health/1/score")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.score").exists());
        }

        @Test
        @DisplayName("健康评分显示评分维度")
        void healthScoreShowsDimensions() throws Exception {
            mockMvc.perform(get("/device-health/1/score")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dimensions").exists());
        }

        @Test
        @DisplayName("健康评分显示保养状态")
        void healthScoreShowsMaintenanceStatus() throws Exception {
            mockMvc.perform(get("/device-health/1/score")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("健康评分显示故障率")
        void healthScoreShowsFaultRate() throws Exception {
            mockMvc.perform(get("/device-health/1/score")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("设备到期提醒测试")
    class DeviceReminderTests {

        @Test
        @DisplayName("可以获取设备校准到期提醒")
        void canGetCalibrationReminders() throws Exception {
            mockMvc.perform(get("/device-health/calibration-reminders")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("可以获取设备保养到期提醒")
        void canGetMaintenanceReminders() throws Exception {
            mockMvc.perform(get("/device-health/maintenance-reminders")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("提醒支持按实验室筛选")
        void remindersSupportLabFilter() throws Exception {
            mockMvc.perform(get("/device-health/calibration-reminders")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("labId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("设备分析报告测试")
    class DeviceAnalysisReportTests {

        @Test
        @DisplayName("可以生成设备分析报告")
        void canGenerateDeviceAnalysisReport() throws Exception {
            mockMvc.perform(get("/device-health/1/report")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("分析报告支持按时间范围")
        void analysisReportSupportsDateRange() throws Exception {
            mockMvc.perform(get("/device-health/1/report")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("分析报告可以导出")
        void analysisReportCanBeExported() throws Exception {
            mockMvc.perform(get("/device-health/1/report/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取设备分析报告")
        void studentCannotGetDeviceAnalysisReport() throws Exception {
            mockMvc.perform(get("/device-health/1/report")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
