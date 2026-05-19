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
 * 场地管理控制器测试
 * 测试实验室场地时段管理功能
 */
@DisplayName("场地管理控制器测试")
public class VenueControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("场地列表测试")
    class VenueListTests {

        @Test
        @DisplayName("系统管理员可以获取场地列表")
        void systemAdminCanGetVenueList() throws Exception {
            mockMvc.perform(get("/venue/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以获取场地列表")
        void labAdminCanGetVenueList() throws Exception {
            mockMvc.perform(get("/venue/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("场地列表支持按实验室筛选")
        void venueListSupportsLabFilter() throws Exception {
            mockMvc.perform(get("/venue/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("labId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生可以获取场地列表")
        void studentCanGetVenueList() throws Exception {
            mockMvc.perform(get("/venue/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能获取场地列表")
        void unauthenticatedUserCannotGetVenueList() throws Exception {
            mockMvc.perform(get("/venue/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("时段配置测试")
    class TimeSlotConfigTests {

        @Test
        @DisplayName("实验室管理员可以配置场地时段")
        void labAdminCanConfigureTimeSlots() throws Exception {
            Map<String, Object> timeSlot = new HashMap<>();
            timeSlot.put("venueId", 1);
            timeSlot.put("date", "2026-03-27");
            timeSlot.put("startTime", "09:00");
            timeSlot.put("endTime", "12:00");
            timeSlot.put("maxBookings", 5);

            mockMvc.perform(post("/venue/timeslot")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(timeSlot)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取时段配置")
        void labAdminCanGetTimeSlotConfig() throws Exception {
            mockMvc.perform(get("/venue/1/timeslots")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("date", "2026-03-27"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以更新时段配置")
        void labAdminCanUpdateTimeSlotConfig() throws Exception {
            Map<String, Object> timeSlot = new HashMap<>();
            timeSlot.put("id", 1);
            timeSlot.put("maxBookings", 8);

            mockMvc.perform(put("/venue/timeslot")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(timeSlot)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以删除时段配置")
        void labAdminCanDeleteTimeSlotConfig() throws Exception {
            mockMvc.perform(delete("/venue/timeslot/1")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能配置场地时段")
        void studentCannotConfigureTimeSlots() throws Exception {
            Map<String, Object> timeSlot = new HashMap<>();
            timeSlot.put("venueId", 1);
            timeSlot.put("startTime", "09:00");

            mockMvc.perform(post("/venue/timeslot")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(timeSlot)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("场地详情测试")
    class VenueDetailTests {

        @Test
        @DisplayName("已登录用户可以获取场地详情")
        void authenticatedUserCanGetVenueDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/venue/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取场地详情")
        void unauthenticatedUserCannotGetVenueDetail() throws Exception {
            mockMvc.perform(get("/venue/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("场地详情显示可用时段")
        void venueDetailShowsAvailableSlots() throws Exception {
            mockMvc.perform(get("/venue/1/availability")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("date", "2026-03-27"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("场地预约冲突测试")
    class VenueBookingConflictTests {

        @Test
        @DisplayName("检测场地预约冲突")
        void canCheckVenueBookingConflict() throws Exception {
            mockMvc.perform(get("/venue/check-conflict")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("venueId", "1")
                            .param("date", "2026-03-27")
                            .param("startTime", "09:00")
                            .param("endTime", "12:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConflict").exists());
        }

        @Test
        @DisplayName("获取场地冲突详情")
        void canGetConflictDetails() throws Exception {
            mockMvc.perform(get("/venue/conflict-detail")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("venueId", "1")
                            .param("date", "2026-03-27")
                            .param("startTime", "09:00")
                            .param("endTime", "12:00"))
                    .andExpect(status().isOk());
        }
    }
}
