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
 * 用户管理控制器测试
 * 测试用户列表查询、新增、编辑、删除、角色分配等功能
 * 对应测试用例：TC-USER-LIST-001 ~ TC-USER-LIST-010
 */
@DisplayName("用户管理控制器测试")
public class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("用户列表查询测试")
    class UserListTests {

        @Test
        @DisplayName("TC-USER-LIST-001: 用户列表页面元素验证 - 系统管理员可以获取用户列表")
        void systemAdminCanGetUserList() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("TC-USER-LIST-002: 用户列表-条件筛选 - 按角色筛选")
        void userListSupportsUserTypeFilter() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("userType", "STUDENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-USER-LIST-003: 用户列表-关键字搜索 - 按用户名搜索")
        void userListSupportsUsernameSearch() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("username", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以获取用户列表")
        void labAdminCanGetUserList() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("教师不能获取用户列表")
        void teacherCannotGetUserList() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能获取用户列表")
        void studentCannotGetUserList() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录用户不能获取用户列表")
        void unauthenticatedUserCannotGetUserList() throws Exception {
            mockMvc.perform(get("/user/list")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("用户列表支持按姓名搜索")
        void userListSupportsRealNameSearch() throws Exception {
            mockMvc.perform(get("/user/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("username", "测试"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("新增用户测试")
    class AddUserTests {

        @Test
        @DisplayName("TC-USER-LIST-004: 用户列表-新增用户")
        void systemAdminCanAddUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "newuser" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "新用户");
            user.put("userType", "STUDENT");
            user.put("department", "计算机学院");
            user.put("email", "newuser@example.com");
            user.put("phone", "13800138000");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("添加成功"));
        }

        @Test
        @DisplayName("TC-USER-LIST-004: 新增用户-教师角色")
        void systemAdminCanAddTeacher() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "newteacher" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "新教师");
            user.put("userType", "TEACHER");
            user.put("department", "计算机学院");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("添加成功"));
        }

        @Test
        @DisplayName("TC-USER-LIST-004: 新增用户-实验室管理员角色")
        void systemAdminCanAddLabAdmin() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "newlabadmin" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "新实验室管理员");
            user.put("userType", "LAB_ADMIN");
            user.put("department", "实验室管理部");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("添加成功"));
        }

        @Test
        @DisplayName("TC-USER-LIST-004: 新增用户-维护人员角色")
        void systemAdminCanAddMaintainer() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "newmaintainer" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "新维护人员");
            user.put("userType", "MAINTAINER");
            user.put("department", "设备维护部");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("添加成功"));
        }

        @Test
        @DisplayName("不能创建系统管理员账号")
        void cannotCreateSystemAdmin() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "newadmin" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "新管理员");
            user.put("userType", "SYSTEM_ADMIN");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("不可通过用户管理创建系统管理员账号"));
        }

        @Test
        @DisplayName("实验室管理员不能新增用户")
        void labAdminCannotAddUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "labadminuser" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "实验室管理员添加的用户");
            user.put("userType", "STUDENT");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能新增用户")
        void teacherCannotAddUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "teacheruser" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "教师添加的用户");
            user.put("userType", "STUDENT");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能新增用户")
        void studentCannotAddUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "studentuser" + System.currentTimeMillis());
            user.put("password", "Test@123456");
            user.put("realName", "学生添加的用户");
            user.put("userType", "STUDENT");

            mockMvc.perform(post("/user")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("更新用户测试")
    class UpdateUserTests {

        @Test
        @DisplayName("TC-USER-LIST-005: 用户列表-编辑用户")
        void systemAdminCanUpdateUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("id", 2);
            user.put("realName", "更新后的姓名");
            user.put("department", "更新后的院系");

            mockMvc.perform(put("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }

        @Test
        @DisplayName("TC-USER-LIST-005: 编辑用户-更新邮箱")
        void systemAdminCanUpdateUserEmail() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("id", 2);
            user.put("email", "updated@example.com");

            mockMvc.perform(put("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("不能编辑内置管理员账号")
        void cannotUpdateBuiltInAdmin() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("id", 1);
            user.put("realName", "尝试更新内置管理员");

            mockMvc.perform(put("/user")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置管理员账号不可修改"));
        }

        @Test
        @DisplayName("实验室管理员不能更新用户")
        void labAdminCannotUpdateUser() throws Exception {
            Map<String, Object> user = new HashMap<>();
            user.put("id", 3);
            user.put("realName", "实验室管理员尝试更新");

            mockMvc.perform(put("/user")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("删除用户测试")
    class DeleteUserTests {

        @Test
        @DisplayName("TC-USER-LIST-006: 用户列表-删除用户")
        void systemAdminCanDeleteUser() throws Exception {
            mockMvc.perform(delete("/user/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-USER-LIST-006: 不能删除内置管理员")
        void cannotDeleteBuiltInAdmin() throws Exception {
            mockMvc.perform(delete("/user/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置管理员账号不可删除"));
        }

        @Test
        @DisplayName("TC-USER-LIST-009: 用户列表-批量删除")
        void systemAdminCanBatchDeleteUsers() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("ids", java.util.Arrays.asList(998, 997));

            mockMvc.perform(delete("/user/batch")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("批量删除-空数组")
        void batchDeleteWithEmptyArray() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("ids", java.util.Collections.emptyList());

            mockMvc.perform(delete("/user/batch")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("批量删除-无效参数")
        void batchDeleteWithInvalidParams() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("ids", "not an array");

            mockMvc.perform(delete("/user/batch")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("请传入 ids 数组"));
        }

        @Test
        @DisplayName("实验室管理员不能删除用户")
        void labAdminCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/user/999")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能删除用户")
        void teacherCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/user/999")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能删除用户")
        void studentCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/user/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("重置密码测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("TC-USER-LIST-007: 用户列表-重置密码")
        void systemAdminCanResetPassword() throws Exception {
            mockMvc.perform(put("/user/2/reset-password")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.defaultPasswordPlain").value("123456"));
        }

        @Test
        @DisplayName("实验室管理员不能重置密码")
        void labAdminCannotResetPassword() throws Exception {
            mockMvc.perform(put("/user/3/reset-password")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能重置密码")
        void studentCannotResetPassword() throws Exception {
            mockMvc.perform(put("/user/4/reset-password")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("启用/禁用用户测试")
    class UserStatusTests {

        @Test
        @DisplayName("TC-USER-LIST-008: 用户列表-启用/禁用 - 禁用用户")
        void systemAdminCanDisableUser() throws Exception {
            mockMvc.perform(put("/user/2/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("TC-USER-LIST-008: 用户列表-启用/禁用 - 启用用户")
        void systemAdminCanEnableUser() throws Exception {
            mockMvc.perform(put("/user/2/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("不能禁用内置管理员")
        void cannotDisableBuiltInAdmin() throws Exception {
            mockMvc.perform(put("/user/1/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置管理员账号不可禁用"));
        }

        @Test
        @DisplayName("实验室管理员不能启用/禁用用户")
        void labAdminCannotChangeUserStatus() throws Exception {
            mockMvc.perform(put("/user/3/status")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("status", "0"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("分配角色测试")
    class AssignRoleTests {

        @Test
        @DisplayName("TC-USER-LIST-008: 系统管理员可以分配角色")
        void systemAdminCanAssignRole() throws Exception {
            mockMvc.perform(put("/user/3/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "TEACHER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("角色分配成功"));
        }

        @Test
        @DisplayName("系统管理员可以将用户角色变更为实验室管理员")
        void systemAdminCanAssignLabAdminRole() throws Exception {
            mockMvc.perform(put("/user/4/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "LAB_ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("角色分配成功"));
        }

        @Test
        @DisplayName("系统管理员可以将用户角色变更为维护人员")
        void systemAdminCanAssignMaintainerRole() throws Exception {
            mockMvc.perform(put("/user/5/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "MAINTAINER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("角色分配成功"));
        }

        @Test
        @DisplayName("不能分配系统管理员角色")
        void cannotAssignSystemAdminRole() throws Exception {
            mockMvc.perform(put("/user/3/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "SYSTEM_ADMIN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("不可通过用户管理分配系统管理员角色"));
        }

        @Test
        @DisplayName("不能修改内置管理员角色")
        void cannotChangeBuiltInAdminRole() throws Exception {
            mockMvc.perform(put("/user/1/role")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "TEACHER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("内置管理员角色不可变更"));
        }

        @Test
        @DisplayName("实验室管理员不能分配角色")
        void labAdminCannotAssignRole() throws Exception {
            mockMvc.perform(put("/user/3/role")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("userType", "TEACHER"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("导出用户列表测试")
    class ExportUserTests {

        @Test
        @DisplayName("TC-USER-LIST-010: 用户列表-导出用户")
        void systemAdminCanExportUsers() throws Exception {
            mockMvc.perform(get("/user/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-USER-LIST-010: 导出用户支持按角色筛选")
        void systemAdminCanExportUsersWithFilter() throws Exception {
            mockMvc.perform(get("/user/export")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "STUDENT"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能导出用户")
        void nonSystemAdminCannotExportUsers() throws Exception {
            mockMvc.perform(get("/user/export")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能导出用户")
        void studentCannotExportUsers() throws Exception {
            mockMvc.perform(get("/user/export")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("按类型获取用户测试")
    class ListByTypeTests {

        @Test
        @DisplayName("系统管理员可以按类型获取用户列表")
        void systemAdminCanListUsersByType() throws Exception {
            mockMvc.perform(get("/user/by-type")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("userType", "MAINTAINER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以按类型获取用户列表")
        void labAdminCanListUsersByType() throws Exception {
            mockMvc.perform(get("/user/by-type")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("userType", "MAINTAINER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("教师不能按类型获取用户列表")
        void teacherCannotListUsersByType() throws Exception {
            mockMvc.perform(get("/user/by-type")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .param("userType", "MAINTAINER"))
                    .andExpect(status().isForbidden());
        }
    }
}
