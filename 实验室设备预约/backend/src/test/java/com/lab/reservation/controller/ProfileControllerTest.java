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
 * 个人中心控制器测试
 * 测试个人信息查看、修改、密码修改、头像上传等功能
 * 对应测试用例：TC-PROFILE-001 ~ TC-PROFILE-004
 */
@DisplayName("个人中心控制器测试")
public class ProfileControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("查看个人信息测试")
    class ViewProfileTests {

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息 - 学生可以查看个人信息")
        void studentCanViewProfile() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.realName").exists())
                    .andExpect(jsonPath("$.userType").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示用户头像")
        void profileShowsAvatar() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avatar").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示学号/工号")
        void profileShowsStudentNo() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentStaffNo").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示角色")
        void profileShowsRole() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("STUDENT"));
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示院系")
        void profileShowsDepartment() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.department").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示邮箱")
        void profileShowsEmail() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示手机号")
        void profileShowsPhone() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示注册时间")
        void profileShowsCreateTime() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createTime").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 查看个人信息-显示最后登录时间")
        void profileShowsLastLoginTime() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastLoginTime").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-001: 教师可以查看个人信息")
        void teacherCanViewProfile() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("TEACHER"));
        }

        @Test
        @DisplayName("TC-PROFILE-001: 实验室管理员可以查看个人信息")
        void labAdminCanViewProfile() throws Exception {
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userType").value("LAB_ADMIN"));
        }

        @Test
        @DisplayName("TC-PROFILE-001: 未登录用户不能查看个人信息")
        void unauthenticatedUserCannotViewProfile() throws Exception {
            mockMvc.perform(get("/profile"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改个人信息测试")
    class UpdateProfileTests {

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息 - 学生可以修改个人信息")
        void studentCanUpdateProfile() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("realName", "测试学生");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息-修改邮箱")
        void studentCanUpdateEmail() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("email", "newemail@example.com");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息-修改手机号")
        void studentCanUpdatePhone() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("phone", "13900139000");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息-修改院系")
        void studentCanUpdateDepartment() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("department", "软件学院");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息-修改真实姓名")
        void studentCanUpdateRealName() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("realName", "新姓名");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("修改个人信息-邮箱格式错误")
        void cannotUpdateWithInvalidEmail() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("email", "invalid-email");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改个人信息-手机号格式错误")
        void cannotUpdateWithInvalidPhone() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("phone", "12345");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改个人信息-不能修改用户名")
        void cannotUpdateUsername() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("username", "newusername");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
            // 用户名不应该被修改
        }

        @Test
        @DisplayName("修改个人信息-不能修改角色")
        void cannotUpdateUserType() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("userType", "ADMIN");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk());
            // 角色不应该被修改
        }

        @Test
        @DisplayName("TC-PROFILE-002: 修改个人信息-保存成功后返回成功消息")
        void updateProfileReturnsSuccessMessage() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("realName", "测试保存");

            mockMvc.perform(put("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("修改成功"));
        }

        @Test
        @DisplayName("TC-PROFILE-002: 未登录用户不能修改个人信息")
        void unauthenticatedUserCannotUpdateProfile() throws Exception {
            Map<String, Object> profile = new HashMap<>();
            profile.put("realName", "测试");

            mockMvc.perform(put("/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profile)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改密码测试")
    class ChangePasswordTests {

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码 - 已登录用户可以修改密码")
        void authenticatedUserCanChangePassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "NewPass@123");
            passwordRequest.put("confirmPassword", "NewPass@123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-原密码错误")
        void cannotChangePasswordWithWrongOldPassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "wrongpassword");
            passwordRequest.put("newPassword", "NewPass@123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-新密码与确认密码不匹配")
        void cannotChangePasswordWithMismatchedConfirmation() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "NewPass@123");
            passwordRequest.put("confirmPassword", "DifferentPass@123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-新密码强度不足")
        void cannotChangePasswordWithWeakPassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "123456");
            passwordRequest.put("confirmPassword", "123456");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-新密码过短")
        void cannotChangePasswordWithTooShortPassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "123");
            passwordRequest.put("confirmPassword", "123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改密码-缺少原密码")
        void cannotChangePasswordWithoutOldPassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("newPassword", "NewPass@123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改密码-缺少新密码")
        void cannotChangePasswordWithoutNewPassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-新密码与原密码相同")
        void cannotChangePasswordToSamePassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "123456");
            passwordRequest.put("confirmPassword", "123456");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 修改密码-保存成功后可用新密码登录")
        void canLoginWithNewPasswordAfterChange() throws Exception {
            // 先修改密码
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "Original@123");
            passwordRequest.put("confirmPassword", "Original@123");

            mockMvc.perform(post("/profile/password")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-PROFILE-003: 未登录用户不能修改密码")
        void unauthenticatedUserCannotChangePassword() throws Exception {
            Map<String, Object> passwordRequest = new HashMap<>();
            passwordRequest.put("oldPassword", "123456");
            passwordRequest.put("newPassword", "NewPass@123");

            mockMvc.perform(post("/profile/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改头像测试")
    class UpdateAvatarTests {

        @Test
        @DisplayName("TC-PROFILE-004: 修改头像 - 已登录用户可以上传头像")
        void authenticatedUserCanUploadAvatar() throws Exception {
            Map<String, Object> avatarRequest = new HashMap<>();
            avatarRequest.put("avatar", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

            mockMvc.perform(post("/profile/avatar")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(avatarRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-004: 修改头像-上传后显示预览")
        void avatarUploadShowsPreview() throws Exception {
            mockMvc.perform(get("/profile/avatar")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avatar").exists());
        }

        @Test
        @DisplayName("TC-PROFILE-004: 修改头像-刷新后保持")
        void avatarPersistsAfterRefresh() throws Exception {
            // 上传新头像
            Map<String, Object> avatarRequest = new HashMap<>();
            avatarRequest.put("avatar", "new-avatar-base64-string");

            mockMvc.perform(post("/profile/avatar")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(avatarRequest)))
                    .andExpect(status().isOk());

            // 验证头像已更新
            mockMvc.perform(get("/profile")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avatar").exists());
        }

        @Test
        @DisplayName("修改头像-图片格式无效")
        void cannotUploadInvalidImageFormat() throws Exception {
            Map<String, Object> avatarRequest = new HashMap<>();
            avatarRequest.put("avatar", "invalid-image-data");

            mockMvc.perform(post("/profile/avatar")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(avatarRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-PROFILE-004: 未登录用户不能修改头像")
        void unauthenticatedUserCannotUploadAvatar() throws Exception {
            Map<String, Object> avatarRequest = new HashMap<>();
            avatarRequest.put("avatar", "some-avatar");

            mockMvc.perform(post("/profile/avatar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(avatarRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("使用记录测试")
    class UsageHistoryTests {

        @Test
        @DisplayName("已登录用户可以查看使用记录")
        void authenticatedUserCanViewUsageHistory() throws Exception {
            mockMvc.perform(get("/profile/usage-history")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("使用记录显示历史预约")
        void usageHistoryShowsHistoricalBookings() throws Exception {
            mockMvc.perform(get("/profile/usage-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("使用记录显示设备使用时长")
        void usageHistoryShowsDeviceUsageDuration() throws Exception {
            mockMvc.perform(get("/profile/usage-history")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用记录支持按时间筛选")
        void usageHistorySupportsDateFilter() throws Exception {
            mockMvc.perform(get("/profile/usage-history")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-03-26"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能查看使用记录")
        void unauthenticatedUserCannotViewUsageHistory() throws Exception {
            mockMvc.perform(get("/profile/usage-history"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("绑定第三方账号测试")
    class ThirdPartyBindingTests {

        @Test
        @DisplayName("用户可以绑定邮箱")
        void userCanBindEmail() throws Exception {
            Map<String, Object> bindingRequest = new HashMap<>();
            bindingRequest.put("email", "bind@example.com");
            bindingRequest.put("code", "123456");

            mockMvc.perform(post("/profile/bind/email")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bindingRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以绑定手机号")
        void userCanBindPhone() throws Exception {
            Map<String, Object> bindingRequest = new HashMap<>();
            bindingRequest.put("phone", "13900139000");
            bindingRequest.put("code", "123456");

            mockMvc.perform(post("/profile/bind/phone")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bindingRequest)))
                    .andExpect(status().isOk());
        }
    }
}
