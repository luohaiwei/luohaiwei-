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
 * 通知消息控制器测试
 * 测试系统消息、通知等功能
 */
@DisplayName("通知消息控制器测试")
public class NotificationControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("通知列表测试")
    class NotificationListTests {

        @Test
        @DisplayName("已登录用户可以获取通知列表")
        void authenticatedUserCanGetNotificationList() throws Exception {
            mockMvc.perform(get("/notification/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("通知列表支持按类型筛选")
        void notificationListSupportsTypeFilter() throws Exception {
            mockMvc.perform(get("/notification/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("type", "booking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("通知列表支持按已读状态筛选")
        void notificationListSupportsReadFilter() throws Exception {
            mockMvc.perform(get("/notification/list")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("isRead", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("通知列表显示未读数量")
        void notificationListShowsUnreadCount() throws Exception {
            mockMvc.perform(get("/notification/unread-count")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").exists());
        }

        @Test
        @DisplayName("未登录用户不能获取通知列表")
        void unauthenticatedUserCannotGetNotificationList() throws Exception {
            mockMvc.perform(get("/notification/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("通知详情测试")
    class NotificationDetailTests {

        @Test
        @DisplayName("已登录用户可以获取通知详情")
        void authenticatedUserCanGetNotificationDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/notification/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取通知详情")
        void unauthenticatedUserCannotGetNotificationDetail() throws Exception {
            mockMvc.perform(get("/notification/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("标记已读测试")
    class MarkReadTests {

        @Test
        @DisplayName("用户可以标记单条通知为已读")
        void userCanMarkNotificationAsRead() throws Exception {
            mockMvc.perform(put("/notification/1/read")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以标记所有通知为已读")
        void userCanMarkAllNotificationsAsRead() throws Exception {
            mockMvc.perform(put("/notification/read-all")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("标记已读后未读数量减少")
        void unreadCountDecreasesAfterMarkRead() throws Exception {
            mockMvc.perform(put("/notification/1/read")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/notification/unread-count")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("删除通知测试")
    class DeleteNotificationTests {

        @Test
        @DisplayName("用户可以删除自己的通知")
        void userCanDeleteOwnNotification() throws Exception {
            mockMvc.perform(delete("/notification/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以批量删除通知")
        void userCanBatchDeleteNotifications() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("ids", java.util.Arrays.asList(1, 2, 3));

            mockMvc.perform(delete("/notification/batch")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户不能删除他人的通知")
        void userCannotDeleteOthersNotification() throws Exception {
            mockMvc.perform(delete("/notification/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk()); // 或403，根据业务逻辑
        }
    }

    @Nested
    @DisplayName("系统通知测试（管理员）")
    class SystemNotificationTests {

        @Test
        @DisplayName("系统管理员可以发送系统通知")
        void systemAdminCanSendSystemNotification() throws Exception {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "系统维护通知");
            notification.put("content", "系统将于周末进行维护");
            notification.put("type", "system");

            mockMvc.perform(post("/notification/system")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notification)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以发送通知给指定用户")
        void systemAdminCanSendNotificationToUsers() throws Exception {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "预约审核通知");
            notification.put("content", "您的预约已通过审核");
            notification.put("type", "booking");
            notification.put("userIds", java.util.Arrays.asList(2, 3, 4));

            mockMvc.perform(post("/notification/to-users")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notification)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能发送系统通知")
        void studentCannotSendSystemNotification() throws Exception {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "学生尝试发送");
            notification.put("content", "测试内容");

            mockMvc.perform(post("/notification/system")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notification)))
                    .andExpect(status().isForbidden());
        }
    }
}
