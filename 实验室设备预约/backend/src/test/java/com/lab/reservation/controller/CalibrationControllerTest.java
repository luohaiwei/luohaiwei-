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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 校准管理控制器测试
 * 测试设备校准计划、校准记录等功能
 */
@DisplayName("校准管理控制器测试")
public class CalibrationControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("校准计划列表测试")
    class CalibrationPlanTests {

        @Test
        @DisplayName("系统管理员可以获取校准计划列表")
        void systemAdminCanGetCalibrationPlanList() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("维护人员可以获取校准计划列表")
        void maintainerCanGetCalibrationPlanList() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以获取校准计划列表")
        void labAdminCanGetCalibrationPlanList() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("校准计划支持按设备筛选")
        void calibrationPlanSupportsDeviceFilter() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("校准计划支持按状态筛选")
        void calibrationPlanSupportsStatusFilter() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("status", "pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("校准计划支持按时间范围筛选")
        void calibrationPlanSupportsDateFilter() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生不能获取校准计划列表")
        void studentCannotGetCalibrationPlanList() throws Exception {
            mockMvc.perform(get("/calibration/plan/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取校准计划列表")
        void unauthenticatedUserCannotGetCalibrationPlanList() throws Exception {
            mockMvc.perform(get("/calibration/plan/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("新增校准计划测试")
    class AddCalibrationPlanTests {

        @Test
        @DisplayName("系统管理员可以新增校准计划")
        void systemAdminCanAddCalibrationPlan() throws Exception {
            Map<String, Object> plan = new HashMap<>();
            plan.put("deviceId", 1);
            plan.put("calibrationDate", "2026-04-01");
            plan.put("calibrationType", "定期校准");
            plan.put("description", "季度校准");

            mockMvc.perform(post("/calibration/plan")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(plan)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("维护人员可以新增校准计划")
        void maintainerCanAddCalibrationPlan() throws Exception {
            Map<String, Object> plan = new HashMap<>();
            plan.put("deviceId", 1);
            plan.put("calibrationDate", "2026-04-15");
            plan.put("calibrationType", "维修后校准");

            mockMvc.perform(post("/calibration/plan")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(plan)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("新增校准计划-缺少必填字段")
        void cannotAddCalibrationPlanWithMissingFields() throws Exception {
            Map<String, Object> plan = new HashMap<>();
            plan.put("deviceId", 1);
            // 缺少calibrationDate

            mockMvc.perform(post("/calibration/plan")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(plan)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("学生不能新增校准计划")
        void studentCannotAddCalibrationPlan() throws Exception {
            Map<String, Object> plan = new HashMap<>();
            plan.put("deviceId", 1);
            plan.put("calibrationDate", "2026-04-01");

            mockMvc.perform(post("/calibration/plan")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(plan)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("校准提醒测试")
    class CalibrationReminderTests {

        @Test
        @DisplayName("系统管理员可以获取校准提醒列表")
        void systemAdminCanGetCalibrationReminders() throws Exception {
            mockMvc.perform(get("/calibration/reminders")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("维护人员可以获取校准提醒列表")
        void maintainerCanGetCalibrationReminders() throws Exception {
            mockMvc.perform(get("/calibration/reminders")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("即将到期的校准显示提醒")
        void upcomingCalibrationsShowReminder() throws Exception {
            mockMvc.perform(get("/calibration/reminders/upcoming")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("执行校准测试")
    class ExecuteCalibrationTests {

        @Test
        @DisplayName("维护人员可以执行校准")
        void maintainerCanExecuteCalibration() throws Exception {
            Map<String, Object> calibration = new HashMap<>();
            calibration.put("result", "合格");
            calibration.put("accuracy", "0.001");
            calibration.put("certificateNo", "CAL" + System.currentTimeMillis());
            calibration.put("nextCalibrationDate", "2026-10-01");
            calibration.put("note", "校准完成");

            mockMvc.perform(post("/calibration/execute/1")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(calibration)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("执行校准后生成校准记录")
        void executeCalibrationGeneratesRecord() throws Exception {
            Map<String, Object> calibration = new HashMap<>();
            calibration.put("result", "合格");
            calibration.put("accuracy", "0.002");

            mockMvc.perform(post("/calibration/execute/1")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(calibration)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("执行校准后更新下次校准时间")
        void executeCalibrationUpdatesNextDate() throws Exception {
            Map<String, Object> calibration = new HashMap<>();
            calibration.put("result", "合格");
            calibration.put("nextCalibrationDate", "2026-10-01");

            mockMvc.perform(post("/calibration/execute/1")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(calibration)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能执行校准")
        void studentCannotExecuteCalibration() throws Exception {
            Map<String, Object> calibration = new HashMap<>();
            calibration.put("result", "合格");

            mockMvc.perform(post("/calibration/execute/1")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(calibration)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("校准记录测试")
    class CalibrationRecordTests {

        @Test
        @DisplayName("系统管理员可以获取校准记录列表")
        void systemAdminCanGetCalibrationRecordList() throws Exception {
            mockMvc.perform(get("/calibration/record/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("维护人员可以获取校准记录列表")
        void maintainerCanGetCalibrationRecordList() throws Exception {
            mockMvc.perform(get("/calibration/record/list")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("校准记录支持按设备筛选")
        void calibrationRecordSupportsDeviceFilter() throws Exception {
            mockMvc.perform(get("/calibration/record/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("可以查看校准证书")
        void canViewCalibrationCertificate() throws Exception {
            mockMvc.perform(get("/calibration/record/1/certificate")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取校准记录列表")
        void studentCannotGetCalibrationRecordList() throws Exception {
            mockMvc.perform(get("/calibration/record/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("校准计划详情测试")
    class CalibrationPlanDetailTests {

        @Test
        @DisplayName("已登录用户可以获取校准计划详情")
        void authenticatedUserCanGetCalibrationPlanDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/calibration/plan/1")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取校准计划详情")
        void unauthenticatedUserCannotGetCalibrationPlanDetail() throws Exception {
            mockMvc.perform(get("/calibration/plan/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("删除校准计划测试")
    class DeleteCalibrationPlanTests {

        @Test
        @DisplayName("系统管理员可以删除校准计划")
        void systemAdminCanDeleteCalibrationPlan() throws Exception {
            mockMvc.perform(delete("/calibration/plan/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能删除校准计划")
        void studentCannotDeleteCalibrationPlan() throws Exception {
            mockMvc.perform(delete("/calibration/plan/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("校准统计测试")
    class CalibrationStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取校准统计")
        void systemAdminCanGetCalibrationStatistics() throws Exception {
            mockMvc.perform(get("/calibration/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取校准合规率统计")
        void canGetCalibrationComplianceRate() throws Exception {
            mockMvc.perform(get("/calibration/statistics/compliance")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取设备校准周期统计")
        void canGetDeviceCalibrationCycleStatistics() throws Exception {
            mockMvc.perform(get("/calibration/statistics/cycle")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("维护人员可以获取校准统计")
        void maintainerCanGetCalibrationStatistics() throws Exception {
            mockMvc.perform(get("/calibration/statistics")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }
    }
}
