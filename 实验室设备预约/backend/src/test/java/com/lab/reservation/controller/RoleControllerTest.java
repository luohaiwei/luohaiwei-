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
 * 角色管理控制器测试
 * 测试角色列表、新增、编辑、删除、权限分配等功能
 * 对应测试用例：TC-ROLE-001 ~ TC-ROLE-003
 */
@DisplayName("角色管理控制器测试")
public class RoleControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("角色列表查询测试")
    class RoleListTests {

        @Test
        @DisplayName("TC-ROLE-001: 角色列表页面元素验证 - 系统管理员可以获取角色列表")
        void systemAdminCanGetRoleList() throws Exception {
            mockMvc.perform(get("/role/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("TC-ROLE-001: 角色列表显示系统预置角色")
        void roleListContainsSystemRoles() throws Exception {
            mockMvc.perform(get("/role/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("实验室管理员不能获取角色列表")
        void labAdminCannotGetRoleList() throws Exception {
            mockMvc.perform(get("/role/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能获取角色列表")
        void teacherCannotGetRoleList() throws Exception {
            mockMvc.perform(get("/role/list")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能获取角色列表")
        void studentCannotGetRoleList() throws Exception {
            mockMvc.perform(get("/role/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取角色列表")
        void unauthenticatedUserCannotGetRoleList() throws Exception {
            mockMvc.perform(get("/role/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("新增角色测试")
    class AddRoleTests {

        @Test
        @DisplayName("TC-ROLE-002: 系统管理员可以新增角色")
        void systemAdminCanAddRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "实验员" + System.currentTimeMillis());
            role.put("roleCode", "EXPEDITOR" + System.currentTimeMillis());
            role.put("description", "负责实验辅助工作");

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-ROLE-002: 新增角色-完整信息")
        void systemAdminCanAddRoleWithFullInfo() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "测试实验员");
            role.put("roleCode", "TEST_EXP" + System.currentTimeMillis());
            role.put("description", "自动化测试创建的角色");
            role.put("status", 1);

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("新增角色-角色名称重复")
        void cannotAddRoleWithDuplicateName() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "系统管理员");
            role.put("roleCode", "DUPLICATE" + System.currentTimeMillis());

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("新增角色-角色编码重复")
        void cannotAddRoleWithDuplicateCode() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "重复编码角色" + System.currentTimeMillis());
            role.put("roleCode", "SYSTEM_ADMIN");

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("新增角色-缺少必填字段")
        void cannotAddRoleWithMissingFields() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "缺少编码的角色");

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能新增角色")
        void labAdminCannotAddRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "非法角色");
            role.put("roleCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能新增角色")
        void teacherCannotAddRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "非法角色");
            role.put("roleCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能新增角色")
        void studentCannotAddRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("roleName", "非法角色");
            role.put("roleCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/role")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("更新角色测试")
    class UpdateRoleTests {

        @Test
        @DisplayName("系统管理员可以更新角色")
        void systemAdminCanUpdateRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("id", 2);
            role.put("roleName", "更新后的角色名称");
            role.put("description", "更新后的描述");

            mockMvc.perform(put("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }

        @Test
        @DisplayName("系统管理员可以更新角色状态")
        void systemAdminCanUpdateRoleStatus() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("id", 2);
            role.put("status", 0);

            mockMvc.perform(put("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("不能更新系统内置角色")
        void cannotUpdateBuiltInRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("id", 1);
            role.put("roleName", "尝试更新内置角色");

            mockMvc.perform(put("/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置角色不可修改"));
        }

        @Test
        @DisplayName("实验室管理员不能更新角色")
        void labAdminCannotUpdateRole() throws Exception {
            Map<String, Object> role = new HashMap<>();
            role.put("id", 2);
            role.put("roleName", "实验室管理员尝试更新");

            mockMvc.perform(put("/role")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("删除角色测试")
    class DeleteRoleTests {

        @Test
        @DisplayName("系统管理员可以删除角色")
        void systemAdminCanDeleteRole() throws Exception {
            mockMvc.perform(delete("/role/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("不能删除系统内置角色")
        void cannotDeleteBuiltInRole() throws Exception {
            mockMvc.perform(delete("/role/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置角色不可删除"));
        }

        @Test
        @DisplayName("不能删除已有用户关联的角色")
        void cannotDeleteRoleWithUsers() throws Exception {
            mockMvc.perform(delete("/role/2")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能删除角色")
        void labAdminCannotDeleteRole() throws Exception {
            mockMvc.perform(delete("/role/999")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能删除角色")
        void teacherCannotDeleteRole() throws Exception {
            mockMvc.perform(delete("/role/999")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能删除角色")
        void studentCannotDeleteRole() throws Exception {
            mockMvc.perform(delete("/role/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("分配权限测试")
    class AssignPermissionTests {

        @Test
        @DisplayName("TC-ROLE-003: 系统管理员可以分配权限")
        void systemAdminCanAssignPermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2, 3));

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("保存成功"));
        }

        @Test
        @DisplayName("TC-ROLE-003: 权限分配-多个权限")
        void systemAdminCanAssignMultiplePermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2, 3, 4, 5));

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-ROLE-003: 权限分配-清空权限")
        void systemAdminCanClearPermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Collections.emptyList());

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-ROLE-003: 不能为内置角色分配权限")
        void cannotAssignPermissionsToBuiltInRole() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2));

            mockMvc.perform(put("/role/1/permissions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置角色权限不可修改"));
        }

        @Test
        @DisplayName("实验室管理员不能分配权限")
        void labAdminCannotAssignPermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2));

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能分配权限")
        void teacherCannotAssignPermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2));

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能分配权限")
        void studentCannotAssignPermissions() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("permIds", java.util.Arrays.asList(1, 2));

            mockMvc.perform(put("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("获取角色权限测试")
    class GetRolePermissionsTests {

        @Test
        @DisplayName("系统管理员可以获取角色权限")
        void systemAdminCanGetRolePermissions() throws Exception {
            mockMvc.perform(get("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("实验室管理员不能获取角色权限")
        void labAdminCannotGetRolePermissions() throws Exception {
            mockMvc.perform(get("/role/6/permissions")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
