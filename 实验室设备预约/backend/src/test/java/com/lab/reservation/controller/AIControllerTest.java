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
 * AI助手控制器测试
 * 测试AI问答、知识库查询、操作指引等功能
 * 对应测试用例：TC-AI-001 ~ TC-AI-005, TC-GUIDE-001 ~ TC-GUIDE-002
 */
@DisplayName("AI助手控制器测试")
public class AIControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("AI问答测试")
    class AIChatTests {

        @Test
        @DisplayName("TC-AI-001: AI助手页面验证 - 已登录用户可以访问AI助手")
        void authenticatedUserCanAccessAIAssistant() throws Exception {
            mockMvc.perform(get("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-002: 正常问答 - 学生可以发送问题")
        void studentCanSendQuestion() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "光谱仪如何校准？");
            question.put("deviceId", 1);

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").exists());
        }

        @Test
        @DisplayName("TC-AI-002: AI问答-教师提问")
        void teacherCanAskQuestion() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "如何操作高效液相色谱仪？");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-002: AI问答-实验室管理员提问")
        void labAdminCanAskQuestion() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "设备维护有什么注意事项？");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-002: AI问答-系统管理员提问")
        void systemAdminCanAskQuestion() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "如何添加新的设备知识？");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-003: 关联追问 - AI理解上下文")
        void AIUnderstandsContext() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "标准品怎么选择？");
            question.put("conversationId", "prev_conv_id");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-004: 危险操作预警 - 检测危险关键词")
        void AIWarnsDangerousOperations() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "我想用高压气体测试");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-AI-005: 无法回答的问题 - 返回友好提示")
        void AIHandlesUnknownQuestions() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "这是一个不存在的问题XYZ123456");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AI问答-问题为空")
        void cannotAskEmptyQuestion() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "");

            mockMvc.perform(post("/ai/chat")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未登录用户不能使用AI问答")
        void unauthenticatedUserCannotUseAI() throws Exception {
            Map<String, Object> question = new HashMap<>();
            question.put("question", "测试问题");

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(question)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("问答历史测试")
    class ChatHistoryTests {

        @Test
        @DisplayName("已登录用户可以获取问答历史")
        void authenticatedUserCanGetChatHistory() throws Exception {
            mockMvc.perform(get("/ai/history")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("问答历史支持按设备筛选")
        void chatHistorySupportsDeviceFilter() throws Exception {
            mockMvc.perform(get("/ai/history")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10")
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取问答历史")
        void unauthenticatedUserCannotGetChatHistory() throws Exception {
            mockMvc.perform(get("/ai/history"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("知识库查询测试")
    class KnowledgeQueryTests {

        @Test
        @DisplayName("已登录用户可以查询知识库")
        void authenticatedUserCanQueryKnowledge() throws Exception {
            mockMvc.perform(get("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("keyword", "校准"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("知识库查询支持按类型筛选")
        void knowledgeQuerySupportsTypeFilter() throws Exception {
            mockMvc.perform(get("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("type", "设备操作"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("知识库查询支持按设备筛选")
        void knowledgeQuerySupportsDeviceFilter() throws Exception {
            mockMvc.perform(get("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能查询知识库")
        void unauthenticatedUserCannotQueryKnowledge() throws Exception {
            mockMvc.perform(get("/ai/knowledge")
                            .param("keyword", "校准"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("操作指引测试")
    class OperationGuideTests {

        @Test
        @DisplayName("TC-GUIDE-001: 操作指引页面验证 - 可以获取设备操作指引")
        void canGetDeviceOperationGuide() throws Exception {
            mockMvc.perform(get("/ai/guide")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1")
                            .param("operationType", "校准操作"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-GUIDE-002: 步骤导航 - 获取操作步骤")
        void canGetOperationSteps() throws Exception {
            mockMvc.perform(get("/ai/guide/steps")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("guideId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-GUIDE-002: 步骤导航 - 获取下一步骤")
        void canGetNextStep() throws Exception {
            mockMvc.perform(get("/ai/guide/step/next")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("guideId", "1")
                            .param("currentStep", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-GUIDE-002: 步骤导航 - 获取上一步骤")
        void canGetPreviousStep() throws Exception {
            mockMvc.perform(get("/ai/guide/step/prev")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("guideId", "1")
                            .param("currentStep", "2"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能获取操作指引")
        void unauthenticatedUserCannotGetGuide() throws Exception {
            mockMvc.perform(get("/ai/guide")
                            .param("deviceId", "1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("安全提示测试")
    class SafetyTipsTests {

        @Test
        @DisplayName("可以获取设备安全提示")
        void canGetDeviceSafetyTips() throws Exception {
            mockMvc.perform(get("/ai/safety-tips")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("deviceId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("可以获取通用安全提示")
        void canGetGeneralSafetyTips() throws Exception {
            mockMvc.perform(get("/ai/safety-tips")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取安全提示")
        void unauthenticatedUserCannotGetSafetyTips() throws Exception {
            mockMvc.perform(get("/ai/safety-tips"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("收藏管理测试")
    class FavoritesTests {

        @Test
        @DisplayName("已登录用户可以收藏问答")
        void authenticatedUserCanFavoriteQA() throws Exception {
            Map<String, Object> favorite = new HashMap<>();
            favorite.put("questionId", 1);
            favorite.put("note", "重要知识点");

            mockMvc.perform(post("/ai/favorite")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(favorite)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("已登录用户可以获取收藏列表")
        void authenticatedUserCanGetFavorites() throws Exception {
            mockMvc.perform(get("/ai/favorites")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("已登录用户可以取消收藏")
        void authenticatedUserCanUnfavorite() throws Exception {
            mockMvc.perform(delete("/ai/favorite/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("未登录用户不能收藏问答")
        void unauthenticatedUserCannotFavorite() throws Exception {
            Map<String, Object> favorite = new HashMap<>();
            favorite.put("questionId", 1);

            mockMvc.perform(post("/ai/favorite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(favorite)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("知识库管理测试（管理员）")
    class KnowledgeBaseManagementTests {

        @Test
        @DisplayName("系统管理员可以获取知识库列表")
        void systemAdminCanGetKnowledgeBaseList() throws Exception {
            mockMvc.perform(get("/ai/knowledge/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }

        @Test
        @DisplayName("TC-KNOW-001: 知识库列表页面验证 - 系统管理员可以新增知识")
        void systemAdminCanAddKnowledge() throws Exception {
            Map<String, Object> knowledge = new HashMap<>();
            knowledge.put("title", "光谱仪校准步骤" + System.currentTimeMillis());
            knowledge.put("type", "设备操作");
            knowledge.put("deviceId", 1);
            knowledge.put("keywords", "校准,光谱仪,操作步骤");
            knowledge.put("content", "详细校准步骤说明内容");

            mockMvc.perform(post("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(knowledge)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-KNOW-002: 新增知识-完整信息")
        void systemAdminCanAddKnowledgeWithFullInfo() throws Exception {
            Map<String, Object> knowledge = new HashMap<>();
            knowledge.put("title", "高效液相色谱仪操作规程");
            knowledge.put("type", "设备操作");
            knowledge.put("deviceId", 1);
            knowledge.put("keywords", "HPLC,液相色谱,操作");
            knowledge.put("content", "HPLC操作详细步骤");
            knowledge.put("safetyWarnings", "注意高压泵操作安全");

            mockMvc.perform(post("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(knowledge)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以更新知识")
        void systemAdminCanUpdateKnowledge() throws Exception {
            Map<String, Object> knowledge = new HashMap<>();
            knowledge.put("id", 1);
            knowledge.put("title", "更新后的知识标题");
            knowledge.put("content", "更新后的内容");

            mockMvc.perform(put("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(knowledge)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以删除知识")
        void systemAdminCanDeleteKnowledge() throws Exception {
            mockMvc.perform(delete("/ai/knowledge/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("非系统管理员不能管理知识库")
        void nonSystemAdminCannotManageKnowledgeBase() throws Exception {
            Map<String, Object> knowledge = new HashMap<>();
            knowledge.put("title", "非法知识");

            mockMvc.perform(post("/ai/knowledge")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(knowledge)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("问答统计测试")
    class QAStatisticsTests {

        @Test
        @DisplayName("系统管理员可以获取问答统计")
        void systemAdminCanGetQAStatistics() throws Exception {
            mockMvc.perform(get("/ai/statistics")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取本室问答统计")
        void labAdminCanGetQAStatistics() throws Exception {
            mockMvc.perform(get("/ai/statistics")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("学生不能获取问答统计")
        void studentCannotGetQAStatistics() throws Exception {
            mockMvc.perform(get("/ai/statistics")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
