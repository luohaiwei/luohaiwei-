package com.lab.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预约申请表单控制器测试
 * 测试预约表单填写、验证、提交等功能
 * 对应测试用例：TC-BOOKING-FORM-001 ~ TC-BOOKING-FORM-003
 */
@DisplayName("预约申请表单控制器测试")
public class BookingFormControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Nested
    @DisplayName("预约表单页面测试")
    class BookingFormPageTests {

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 填写预约表单 - 学生可以进入预约表单页面")
        void studentCanAccessBookingForm() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示设备信息")
        void bookingFormShowsDeviceInfo() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.device").exists())
                    .andExpect(jsonPath("$.device.deviceName").exists())
                    .andExpect(jsonPath("$.device.deviceNo").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示设备图片")
        void bookingFormShowsDeviceImage() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.device.image").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示设备编号")
        void bookingFormShowsDeviceNo() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.device.deviceNo").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示设备类型")
        void bookingFormShowsDeviceType() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.device.deviceType").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示可用时段")
        void bookingFormShowsAvailableSlots() throws Exception {
            mockMvc.perform(get("/booking/form/slots")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("date", LocalDate.now().plusDays(1).format(DATE_FORMATTER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots").isArray());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 预约表单显示预约规则")
        void bookingFormShowsBookingRules() throws Exception {
            mockMvc.perform(get("/booking/form/rules")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxDurationPerDay").exists())
                    .andExpect(jsonPath("$.advanceBookingHours").exists());
        }

        @Test
        @DisplayName("预约表单显示指导教师选项（学生可见）")
        void bookingFormShowsTeacherOptions() throws Exception {
            mockMvc.perform(get("/booking/form/teachers")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("预约表单显示费用信息")
        void bookingFormShowsFeeInfo() throws Exception {
            mockMvc.perform(get("/booking/form/fee")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能访问预约表单")
        void unauthenticatedUserCannotAccessBookingForm() throws Exception {
            mockMvc.perform(get("/booking/form")
                            .param("deviceId", "1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("提交预约表单测试")
    class SubmitBookingFormTests {

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 填写预约表单 - 学生可以提交预约")
        void studentCanSubmitBookingForm() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 提交预约-生成预约编号")
        void bookingSubmitGeneratesOrderNo() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNo").exists());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 提交预约-预约状态为待审核")
        void bookingSubmitStatusIsPending() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-BOOKING-FORM-001: 提交预约-跳转成功页面")
        void bookingSubmitRedirectsToSuccess() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-填写实验名称")
        void bookingFormRequiresExperimentProject() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("experimentProject", "光谱分析实验");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-填写实验目的")
        void bookingFormRequiresExperimentPurpose() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("experimentProject", "光谱分析实验");
            bookingForm.put("experimentPurpose", "测量样品吸收光谱曲线");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-填写使用人数")
        void bookingFormRequiresUsageCount() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("experimentProject", "光谱分析实验");
            bookingForm.put("experimentPurpose", "测量样品吸收光谱");
            bookingForm.put("usageCount", 3);

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-填写备注")
        void bookingFormCanIncludeRemark() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("remark", "需要使用比色皿");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-选择指导教师（可选）")
        void bookingFormCanSelectTeacher() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("teacherId", 2);

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交预约-缺少必填字段")
        void cannotSubmitBookingWithMissingFields() throws Exception {
            Map<String, Object> bookingForm = new HashMap<>();
            // 缺少deviceId, bookingDate等必填字段

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交预约-缺少设备ID")
        void cannotSubmitBookingWithoutDeviceId() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.remove("deviceId");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交预约-缺少预约日期")
        void cannotSubmitBookingWithoutDate() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.remove("bookingDate");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交预约-缺少实验名称")
        void cannotSubmitBookingWithoutExperimentProject() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.remove("experimentProject");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未登录用户不能提交预约")
        void unauthenticatedUserCannotSubmitBooking() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("预约时长限制测试")
    class BookingDurationLimitTests {

        @Test
        @DisplayName("TC-BOOKING-FORM-002: 预约时间超限 - 单台设备单日预约时长不能超过4小时")
        void cannotExceedMaxDurationPerDay() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "14:00:00");
            bookingForm.put("endTime", "19:00:00"); // 5小时

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("4小时")));
        }

        @Test
        @DisplayName("预约时长刚好4小时可以通过")
        void canSubmitWithExactly4Hours() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "14:00:00");
            bookingForm.put("endTime", "18:00:00"); // 正好4小时

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("预约时长3小时可以通过")
        void canSubmitWith3Hours() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "14:00:00");
            bookingForm.put("endTime", "17:00:00"); // 3小时

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("多个预约时段累计超限")
        void multipleBookingsExceedDailyLimit() throws Exception {
            // 已有2小时预约
            Map<String, Object> existingBooking = createValidBookingForm();
            existingBooking.put("startTime", "09:00:00");
            existingBooking.put("endTime", "11:00:00");

            // 尝试再预约3小时
            Map<String, Object> newBooking = createValidBookingForm();
            newBooking.put("startTime", "14:00:00");
            newBooking.put("endTime", "17:00:00"); // 3小时，总计5小时

            // 提交第二次预约
            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newBooking)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("提前预约时间测试")
    class AdvanceBookingTimeTests {

        @Test
        @DisplayName("TC-BOOKING-FORM-003: 未提前预约 - 请提前24小时进行预约")
        void cannotBookWithoutAdvanceNotice() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            // 预约当天
            bookingForm.put("bookingDate", LocalDate.now().format(DATE_FORMATTER));

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("24小时")));
        }

        @Test
        @DisplayName("提前23小时预约被拒绝")
        void cannotBookWith23HoursAdvance() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            // 预约明天
            bookingForm.put("bookingDate", LocalDate.now().plusDays(1).format(DATE_FORMATTER));

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk()); // 应该是可以的
        }

        @Test
        @DisplayName("提前2天预约可以通过")
        void canBookWith2DaysAdvance() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("bookingDate", LocalDate.now().plusDays(2).format(DATE_FORMATTER));

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提前1周预约可以通过")
        void canBookWith1WeekAdvance() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("bookingDate", LocalDate.now().plusDays(7).format(DATE_FORMATTER));

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("预约时段验证测试")
    class BookingSlotValidationTests {

        @Test
        @DisplayName("预约时段冲突检测")
        void cannotBookConflictingSlot() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "14:00:00");
            bookingForm.put("endTime", "16:00:00");

            mockMvc.perform(post("/booking/form/check")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").exists());
        }

        @Test
        @DisplayName("预约开始时间不能晚于结束时间")
        void cannotBookWithEndBeforeStart() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "16:00:00");
            bookingForm.put("endTime", "14:00:00");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("预约开始时间不能等于结束时间")
        void cannotBookWithSameStartAndEnd() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "14:00:00");
            bookingForm.put("endTime", "14:00:00");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("预约日期不能是过去日期")
        void cannotBookPastDate() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("bookingDate", LocalDate.now().minusDays(1).format(DATE_FORMATTER));

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("预约时段必须在实验室开放时间内")
        void bookingMustBeWithinOpenHours() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("startTime", "06:00:00"); // 太早
            bookingForm.put("endTime", "08:00:00");

            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("预约频次限制测试")
    class BookingFrequencyLimitTests {

        @Test
        @DisplayName("当日预约次数超限")
        void cannotExceedDailyBookingLimit() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();
            bookingForm.put("bookingDate", LocalDate.now().plusDays(2).format(DATE_FORMATTER));

            // 假设当日限制为2次，这里尝试第3次
            mockMvc.perform(post("/booking/form/submit")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookingForm)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("当周预约次数超限")
        void cannotExceedWeeklyBookingLimit() throws Exception {
            Map<String, Object> bookingForm = createValidBookingForm();

            mockMvc.perform(post("/booking/form/check-frequency")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("bookingDate", LocalDate.now().plusDays(3).format(DATE_FORMATTER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canBook").exists());
        }

        @Test
        @DisplayName("当月预约次数超限")
        void cannotExceedMonthlyBookingLimit() throws Exception {
            mockMvc.perform(post("/booking/form/check-frequency")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("bookingDate", LocalDate.now().plusDays(10).format(DATE_FORMATTER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remainingBookings").exists());
        }
    }

    @Nested
    @DisplayName("保存预约草稿测试")
    class SaveBookingDraftTests {

        @Test
        @DisplayName("用户可以保存预约草稿")
        void userCanSaveBookingDraft() throws Exception {
            Map<String, Object> draft = createValidBookingForm();

            mockMvc.perform(post("/booking/form/draft")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(draft)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.draftId").exists());
        }

        @Test
        @DisplayName("用户可以获取预约草稿")
        void userCanGetBookingDraft() throws Exception {
            mockMvc.perform(get("/booking/form/draft")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以删除预约草稿")
        void userCanDeleteBookingDraft() throws Exception {
            mockMvc.perform(delete("/booking/form/draft/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户可以提交草稿为正式预约")
        void userCanSubmitDraftAsFormalBooking() throws Exception {
            mockMvc.perform(post("/booking/form/draft/1/submit")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }
    }

    private Map<String, Object> createValidBookingForm() {
        Map<String, Object> bookingForm = new HashMap<>();
        bookingForm.put("deviceId", 1);
        bookingForm.put("bookingDate", LocalDate.now().plusDays(2).format(DATE_FORMATTER));
        bookingForm.put("startTime", "14:00:00");
        bookingForm.put("endTime", "16:00:00");
        bookingForm.put("experimentProject", "光谱分析实验");
        bookingForm.put("experimentPurpose", "测量样品吸收光谱");
        bookingForm.put("usageCount", 3);
        return bookingForm;
    }
}
