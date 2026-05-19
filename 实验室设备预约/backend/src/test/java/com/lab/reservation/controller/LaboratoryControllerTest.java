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
 * 实验室管理控制器测试
 * 测试实验室的CRUD操作及权限控制
 * 对应测试用例：TC-LAB-001 ~ TC-LAB-002
 */
@DisplayName("实验室管理控制器测试")
public class LaboratoryControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("实验室列表查询测试")
    class LaboratoryListTests {

        @Test
        @DisplayName("TC-LAB-001: 实验室列表页面元素验证 - 系统管理员可以获取实验室列表")
        void systemAdminCanGetLaboratoryList() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("TC-LAB-001: 实验室管理员可以获取实验室列表")
        void labAdminCanGetLaboratoryList() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("教师可以获取实验室列表")
        void teacherCanGetLaboratoryList() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生可以获取实验室列表")
        void studentCanGetLaboratoryList() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取实验室列表")
        void unauthenticatedUserCannotGetLaboratoryList() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("实验室列表支持按名称筛选")
        void laboratoryListSupportsNameFilter() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("laboratoryName", "化学"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室列表支持按状态筛选")
        void laboratoryListSupportsStatusFilter() throws Exception {
            mockMvc.perform(get("/laboratory/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("实验室详情查询测试")
    class LaboratoryDetailTests {

        @Test
        @DisplayName("已登录用户可以获取实验室详情")
        void authenticatedUserCanGetLaboratoryDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/laboratory/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取实验室详情")
        void unauthenticatedUserCannotGetLaboratoryDetail() throws Exception {
            mockMvc.perform(get("/laboratory/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("新增实验室测试")
    class AddLaboratoryTests {

        @Test
        @DisplayName("TC-LAB-002: 系统管理员可以新增实验室")
        void systemAdminCanAddLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "测试实验室" + System.currentTimeMillis());
            laboratory.put("laboratoryNo", "TEST" + System.currentTimeMillis());
            laboratory.put("location", "A栋101");
            laboratory.put("capacity", 30);
            laboratory.put("description", "自动化测试创建的实验室");

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-LAB-002: 新增实验室-完整信息")
        void systemAdminCanAddLaboratoryWithFullInfo() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "化学实验室1");
            laboratory.put("laboratoryNo", "CHEM" + System.currentTimeMillis());
            laboratory.put("location", "B栋201");
            laboratory.put("capacity", 25);
            laboratory.put("adminName", "管理员A");
            laboratory.put("contactPhone", "13800138000");
            laboratory.put("description", "化学实验室");

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("新增实验室-实验室编号重复")
        void cannotAddLaboratoryWithDuplicateNo() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "重复编号实验室" + System.currentTimeMillis());
            laboratory.put("laboratoryNo", "LAB001");

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("新增实验室-缺少必填字段")
        void cannotAddLaboratoryWithMissingFields() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "缺少编号的实验室");

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能新增实验室")
        void labAdminCannotAddLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "非法实验室" + System.currentTimeMillis());
            laboratory.put("laboratoryNo", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能新增实验室")
        void teacherCannotAddLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "非法实验室" + System.currentTimeMillis());
            laboratory.put("laboratoryNo", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能新增实验室")
        void studentCannotAddLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("laboratoryName", "非法实验室" + System.currentTimeMillis());
            laboratory.put("laboratoryNo", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/laboratory")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("更新实验室测试")
    class UpdateLaboratoryTests {

        @Test
        @DisplayName("系统管理员可以更新实验室")
        void systemAdminCanUpdateLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("id", 1);
            laboratory.put("laboratoryName", "更新后的实验室名称");
            laboratory.put("capacity", 35);

            mockMvc.perform(put("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }

        @Test
        @DisplayName("实验室管理员可以更新实验室")
        void labAdminCanUpdateLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("id", 1);
            laboratory.put("description", "实验室管理员更新");

            mockMvc.perform(put("/laboratory")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以更新实验室状态")
        void systemAdminCanUpdateLaboratoryStatus() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("id", 1);
            laboratory.put("status", 0);

            mockMvc.perform(put("/laboratory")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能更新实验室")
        void studentCannotUpdateLaboratory() throws Exception {
            Map<String, Object> laboratory = new HashMap<>();
            laboratory.put("id", 1);
            laboratory.put("laboratoryName", "学生尝试更新");

            mockMvc.perform(put("/laboratory")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(laboratory)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("删除实验室测试")
    class DeleteLaboratoryTests {

        @Test
        @DisplayName("系统管理员可以删除实验室")
        void systemAdminCanDeleteLaboratory() throws Exception {
            mockMvc.perform(delete("/laboratory/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("不能删除有设备关联的实验室")
        void cannotDeleteLaboratoryWithDevices() throws Exception {
            mockMvc.perform(delete("/laboratory/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能删除实验室")
        void labAdminCannotDeleteLaboratory() throws Exception {
            mockMvc.perform(delete("/laboratory/999")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能删除实验室")
        void studentCannotDeleteLaboratory() throws Exception {
            mockMvc.perform(delete("/laboratory/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("获取所有实验室测试")
    class GetAllLaboratoriesTests {

        @Test
        @DisplayName("已登录用户可以获取所有实验室下拉列表")
        void authenticatedUserCanGetAllLaboratories() throws Exception {
            mockMvc.perform(get("/laboratory/all")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取所有实验室")
        void unauthenticatedUserCannotGetAllLaboratories() throws Exception {
            mockMvc.perform(get("/laboratory/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("实验室设备统计测试")
    class LaboratoryDeviceStatsTests {

        @Test
        @DisplayName("系统管理员可以获取实验室设备统计")
        void systemAdminCanGetLaboratoryDeviceStats() throws Exception {
            mockMvc.perform(get("/laboratory/1/device-stats")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取本实验室设备统计")
        void labAdminCanGetLaboratoryDeviceStats() throws Exception {
            mockMvc.perform(get("/laboratory/1/device-stats")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取实验室设备统计")
        void studentCannotGetLaboratoryDeviceStats() throws Exception {
            mockMvc.perform(get("/laboratory/1/device-stats")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
