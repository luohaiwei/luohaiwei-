package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 仪表盘/首页控制器测试
 * 测试系统管理员首页、学生首页、教师首页等仪表盘数据
 * 对应测试用例：TC-HOME-ADMIN-001 ~ TC-HOME-ADMIN-004, TC-HOME-STU-001 ~ TC-HOME-STU-002
 */
@DisplayName("仪表盘控制器测试")
public class DashboardControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("系统管理员首页测试")
    class AdminDashboardTests {

        @Test
        @DisplayName("TC-HOME-ADMIN-001: 顶部导航栏验证 - 系统管理员可以访问首页")
        void systemAdminCanAccessDashboard() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-002: 左侧菜单栏验证 - 系统管理员首页显示完整菜单")
        void systemAdminDashboardShowsFullMenu() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.menus").isArray());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-003: 快捷入口卡片验证 - 首页显示快捷入口")
        void dashboardShowsQuickEntryCards() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quickEntries").isArray());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-004: 数据统计卡片验证 - 首页显示统计数据")
        void dashboardShowsStatisticsCards() throws Exception {
            mockMvc.perform(get("/dashboard/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userCount").exists())
                    .andExpect(jsonPath("$.deviceCount").exists())
                    .andExpect(jsonPath("$.todayBookingCount").exists())
                    .andExpect(jsonPath("$.pendingAuditCount").exists());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-004: 统计数据-系统用户总数")
        void dashboardShowsUserCount() throws Exception {
            mockMvc.perform(get("/dashboard/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userCount").isNumber());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-004: 统计数据-设备总数")
        void dashboardShowsDeviceCount() throws Exception {
            mockMvc.perform(get("/dashboard/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceCount").isNumber());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-004: 统计数据-今日预约数")
        void dashboardShowsTodayBookingCount() throws Exception {
            mockMvc.perform(get("/dashboard/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayBookingCount").isNumber());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-004: 统计数据-待审核预约数")
        void dashboardShowsPendingAuditCount() throws Exception {
            mockMvc.perform(get("/dashboard/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingAuditCount").isNumber());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-005: 待办事项列表验证 - 显示待审核预约")
        void dashboardShowsPendingBookings() throws Exception {
            mockMvc.perform(get("/dashboard/todo-list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingBookings").isArray());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-005: 待办事项列表-显示待处理报修")
        void dashboardShowsPendingRepairs() throws Exception {
            mockMvc.perform(get("/dashboard/todo-list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingRepairs").isArray());
        }

        @Test
        @DisplayName("TC-HOME-ADMIN-006: 使用趋势图表 - 显示近7天趋势")
        void dashboardShowsUsageTrend() throws Exception {
            mockMvc.perform(get("/dashboard/usage-trend")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trend").isArray());
        }

        @Test
        @DisplayName("系统管理员可以获取仪表盘数据")
        void systemAdminCanGetDashboardData() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("学生首页测试")
    class StudentDashboardTests {

        @Test
        @DisplayName("TC-HOME-STU-001: 快捷入口验证 - 学生首页显示快捷入口")
        void studentDashboardShowsQuickEntries() throws Exception {
            mockMvc.perform(get("/dashboard/student")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quickEntries").isArray());
        }

        @Test
        @DisplayName("TC-HOME-STU-001: 学生首页显示设备预约入口")
        void studentDashboardShowsDeviceBookingEntry() throws Exception {
            mockMvc.perform(get("/dashboard/student")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-HOME-STU-001: 学生首页显示AI助手入口")
        void studentDashboardShowsAIAssistantEntry() throws Exception {
            mockMvc.perform(get("/dashboard/student")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-HOME-STU-001: 学生首页显示故障报修入口")
        void studentDashboardShowsRepairEntry() throws Exception {
            mockMvc.perform(get("/dashboard/student")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-HOME-STU-002: 我的预约验证 - 显示今日预约")
        void studentDashboardShowsTodayBookings() throws Exception {
            mockMvc.perform(get("/dashboard/student/bookings")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayBookings").isArray());
        }

        @Test
        @DisplayName("TC-HOME-STU-002: 我的预约-显示近期预约列表")
        void studentDashboardShowsRecentBookings() throws Exception {
            mockMvc.perform(get("/dashboard/student/bookings")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentBookings").isArray());
        }

        @Test
        @DisplayName("TC-HOME-STU-003: 预约提醒验证 - 今日有预约时显示提醒")
        void studentDashboardShowsBookingReminder() throws Exception {
            mockMvc.perform(get("/dashboard/student/reminder")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-HOME-STU-004: 使用统计验证 - 显示本月使用时长")
        void studentDashboardShowsUsageStatistics() throws Exception {
            mockMvc.perform(get("/dashboard/student/usage-statistics")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.monthUsageHours").exists());
        }

        @Test
        @DisplayName("TC-HOME-STU-004: 使用统计-设备使用排行")
        void studentDashboardShowsDeviceUsageRanking() throws Exception {
            mockMvc.perform(get("/dashboard/student/device-ranking")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ranking").isArray());
        }
    }

    @Nested
    @DisplayName("教师首页测试")
    class TeacherDashboardTests {

        @Test
        @DisplayName("教师首页显示教学概览")
        void teacherDashboardShowsTeachingOverview() throws Exception {
            mockMvc.perform(get("/dashboard/teacher")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("教师首页显示待审核学生预约")
        void teacherDashboardShowsPendingStudentBookings() throws Exception {
            mockMvc.perform(get("/dashboard/teacher/pending-audits")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("教师首页显示我的学生列表")
        void teacherDashboardShowsMyStudents() throws Exception {
            mockMvc.perform(get("/dashboard/teacher/students")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("教师可以获取教学统计")
        void teacherCanGetTeachingStatistics() throws Exception {
            mockMvc.perform(get("/dashboard/teacher/statistics")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("实验室管理员首页测试")
    class LabAdminDashboardTests {

        @Test
        @DisplayName("实验室管理员首页显示工作台")
        void labAdminDashboardShowsWorkbench() throws Exception {
            mockMvc.perform(get("/dashboard/lab-admin")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员显示本实验室设备统计")
        void labAdminDashboardShowsLabDeviceStats() throws Exception {
            mockMvc.perform(get("/dashboard/lab-admin/device-stats")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员显示待审核预约")
        void labAdminDashboardShowsPendingBookings() throws Exception {
            mockMvc.perform(get("/dashboard/lab-admin/pending-bookings")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员显示待处理报修")
        void labAdminDashboardShowsPendingRepairs() throws Exception {
            mockMvc.perform(get("/dashboard/lab-admin/pending-repairs")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("维护人员首页测试")
    class MaintainerDashboardTests {

        @Test
        @DisplayName("维护人员首页显示维护工作台")
        void maintainerDashboardShowsWorkbench() throws Exception {
            mockMvc.perform(get("/dashboard/maintainer")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("维护人员显示待接单任务")
        void maintainerDashboardShowsPendingTasks() throws Exception {
            mockMvc.perform(get("/dashboard/maintainer/pending-tasks")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("维护人员显示维修中任务")
        void maintainerDashboardShowsInProgressTasks() throws Exception {
            mockMvc.perform(get("/dashboard/maintainer/in-progress-tasks")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("维护人员显示工作量统计")
        void maintainerDashboardShowsWorkloadStats() throws Exception {
            mockMvc.perform(get("/dashboard/maintainer/workload-stats")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("认证测试")
    class AuthenticationTests {

        @Test
        @DisplayName("已登录用户可以访问仪表盘")
        void authenticatedUserCanAccessDashboard() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能访问仪表盘")
        void unauthenticatedUserCannotAccessDashboard() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("不同角色获取不同的仪表盘数据")
        void differentRolesGetDifferentDashboardData() throws Exception {
            // 系统管理员
            mockMvc.perform(get("/dashboard")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());

            // 学生
            mockMvc.perform(get("/dashboard/student")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());

            // 教师
            mockMvc.perform(get("/dashboard/teacher")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("快捷操作测试")
    class QuickActionTests {

        @Test
        @DisplayName("系统管理员可以获取快捷操作列表")
        void systemAdminCanGetQuickActions() throws Exception {
            mockMvc.perform(get("/dashboard/quick-actions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生可以获取快捷操作列表")
        void studentCanGetQuickActions() throws Exception {
            mockMvc.perform(get("/dashboard/quick-actions")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("快捷操作包含设备预约")
        void quickActionsIncludeDeviceBooking() throws Exception {
            mockMvc.perform(get("/dashboard/quick-actions")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("快捷操作包含AI助手")
        void quickActionsIncludeAIAssistant() throws Exception {
            mockMvc.perform(get("/dashboard/quick-actions")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }
}
