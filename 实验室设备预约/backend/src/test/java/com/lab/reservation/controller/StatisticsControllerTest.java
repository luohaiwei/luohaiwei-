package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统计分析控制器测试
 * 测试设备统计、预约统计等功能
 * 对应测试用例：TC-STAT-001 ~ TC-STAT-004, TC-STAT-011 ~ TC-STAT-012
 */
@DisplayName("统计分析控制器测试")
public class StatisticsControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("设备统计测试")
    class DeviceStatisticsTests {

        @Test
        @DisplayName("TC-STAT-001: 设备统计页面验证 - 系统管理员可以获取设备统计")
        void systemAdminCanGetDeviceStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-001: 设备统计页面显示统计数据")
        void deviceStatisticsShowsData() throws Exception {
            mockMvc.perform(get("/statistics/device")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-002: 设备使用率柱状图 - 可以获取设备使用率数据")
        void canGetDeviceUsageRateData() throws Exception {
            mockMvc.perform(get("/statistics/device/usage-rate")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-STAT-003: 使用趋势折线图 - 可以获取使用趋势数据")
        void canGetUsageTrendData() throws Exception {
            mockMvc.perform(get("/statistics/device/trend")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("设备使用率支持按实验室筛选")
        void deviceUsageRateSupportsLabFilter() throws Exception {
            mockMvc.perform(get("/statistics/device/usage-rate")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("labId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("设备使用率支持按设备类型筛选")
        void deviceUsageRateSupportsCategoryFilter() throws Exception {
            mockMvc.perform(get("/statistics/device/usage-rate")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("categoryId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取本实验室设备统计")
        void labAdminCanGetLabDeviceStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取设备统计")
        void studentCannotGetDeviceStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取设备统计")
        void unauthenticatedUserCannotGetDeviceStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("预约统计测试")
    class BookingStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取预约统计")
        void systemAdminCanGetBookingStatistics() throws Exception {
            mockMvc.perform(get("/statistics/booking")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-011: 预约需求分布统计 - 可以获取预约需求分布")
        void canGetBookingDemandDistribution() throws Exception {
            mockMvc.perform(get("/statistics/booking/demand")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("dimension", "department"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-STAT-011: 预约需求分布-按专业维度")
        void canGetBookingDemandByMajor() throws Exception {
            mockMvc.perform(get("/statistics/booking/demand")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("dimension", "major"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-012: 预约高峰时段分析 - 可以获取高峰时段数据")
        void canGetPeakHoursData() throws Exception {
            mockMvc.perform(get("/statistics/booking/peak-hours")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-STAT-012: 高峰时段分析-支持按周查看")
        void canGetPeakHoursByWeek() throws Exception {
            mockMvc.perform(get("/statistics/booking/peak-hours")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("period", "week"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("预约统计支持按时间范围筛选")
        void bookingStatisticsSupportsDateFilter() throws Exception {
            mockMvc.perform(get("/statistics/booking")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取本室预约统计")
        void labAdminCanGetLabBookingStatistics() throws Exception {
            mockMvc.perform(get("/statistics/booking")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("教师可以获取预约统计")
        void teacherCanGetBookingStatistics() throws Exception {
            mockMvc.perform(get("/statistics/booking")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取预约统计")
        void studentCannotGetBookingStatistics() throws Exception {
            mockMvc.perform(get("/statistics/booking")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("设备故障统计测试")
    class DeviceFaultStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取设备故障统计")
        void systemAdminCanGetDeviceFaultStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device/fault")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取故障类型分布")
        void canGetFaultTypeDistribution() throws Exception {
            mockMvc.perform(get("/statistics/device/fault/type")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("可以获取设备故障排行")
        void canGetDeviceFaultRanking() throws Exception {
            mockMvc.perform(get("/statistics/device/fault/ranking")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("维护人员可以获取故障统计")
        void maintainerCanGetDeviceFaultStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device/fault")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取故障统计")
        void studentCannotGetDeviceFaultStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device/fault")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("用户使用统计测试")
    class UserUsageStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取用户使用统计")
        void systemAdminCanGetUserUsageStatistics() throws Exception {
            mockMvc.perform(get("/statistics/user/usage")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取用户使用时长统计")
        void canGetUserUsageDurationStatistics() throws Exception {
            mockMvc.perform(get("/statistics/user/usage/duration")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取用户预约次数统计")
        void canGetUserBookingCountStatistics() throws Exception {
            mockMvc.perform(get("/statistics/user/usage/count")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("教师可以获取学生使用统计")
        void teacherCanGetStudentUsageStatistics() throws Exception {
            mockMvc.perform(get("/statistics/user/usage")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生可以获取自己的使用统计")
        void studentCanGetOwnUsageStatistics() throws Exception {
            mockMvc.perform(get("/statistics/user/usage/self")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("数据导出测试")
    class DataExportTests {

        @Test
        @DisplayName("TC-STAT-004: 导出统计数据 - 系统管理员可以导出设备统计")
        void systemAdminCanExportDeviceStatistics() throws Exception {
            mockMvc.perform(get("/statistics/device/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("format", "excel"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-004: 导出统计数据 - 支持导出为Excel")
        void canExportStatisticsAsExcel() throws Exception {
            mockMvc.perform(get("/statistics/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("type", "device")
                            .param("format", "excel"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-STAT-004: 导出统计数据 - 支持导出为PDF")
        void canExportStatisticsAsPdf() throws Exception {
            mockMvc.perform(get("/statistics/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("type", "booking")
                            .param("format", "pdf"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("导出统计支持按时间范围筛选")
        void exportStatisticsSupportsDateFilter() throws Exception {
            mockMvc.perform(get("/statistics/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能导出统计")
        void nonSystemAdminCannotExportStatistics() throws Exception {
            mockMvc.perform(get("/statistics/export")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("仪表盘数据测试")
    class DashboardTests {

        @Test
        @DisplayName("系统管理员可以获取仪表盘数据")
        void systemAdminCanGetDashboardData() throws Exception {
            mockMvc.perform(get("/statistics/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取仪表盘数据")
        void labAdminCanGetDashboardData() throws Exception {
            mockMvc.perform(get("/statistics/dashboard")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("教师可以获取仪表盘数据")
        void teacherCanGetDashboardData() throws Exception {
            mockMvc.perform(get("/statistics/dashboard")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生可以获取仪表盘数据")
        void studentCanGetDashboardData() throws Exception {
            mockMvc.perform(get("/statistics/dashboard")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能获取仪表盘数据")
        void unauthenticatedUserCannotGetDashboardData() throws Exception {
            mockMvc.perform(get("/statistics/dashboard"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
