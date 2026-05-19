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
 * 智能推荐控制器测试
 * 测试设备推荐、时段推荐等功能
 * 对应测试用例：TC-BOOKING-005
 */
@DisplayName("智能推荐控制器测试")
public class RecommendationControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("设备推荐测试")
    class DeviceRecommendationTests {

        @Test
        @DisplayName("TC-BOOKING-005: 智能推荐功能 - 可以获取推荐设备")
        void canGetRecommendedDevices() throws Exception {
            mockMvc.perform(get("/recommendation/devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-BOOKING-005: 推荐设备显示推荐理由")
        void recommendedDevicesShowReasons() throws Exception {
            mockMvc.perform(get("/recommendation/devices")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("推荐设备支持按类型筛选")
        void recommendedDevicesSupportTypeFilter() throws Exception {
            mockMvc.perform(get("/recommendation/devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("推荐设备支持按实验室筛选")
        void recommendedDevicesSupportLabFilter() throws Exception {
            mockMvc.perform(get("/recommendation/devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("labId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("热门设备推荐")
        void canGetHotDevices() throws Exception {
            mockMvc.perform(get("/recommendation/hot-devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("课程配套设备推荐")
        void canGetCourseRelatedDevices() throws Exception {
            mockMvc.perform(get("/recommendation/course-devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取推荐设备")
        void unauthenticatedUserCannotGetRecommendedDevices() throws Exception {
            mockMvc.perform(get("/recommendation/devices"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("时段推荐测试")
    class TimeSlotRecommendationTests {

        @Test
        @DisplayName("可以为设备推荐可用时段")
        void canRecommendAvailableSlots() throws Exception {
            mockMvc.perform(get("/recommendation/slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", "2026-03-27"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots").isArray());
        }

        @Test
        @DisplayName("时段推荐避开高峰")
        void recommendedSlotsAvoidPeakHours() throws Exception {
            mockMvc.perform(get("/recommendation/slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", "2026-03-27")
                            .param("avoidPeak", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots").isArray());
        }

        @Test
        @DisplayName("时段推荐支持连续时段")
        void recommendedSlotsSupportContinuousTime() throws Exception {
            mockMvc.perform(get("/recommendation/slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", "2026-03-27")
                            .param("duration", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots").isArray());
        }

        @Test
        @DisplayName("时段推荐显示拥挤度")
        void recommendedSlotsShowCrowdLevel() throws Exception {
            mockMvc.perform(get("/recommendation/slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", "2026-03-27"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能获取时段推荐")
        void unauthenticatedUserCannotGetSlotRecommendation() throws Exception {
            mockMvc.perform(get("/recommendation/slots")
                            .param("deviceId", "1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("替代设备推荐测试")
    class AlternativeDeviceRecommendationTests {

        @Test
        @DisplayName("可以获取替代设备推荐")
        void canGetAlternativeDevices() throws Exception {
            mockMvc.perform(get("/recommendation/alternatives")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", "2026-03-27")
                            .param("startTime", "09:00")
                            .param("endTime", "12:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("替代设备推荐显示相似度")
        void alternativeDevicesShowSimilarity() throws Exception {
            mockMvc.perform(get("/recommendation/alternatives")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("替代设备推荐支持按类型筛选")
        void alternativeDevicesSupportTypeFilter() throws Exception {
            mockMvc.perform(get("/recommendation/alternatives")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("用户画像推荐测试")
    class UserProfileRecommendationTests {

        @Test
        @DisplayName("可以获取用户画像推荐")
        void canGetUserProfileBasedRecommendations() throws Exception {
            mockMvc.perform(get("/recommendation/personalized")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("个性化推荐基于历史使用")
        void personalizedRecommendationBasedOnHistory() throws Exception {
            mockMvc.perform(get("/recommendation/personalized")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("个性化推荐基于专业背景")
        void personalizedRecommendationBasedOnMajor() throws Exception {
            mockMvc.perform(get("/recommendation/personalized")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("basedOn", "major"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("个性化推荐基于课程")
        void personalizedRecommendationBasedOnCourse() throws Exception {
            mockMvc.perform(get("/recommendation/personalized")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("basedOn", "course"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("推荐反馈测试")
    class RecommendationFeedbackTests {

        @Test
        @DisplayName("用户可以反馈推荐是否有用")
        void userCanFeedbackRecommendation() throws Exception {
            Map<String, Object> feedback = new HashMap<>();
            feedback.put("deviceId", 1);
            feedback.put("helpful", true);
            feedback.put("reason", "设备符合需求");

            mockMvc.perform(post("/recommendation/feedback")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(feedback)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以反馈推荐无用")
        void userCanFeedbackRecommendationNotHelpful() throws Exception {
            Map<String, Object> feedback = new HashMap<>();
            feedback.put("deviceId", 1);
            feedback.put("helpful", false);
            feedback.put("reason", "不符合实验需求");

            mockMvc.perform(post("/recommendation/feedback")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(feedback)))
                    .andExpect(status().isOk());
        }
    }
}
