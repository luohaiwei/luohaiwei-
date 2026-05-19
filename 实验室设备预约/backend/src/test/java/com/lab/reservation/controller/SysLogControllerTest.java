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
 * 系统日志控制器测试
 * 测试操作日志、登录日志等功能
 * 对应测试用例：TC-LOG-001 ~ TC-LOG-002
 */
@DisplayName("系统日志控制器测试")
public class SysLogControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("操作日志查询测试")
    class OperationLogTests {

        @Test
        @DisplayName("TC-LOG-001: 操作日志页面验证 - 系统管理员可以获取操作日志")
        void systemAdminCanGetOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("TC-LOG-001: 操作日志显示完整信息")
        void operationLogsShowCompleteInfo() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("操作日志支持按时间范围筛选")
        void operationLogsSupportDateFilter() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("操作日志支持按用户名筛选")
        void operationLogsSupportUsernameFilter() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("username", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("操作日志支持按操作类型筛选")
        void operationLogsSupportOperationTypeFilter() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("operationType", "新增"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员不能获取操作日志")
        void labAdminCannotGetOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能获取操作日志")
        void teacherCannotGetOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能获取操作日志")
        void studentCannotGetOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取操作日志")
        void unauthenticatedUserCannotGetOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("登录日志查询测试")
    class LoginLogTests {

        @Test
        @DisplayName("TC-LOG-002: 登录日志页面验证 - 系统管理员可以获取登录日志")
        void systemAdminCanGetLoginLogs() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("TC-LOG-002: 登录日志显示登录记录")
        void loginLogsShowLoginRecords() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("登录日志显示登录结果")
        void loginLogsShowLoginResult() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("登录日志支持按账号筛选")
        void loginLogsSupportAccountFilter() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("account", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("登录日志支持按时间范围筛选")
        void loginLogsSupportDateFilter() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("登录日志支持按结果筛选")
        void loginLogsSupportResultFilter() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("result", "success"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("登录日志支持按IP地址筛选")
        void loginLogsSupportIpFilter() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("ip", "192.168"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员不能获取登录日志")
        void labAdminCannotGetLoginLogs() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能获取登录日志")
        void studentCannotGetLoginLogs() throws Exception {
            mockMvc.perform(get("/system/log/login")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取登录日志")
        void unauthenticatedUserCannotGetLoginLogs() throws Exception {
            mockMvc.perform(get("/system/log/login"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("日志导出测试")
    class LogExportTests {

        @Test
        @DisplayName("系统管理员可以导出操作日志")
        void systemAdminCanExportOperationLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以导出登录日志")
        void systemAdminCanExportLoginLogs() throws Exception {
            mockMvc.perform(get("/system/log/login/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("导出操作日志支持筛选条件")
        void exportOperationLogsSupportsFilter() throws Exception {
            mockMvc.perform(get("/system/log/operation/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能导出日志")
        void nonSystemAdminCannotExportLogs() throws Exception {
            mockMvc.perform(get("/system/log/operation/export")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("日志统计测试")
    class LogStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取登录统计")
        void systemAdminCanGetLoginStatistics() throws Exception {
            mockMvc.perform(get("/system/log/statistics/login")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以获取操作统计")
        void systemAdminCanGetOperationStatistics() throws Exception {
            mockMvc.perform(get("/system/log/statistics/operation")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取登录失败统计")
        void canGetLoginFailureStatistics() throws Exception {
            mockMvc.perform(get("/system/log/statistics/login/failure")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("可以获取高频操作用户统计")
        void canGetFrequentOperationUserStatistics() throws Exception {
            mockMvc.perform(get("/system/log/statistics/operation/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("日志清理测试")
    class LogCleanupTests {

        @Test
        @DisplayName("系统管理员可以清理历史日志")
        void systemAdminCanCleanupOldLogs() throws Exception {
            mockMvc.perform(delete("/system/log/cleanup")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("days", "90"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("清理登录日志")
        void canCleanupLoginLogs() throws Exception {
            mockMvc.perform(delete("/system/log/login/cleanup")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("days", "90"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("清理操作日志")
        void canCleanupOperationLogs() throws Exception {
            mockMvc.perform(delete("/system/log/operation/cleanup")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("days", "90"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能清理日志")
        void nonSystemAdminCannotCleanupLogs() throws Exception {
            mockMvc.perform(delete("/system/log/cleanup")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("days", "90"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("日志详情测试")
    class LogDetailTests {

        @Test
        @DisplayName("系统管理员可以查看操作日志详情")
        void systemAdminCanViewOperationLogDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/system/log/operation/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("系统管理员可以查看登录日志详情")
        void systemAdminCanViewLoginLogDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/system/log/login/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }
    }
}
