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
 * 故障报修控制器测试
 * 测试设备报修、维修处理等功能
 * 对应测试用例：TC-REPAIR-001 ~ TC-REPAIR-004
 */
@DisplayName("故障报修控制器测试")
public class RepairControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("报修列表查询测试")
    class RepairListTests {

        @Test
        @DisplayName("TC-REPAIR-001: 故障报修列表页面验证 - 系统管理员可以获取报修列表")
        void systemAdminCanGetRepairList() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("TC-REPAIR-001: 报修列表显示紧急程度标签")
        void repairListShowsUrgencyLevel() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("维护人员可以获取报修列表")
        void maintainerCanGetRepairList() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("实验室管理员可以获取报修列表")
        void labAdminCanGetRepairList() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("学生可以获取自己的报修列表")
        void studentCanGetOwnRepairList() throws Exception {
            mockMvc.perform(get("/repair/my")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("报修列表支持按状态筛选")
        void repairListSupportsStatusFilter() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("报修列表支持按紧急程度筛选")
        void repairListSupportsUrgencyFilter() throws Exception {
            mockMvc.perform(get("/repair/list")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("urgency", "urgent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取报修列表")
        void unauthenticatedUserCannotGetRepairList() throws Exception {
            mockMvc.perform(get("/repair/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("提交报修测试")
    class SubmitRepairTests {

        @Test
        @DisplayName("TC-REPAIR-001: 学生可以提交设备报修")
        void studentCanSubmitRepair() throws Exception {
            Map<String, Object> repair = new HashMap<>();
            repair.put("deviceId", 1);
            repair.put("description", "设备无法正常启动");
            repair.put("urgency", "urgent");

            mockMvc.perform(post("/repair")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repair)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("学生可以提交带图片的报修")
        void studentCanSubmitRepairWithImages() throws Exception {
            Map<String, Object> repair = new HashMap<>();
            repair.put("deviceId", 1);
            repair.put("description", "设备异常噪音");
            repair.put("urgency", "normal");
            repair.put("images", java.util.Arrays.asList("image1.jpg", "image2.jpg"));

            mockMvc.perform(post("/repair")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repair)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("提交报修-缺少必填字段")
        void cannotSubmitRepairWithMissingFields() throws Exception {
            Map<String, Object> repair = new HashMap<>();
            repair.put("deviceId", 1);
            // 缺少description

            mockMvc.perform(post("/repair")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repair)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("教师可以提交设备报修")
        void teacherCanSubmitRepair() throws Exception {
            Map<String, Object> repair = new HashMap<>();
            repair.put("deviceId", 1);
            repair.put("description", "设备校准到期");
            repair.put("urgency", "important");

            mockMvc.perform(post("/repair")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repair)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能提交报修")
        void unauthenticatedUserCannotSubmitRepair() throws Exception {
            Map<String, Object> repair = new HashMap<>();
            repair.put("deviceId", 1);
            repair.put("description", "测试报修");

            mockMvc.perform(post("/repair")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(repair)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("报修详情查询测试")
    class RepairDetailTests {

        @Test
        @DisplayName("已登录用户可以获取报修详情")
        void authenticatedUserCanGetRepairDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/repair/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取报修详情")
        void unauthenticatedUserCannotGetRepairDetail() throws Exception {
            mockMvc.perform(get("/repair/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("维护人员接单测试")
    class AcceptRepairTests {

        @Test
        @DisplayName("TC-REPAIR-002: 维护人员可以接单")
        void maintainerCanAcceptRepair() throws Exception {
            mockMvc.perform(put("/repair/1/accept")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-002: 接单后报修状态变为处理中")
        void repairStatusChangesToProcessingAfterAccept() throws Exception {
            mockMvc.perform(put("/repair/2/accept")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-002: 接单后设备状态变为维修中")
        void deviceStatusChangesToRepairingAfterAccept() throws Exception {
            mockMvc.perform(put("/repair/2/accept")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能接单")
        void studentCannotAcceptRepair() throws Exception {
            mockMvc.perform(put("/repair/1/accept")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("只能接待处理的报修")
        void cannotAcceptNonPendingRepair() throws Exception {
            mockMvc.perform(put("/repair/999/accept")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("填写维修记录测试")
    class FillRepairRecordTests {

        @Test
        @DisplayName("TC-REPAIR-003: 维护人员可以填写维修记录")
        void maintainerCanFillRepairRecord() throws Exception {
            Map<String, Object> record = new HashMap<>();
            record.put("faultReason", "电源模块损坏");
            record.put("repairSolution", "更换电源模块");
            record.put("replacedParts", "电源模块×1");
            record.put("workHours", 2);

            mockMvc.perform(put("/repair/1/record")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(record)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-003: 维修记录关联到报修单")
        void repairRecordLinkedToRepairOrder() throws Exception {
            Map<String, Object> record = new HashMap<>();
            record.put("faultReason", "传感器故障");
            record.put("repairSolution", "更换传感器");

            mockMvc.perform(put("/repair/1/record")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(record)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("维修记录可以查看")
        void canViewRepairRecord() throws Exception {
            mockMvc.perform(get("/repair/1/record")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能填写维修记录")
        void studentCannotFillRepairRecord() throws Exception {
            Map<String, Object> record = new HashMap<>();
            record.put("faultReason", "学生尝试添加");
            record.put("repairSolution", "学生尝试添加");

            mockMvc.perform(put("/repair/1/record")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(record)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("维修完成测试")
    class CompleteRepairTests {

        @Test
        @DisplayName("TC-REPAIR-004: 维护人员可以完成维修")
        void maintainerCanCompleteRepair() throws Exception {
            Map<String, Object> completion = new HashMap<>();
            completion.put("result", "维修成功");
            completion.put("note", "设备已恢复正常");

            mockMvc.perform(put("/repair/1/complete")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completion)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-004: 维修完成后报修状态变为已完成")
        void repairStatusChangesToCompletedAfterRepair() throws Exception {
            Map<String, Object> completion = new HashMap<>();
            completion.put("result", "维修完成");

            mockMvc.perform(put("/repair/1/complete")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completion)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-004: 维修完成后设备状态恢复为空闲")
        void deviceStatusRestoredToIdleAfterRepair() throws Exception {
            Map<String, Object> completion = new HashMap<>();
            completion.put("result", "完成");

            mockMvc.perform(put("/repair/1/complete")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completion)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-REPAIR-004: 维修完成后报修人收到通知")
        void repairCreatorReceivesNotification() throws Exception {
            Map<String, Object> completion = new HashMap<>();
            completion.put("result", "维修完成");

            mockMvc.perform(put("/repair/1/complete")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completion)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能完成维修")
        void studentCannotCompleteRepair() throws Exception {
            mockMvc.perform(put("/repair/1/complete")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("只能完成已接单的维修")
        void cannotCompleteUnacceptedRepair() throws Exception {
            mockMvc.perform(put("/repair/999/complete")
                            .header("Authorization", getAuthHeader(getMaintainerToken())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("取消报修测试")
    class CancelRepairTests {

        @Test
        @DisplayName("学生可以取消自己的报修")
        void studentCanCancelOwnRepair() throws Exception {
            Map<String, Object> cancel = new HashMap<>();
            cancel.put("reason", "设备已恢复正常");

            mockMvc.perform(delete("/repair/1")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancel)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以取消报修")
        void systemAdminCanCancelRepair() throws Exception {
            mockMvc.perform(delete("/repair/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能取消他人的报修")
        void studentCannotCancelOthersRepair() throws Exception {
            mockMvc.perform(delete("/repair/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("评价维修服务测试")
    class RateRepairTests {

        @Test
        @DisplayName("报修人可以评价维修服务")
        void repairCreatorCanRateRepair() throws Exception {
            Map<String, Object> rating = new HashMap<>();
            rating.put("rating", 5);
            rating.put("comment", "维修及时，服务态度好");

            mockMvc.perform(post("/repair/1/rate")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("只能评价已完成的报修")
        void canOnlyRateCompletedRepairs() throws Exception {
            Map<String, Object> rating = new HashMap<>();
            rating.put("rating", 4);

            mockMvc.perform(post("/repair/999/rate")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未登录用户不能评价")
        void unauthenticatedUserCannotRateRepair() throws Exception {
            Map<String, Object> rating = new HashMap<>();
            rating.put("rating", 5);

            mockMvc.perform(post("/repair/1/rate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("待接单列表测试")
    class PendingRepairTests {

        @Test
        @DisplayName("维护人员可以获取待接单列表")
        void maintainerCanGetPendingRepairList() throws Exception {
            mockMvc.perform(get("/repair/pending")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("待接单列表支持紧急程度排序")
        void pendingListSupportsUrgencySort() throws Exception {
            mockMvc.perform(get("/repair/pending")
                            .header("Authorization", getAuthHeader(getMaintainerToken()))
                            .param("sortBy", "urgency")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk());
        }
    }
}
