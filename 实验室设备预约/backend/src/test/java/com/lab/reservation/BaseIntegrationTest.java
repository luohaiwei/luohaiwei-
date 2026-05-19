package com.lab.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 集成测试基类
 * 提供MockMvc和认证令牌支持
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private String studentToken;
    private String teacherToken;
    private String labAdminToken;
    private String systemAdminToken;
    private String maintainerToken;

    @BeforeEach
    void setUpAuthTokens() {
        this.studentToken = loginAndGetToken("student", "123456");
        this.teacherToken = loginAndGetToken("teacher", "123456");
        this.labAdminToken = loginAndGetToken("lab_admin", "123456");
        this.systemAdminToken = loginAndGetToken("system_admin", "123456");
        this.maintainerToken = loginAndGetToken("maintainer", "123456");
    }

    private String loginAndGetToken(String username, String password) {
        try {
            String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            MvcResult result = mockMvc.perform(post("/user/login")
                            .contentType("application/json")
                            .content(json))
                    .andReturn();
            String responseBody = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("token")) {
                return node.get("token").asText();
            }
        } catch (Exception e) {
            // 登录失败
        }
        return null;
    }

    protected String getStudentToken() {
        return studentToken;
    }

    protected String getTeacherToken() {
        return teacherToken;
    }

    protected String getLabAdminToken() {
        return labAdminToken;
    }

    protected String getSystemAdminToken() {
        return systemAdminToken;
    }

    protected String getMaintainerToken() {
        return maintainerToken;
    }

    protected String getAuthHeader(String token) {
        return token != null ? "Bearer " + token : "";
    }
}
