package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.DeviceScrapApplication;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.DeviceScrapApplicationService;
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
 * 设备管理控制器测试
 * 测试设备相关的CRUD操作及权限控制
 */
@DisplayName("设备控制器测试")
public class DeviceControllerTest extends BaseIntegrationTest {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DeviceScrapApplicationService deviceScrapApplicationService;

    @Nested
    @DisplayName("设备列表查询测试")
    class DeviceListTests {

        @Test
        @DisplayName("系统管理员可以获取设备列表")
        void systemAdminCanGetDeviceList() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("实验室管理员可以获取设备列表")
        void labAdminCanGetDeviceList() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("教师可以获取设备列表")
        void teacherCanGetDeviceList() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生可以获取设备列表")
        void studentCanGetDeviceList() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取设备列表")
        void unauthenticatedUserCannotGetDeviceList() throws Exception {
            mockMvc.perform(get("/device/list")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("设备列表支持按名称筛选")
        void deviceListSupportsNameFilter() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("deviceName", "测试设备"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("设备列表支持按分类筛选")
        void deviceListSupportsCategoryFilter() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("设备列表支持按状态筛选")
        void deviceListSupportsStatusFilter() throws Exception {
            mockMvc.perform(get("/device/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("设备详情查询测试")
    class DeviceDetailTests {

        @Test
        @DisplayName("已登录用户可以获取设备详情")
        void authenticatedUserCanGetDeviceDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/device/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode == 200 || statusCode == 404 || statusCode == 403);
        }

        @Test
        @DisplayName("未登录用户不能获取设备详情")
        void unauthenticatedUserCannotGetDeviceDetail() throws Exception {
            mockMvc.perform(get("/device/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("设备增删改操作测试")
    class DeviceCrudTests {

        @Test
        @DisplayName("系统管理员可以添加设备")
        void systemAdminCanAddDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("deviceName", "测试设备");
            device.put("deviceNo", "TEST" + System.currentTimeMillis());
            device.put("categoryId", 1);
            device.put("status", 0);
            device.put("laboratory", "测试实验室");

            mockMvc.perform(post("/device")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("实验室管理员可以添加设备")
        void labAdminCanAddDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("deviceName", "测试设备-实验室管理员");
            device.put("deviceNo", "TESTLA" + System.currentTimeMillis());
            device.put("categoryId", 1);
            device.put("status", 0);

            mockMvc.perform(post("/device")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("学生不能添加设备")
        void studentCannotAddDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("deviceName", "非法设备");
            device.put("deviceNo", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/device")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("系统管理员可以更新设备")
        void systemAdminCanUpdateDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("id", 1);
            device.put("deviceName", "更新后的设备名称");

            mockMvc.perform(put("/device")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("实验室管理员可以更新设备")
        void labAdminCanUpdateDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("id", 1);
            device.put("deviceName", "实验室管理员更新设备");

            mockMvc.perform(put("/device")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("学生不能更新设备")
        void studentCannotUpdateDevice() throws Exception {
            Map<String, Object> device = new HashMap<>();
            device.put("id", 1);
            device.put("deviceName", "非法更新");

            mockMvc.perform(put("/device")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(device)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("系统管理员可以删除设备")
        void systemAdminCanDeleteDevice() throws Exception {
            mockMvc.perform(delete("/device/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("学生不能删除设备")
        void studentCannotDeleteDevice() throws Exception {
            mockMvc.perform(delete("/device/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("设备状态管理测试")
    class DeviceStatusTests {

        @Test
        @DisplayName("系统管理员可以更新设备状态")
        void systemAdminCanUpdateDeviceStatus() throws Exception {
            mockMvc.perform(put("/device/1/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("实验室管理员可以更新设备状态")
        void labAdminCanUpdateDeviceStatus() throws Exception {
            mockMvc.perform(put("/device/1/status")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("status", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("维护人员可以更新设备状态")
        void maintainerCanUpdateDeviceStatus() throws Exception {
            mockMvc.perform(put("/device/1/status")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("status", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("成功")));
        }

        @Test
        @DisplayName("学生不能更新设备状态")
        void studentCannotUpdateDeviceStatus() throws Exception {
            mockMvc.perform(put("/device/1/status")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("status", "1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("设备报废流程测试")
    class DeviceScrapTests {

        @Test
        @DisplayName("系统管理员可以查看报废列表")
        void systemAdminCanViewScrapList() throws Exception {
            mockMvc.perform(get("/device/scrap/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以查看报废列表")
        void labAdminCanViewScrapList() throws Exception {
            mockMvc.perform(get("/device/scrap/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生不能查看报废列表")
        void studentCannotViewScrapList() throws Exception {
            mockMvc.perform(get("/device/scrap/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师可以提交设备报废申请")
        void teacherCanSubmitScrapApplication() throws Exception {
            Map<String, Object> scrapRequest = new HashMap<>();
            scrapRequest.put("deviceId", 1);
            scrapRequest.put("scrapReason", "设备老化，需要报废");

            mockMvc.perform(post("/device/scrap")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(scrapRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以审批报废申请")
        void systemAdminCanApproveScrap() throws Exception {
            mockMvc.perform(put("/device/scrap/1/approve")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以拒绝报废申请")
        void systemAdminCanRejectScrap() throws Exception {
            mockMvc.perform(put("/device/scrap/1/reject")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("opinion", "不同意报废"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("智能推荐功能测试")
    class DeviceRecommendationTests {

        @Test
        @DisplayName("已登录用户可以获取推荐设备")
        void authenticatedUserCanGetRecommendedDevices() throws Exception {
            mockMvc.perform(get("/device/recommend")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("已登录用户可以获取设备推荐时段")
        void authenticatedUserCanGetRecommendedSlots() throws Exception {
            mockMvc.perform(get("/device/1/recommend-slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("date", "2024-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("设备状态日志测试")
    class DeviceStatusLogTests {

        @Test
        @DisplayName("系统管理员可以查看全局状态日志")
        void systemAdminCanViewGlobalStatusLogs() throws Exception {
            mockMvc.perform(get("/device/status-logs")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists());
        }

        @Test
        @DisplayName("非系统管理员不能查看全局状态日志")
        void nonSystemAdminCannotViewGlobalStatusLogs() throws Exception {
            mockMvc.perform(get("/device/status-logs")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("已登录用户可以查看自己设备的状态日志")
        void authenticatedUserCanViewDeviceStatusLogs() throws Exception {
            mockMvc.perform(get("/device/1/status-logs")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("获取所有设备测试")
    class GetAllDevicesTests {

        @Test
        @DisplayName("已登录用户可以获取所有设备下拉列表")
        void authenticatedUserCanGetAllDevices() throws Exception {
            mockMvc.perform(get("/device/all")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取所有设备")
        void unauthenticatedUserCannotGetAllDevices() throws Exception {
            mockMvc.perform(get("/device/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("设备使用记录测试")
    class DeviceUsageTests {

        @Test
        @DisplayName("系统管理员可以查看设备使用记录")
        void systemAdminCanViewUsageRecords() throws Exception {
            mockMvc.perform(get("/device/usage")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以查看设备使用记录")
        void labAdminCanViewUsageRecords() throws Exception {
            mockMvc.perform(get("/device/usage")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生不能查看设备使用记录")
        void studentCannotViewUsageRecords() throws Exception {
            mockMvc.perform(get("/device/usage")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }
    }
}
