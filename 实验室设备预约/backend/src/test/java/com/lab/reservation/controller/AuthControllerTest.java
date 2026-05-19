package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器测试
 * 测试登录、注册、个人信息获取、密码修改等功能
 * 对应测试用例：TC-LOGIN-001 ~ TC-LOGIN-007, TC-REG-001 ~ TC-REG-006
 */
@DisplayName("认证控制器测试")
public class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("登录功能测试")
    class LoginTests {

        @Test
        @DisplayName("TC-LOGIN-001: 登录表单元素验证 - 系统管理员正确登录")
        void adminLoginSuccess() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "system_admin");
            loginRequest.put("password", "123456");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").value("system_admin"))
                    .andExpect(jsonPath("$.userType").value("SYSTEM_ADMIN"));
        }

        @Test
        @DisplayName("TC-LOGIN-002: 登录功能-账号密码正确")
        void studentLoginSuccess() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "student");
            loginRequest.put("password", "123456");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value("student"));
        }

        @Test
        @DisplayName("TC-LOGIN-003: 登录功能-账号不存在")
        void loginWithNonExistentAccount() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "notexist123");
            loginRequest.put("password", "123456");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("用户不存在"));
        }

        @Test
        @DisplayName("TC-LOGIN-004: 登录功能-密码错误")
        void loginWithWrongPassword() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "student");
            loginRequest.put("password", "wrongpass");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }

        @Test
        @DisplayName("TC-LOGIN-005: 登录功能-账号已禁用")
        void loginWithDisabledAccount() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "disabled_user");
            loginRequest.put("password", "123456");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("你已被禁用，请联系管理员"));
        }

        @Test
        @DisplayName("TC-LOGIN-006: 登录功能-空用户名")
        void loginWithEmptyUsername() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "");
            loginRequest.put("password", "123456");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("TC-LOGIN-007: 登录功能-空密码")
        void loginWithEmptyPassword() throws Exception {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "student");
            loginRequest.put("password", "");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("注册功能测试")
    class RegisterTests {

        @Test
        @DisplayName("TC-REG-001: 正常注册流程 - 学生注册")
        void studentRegisterSuccess() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "newstudent" + System.currentTimeMillis());
            registerRequest.put("password", "Test@123456");
            registerRequest.put("realName", "测试学生");
            registerRequest.put("userType", "STUDENT");
            registerRequest.put("department", "计算机学院");
            registerRequest.put("email", "test" + System.currentTimeMillis() + "@example.com");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("注册成功"));
        }

        @Test
        @DisplayName("TC-REG-002: 注册-用户名重复")
        void registerWithDuplicateUsername() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "student");
            registerRequest.put("password", "Test@123456");
            registerRequest.put("realName", "重复用户名");
            registerRequest.put("userType", "STUDENT");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-REG-003: 注册-密码不匹配")
        void registerWithMismatchedPasswords() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "testuser" + System.currentTimeMillis());
            registerRequest.put("password", "Test@123456");
            registerRequest.put("confirmPassword", "Test@123457");
            registerRequest.put("realName", "测试用户");
            registerRequest.put("userType", "STUDENT");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-REG-004: 注册-教师注册")
        void teacherRegisterSuccess() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "newteacher" + System.currentTimeMillis());
            registerRequest.put("password", "Test@123456");
            registerRequest.put("realName", "测试教师");
            registerRequest.put("userType", "TEACHER");
            registerRequest.put("department", "计算机学院");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("注册成功"));
        }

        @Test
        @DisplayName("TC-REG-005: 注册-邮箱格式错误")
        void registerWithInvalidEmail() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "testuser" + System.currentTimeMillis());
            registerRequest.put("password", "Test@123456");
            registerRequest.put("realName", "测试用户");
            registerRequest.put("userType", "STUDENT");
            registerRequest.put("email", "invalid-email");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-REG-006: 注册-必填字段为空")
        void registerWithMissingRequiredFields() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "");
            registerRequest.put("password", "Test@123456");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-REG-007: 注册-非法用户类型")
        void registerWithInvalidUserType() throws Exception {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", "testuser" + System.currentTimeMillis());
            registerRequest.put("password", "Test@123456");
            registerRequest.put("realName", "测试用户");
            registerRequest.put("userType", "SYSTEM_ADMIN");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("用户类型不合法"));
        }
    }

    @Nested
    @DisplayName("获取当前用户信息测试")
    class UserInfoTests {

        @Test
        @DisplayName("已登录用户可以获取个人信息")
        void authenticatedUserCanGetUserInfo() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.realName").exists())
                    .andExpect(jsonPath("$.userType").exists());
        }

        @Test
        @DisplayName("未登录用户不能获取个人信息")
        void unauthenticatedUserCannotGetUserInfo() throws Exception {
            mockMvc.perform(get("/auth/info"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("教师可以获取个人信息")
        void teacherCanGetUserInfo() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("TEACHER"));
        }

        @Test
        @DisplayName("实验室管理员可以获取个人信息")
        void labAdminCanGetUserInfo() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("LAB_ADMIN"));
        }
    }

    @Nested
    @DisplayName("获取菜单权限测试")
    class MenuTests {

        @Test
        @DisplayName("已登录用户可以获取菜单")
        void authenticatedUserCanGetMenus() throws Exception {
            mockMvc.perform(get("/auth/menus")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取菜单")
        void unauthenticatedUserCannotGetMenus() throws Exception {
            mockMvc.perform(get("/auth/menus"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("系统管理员可以获取完整菜单")
        void systemAdminCanGetFullMenus() throws Exception {
            mockMvc.perform(get("/auth/menus")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("修改密码测试")
    class PasswordChangeTests {

        @Test
        @DisplayName("已登录用户可以修改密码")
        void authenticatedUserCanChangePassword() throws Exception {
            Map<String, String> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "Test@654321");

            mockMvc.perform(post("/auth/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("修改密码-原密码错误")
        void changePasswordWithWrongOldPassword() throws Exception {
            Map<String, String> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "wrongpassword");
            passwordRequest.put("newPassword", "Test@654321");

            mockMvc.perform(post("/auth/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未登录用户不能修改密码")
        void unauthenticatedUserCannotChangePassword() throws Exception {
            Map<String, String> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "Test@654321");

            mockMvc.perform(post("/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("更新个人信息测试")
    class ProfileUpdateTests {

        @Test
        @DisplayName("已登录用户可以更新个人信息")
        void authenticatedUserCanUpdateProfile() throws Exception {
            Map<String, Object> profileRequest = new HashMap<>();
            profileRequest.put("phone", "13800138000");
            profileRequest.put("email", "updated@example.com");

            mockMvc.perform(put("/auth/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("保存成功"));
        }

        @Test
        @DisplayName("已登录用户可以更新真实姓名")
        void authenticatedUserCanUpdateRealName() throws Exception {
            Map<String, Object> profileRequest = new HashMap<>();
            profileRequest.put("realName", "新姓名");

            mockMvc.perform(put("/auth/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("保存成功"));
        }

        @Test
        @DisplayName("已登录用户可以更新院系")
        void authenticatedUserCanUpdateDepartment() throws Exception {
            Map<String, Object> profileRequest = new HashMap<>();
            profileRequest.put("department", "软件学院");

            mockMvc.perform(put("/auth/profile")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("保存成功"));
        }

        @Test
        @DisplayName("未登录用户不能更新个人信息")
        void unauthenticatedUserCannotUpdateProfile() throws Exception {
            Map<String, Object> profileRequest = new HashMap<>();
            profileRequest.put("phone", "13800138000");

            mockMvc.perform(put("/auth/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("通过手机号重置密码测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("用户可以通过手机号重置密码")
        void userCanResetPasswordByPhone() throws Exception {
            Map<String, String> resetRequest = new HashMap<>();
            resetRequest.put("username", "student");
            resetRequest.put("phone", "13800138000");
            resetRequest.put("newPassword", "Reset@123456");

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("重置密码-用户名与手机号不匹配")
        void resetPasswordWithMismatchedInfo() throws Exception {
            Map<String, String> resetRequest = new HashMap<>();
            resetRequest.put("username", "student");
            resetRequest.put("phone", "13999999999");
            resetRequest.put("newPassword", "Reset@123456");

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("JWT Token验证测试")
    class TokenValidationTests {

        @Test
        @DisplayName("有效的Token可以访问受保护资源")
        void validTokenCanAccessProtectedResource() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("无效的Token返回401")
        void invalidTokenReturns401() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", "Bearer invalid_token_123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("过期的Token返回401")
        void expiredTokenReturns401() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", "Bearer expired_token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("缺少Bearer前缀返回401")
        void tokenWithoutBearerPrefixReturns401() throws Exception {
            mockMvc.perform(get("/auth/info")
                            .header("Authorization", getStudentToken()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
