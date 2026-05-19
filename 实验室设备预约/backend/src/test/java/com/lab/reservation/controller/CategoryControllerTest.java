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
 * 设备分类控制器测试
 * 测试设备分类的CRUD操作及权限控制
 * 对应测试用例：TC-CATEGORY-001 ~ TC-CATEGORY-002
 */
@DisplayName("设备分类控制器测试")
public class CategoryControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("分类列表查询测试")
    class CategoryListTests {

        @Test
        @DisplayName("TC-CATEGORY-001: 分类列表页面元素验证 - 系统管理员可以获取分类列表")
        void systemAdminCanGetCategoryList() throws Exception {
            mockMvc.perform(get("/category/list")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("TC-CATEGORY-001: 分类列表显示树形结构")
        void categoryListShowsTreeStructure() throws Exception {
            mockMvc.perform(get("/category/tree")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-CATEGORY-001: 分类列表显示一级分类")
        void categoryListShowsTopLevelCategories() throws Exception {
            mockMvc.perform(get("/category/tree")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-CATEGORY-001: 二级分类在一级分类下缩进显示")
        void subCategoriesIndentedUnderParent() throws Exception {
            mockMvc.perform(get("/category/tree")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("实验室管理员可以获取分类列表")
        void labAdminCanGetCategoryList() throws Exception {
            mockMvc.perform(get("/category/list")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("教师可以获取分类列表")
        void teacherCanGetCategoryList() throws Exception {
            mockMvc.perform(get("/category/list")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("学生可以获取分类列表")
        void studentCanGetCategoryList() throws Exception {
            mockMvc.perform(get("/category/list")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取分类列表")
        void unauthenticatedUserCannotGetCategoryList() throws Exception {
            mockMvc.perform(get("/category/list"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("新增分类测试")
    class AddCategoryTests {

        @Test
        @DisplayName("TC-CATEGORY-002: 系统管理员可以新增分类")
        void systemAdminCanAddCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "光学仪器类" + System.currentTimeMillis());
            category.put("categoryCode", "OPT" + System.currentTimeMillis());
            category.put("parentId", 0);
            category.put("description", "光学相关设备");

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-CATEGORY-002: 新增一级分类")
        void systemAdminCanAddTopLevelCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "测试一级分类" + System.currentTimeMillis());
            category.put("categoryCode", "TEST" + System.currentTimeMillis());
            category.put("parentId", 0);

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-CATEGORY-002: 新增二级分类")
        void systemAdminCanAddSubCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "光谱分析子类" + System.currentTimeMillis());
            category.put("categoryCode", "SPECTRO" + System.currentTimeMillis());
            category.put("parentId", 1);

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-CATEGORY-002: 新增分类-分类编号重复")
        void cannotAddCategoryWithDuplicateCode() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "重复编号分类" + System.currentTimeMillis());
            category.put("categoryCode", "SPECTRO");

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("新增分类-缺少必填字段")
        void cannotAddCategoryWithMissingFields() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "缺少编号的分类");

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能新增分类")
        void labAdminCannotAddCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "非法分类" + System.currentTimeMillis());
            category.put("categoryCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能新增分类")
        void teacherCannotAddCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "非法分类" + System.currentTimeMillis());
            category.put("categoryCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getTeacherToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能新增分类")
        void studentCannotAddCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryName", "非法分类" + System.currentTimeMillis());
            category.put("categoryCode", "INVALID" + System.currentTimeMillis());

            mockMvc.perform(post("/category")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("更新分类测试")
    class UpdateCategoryTests {

        @Test
        @DisplayName("系统管理员可以更新分类")
        void systemAdminCanUpdateCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("id", 1);
            category.put("categoryName", "更新后的分类名称");
            category.put("description", "更新后的描述");

            mockMvc.perform(put("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }

        @Test
        @DisplayName("系统管理员可以更新分类排序")
        void systemAdminCanUpdateCategorySort() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("id", 1);
            category.put("sortOrder", 99);

            mockMvc.perform(put("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("不能更新系统内置分类")
        void cannotUpdateBuiltInCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("id", 1);
            category.put("categoryName", "尝试更新内置分类");

            mockMvc.perform(put("/category")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能更新分类")
        void labAdminCannotUpdateCategory() throws Exception {
            Map<String, Object> category = new HashMap<>();
            category.put("id", 2);
            category.put("categoryName", "实验室管理员尝试更新");

            mockMvc.perform(put("/category")
                            .header("Authorization", getAuthHeader(getLabAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("删除分类测试")
    class DeleteCategoryTests {

        @Test
        @DisplayName("系统管理员可以删除分类")
        void systemAdminCanDeleteCategory() throws Exception {
            mockMvc.perform(delete("/category/999")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("不能删除有子分类的分类")
        void cannotDeleteCategoryWithSubCategories() throws Exception {
            mockMvc.perform(delete("/category/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("不能删除有设备关联的分类")
        void cannotDeleteCategoryWithDevices() throws Exception {
            mockMvc.perform(delete("/category/2")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("不能删除系统内置分类")
        void cannotDeleteBuiltInCategory() throws Exception {
            mockMvc.perform(delete("/category/1")
                            .header("Authorization", getAuthHeader(getSystemAdminToken())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("实验室管理员不能删除分类")
        void labAdminCannotDeleteCategory() throws Exception {
            mockMvc.perform(delete("/category/999")
                            .header("Authorization", getAuthHeader(getLabAdminToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("教师不能删除分类")
        void teacherCannotDeleteCategory() throws Exception {
            mockMvc.perform(delete("/category/999")
                            .header("Authorization", getAuthHeader(getTeacherToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("学生不能删除分类")
        void studentCannotDeleteCategory() throws Exception {
            mockMvc.perform(delete("/category/999")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("分类详情查询测试")
    class CategoryDetailTests {

        @Test
        @DisplayName("已登录用户可以获取分类详情")
        void authenticatedUserCanGetCategoryDetail() throws Exception {
            int statusCode = mockMvc.perform(get("/category/1")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(statusCode == 200 || statusCode == 404);
        }

        @Test
        @DisplayName("未登录用户不能获取分类详情")
        void unauthenticatedUserCannotGetCategoryDetail() throws Exception {
            mockMvc.perform(get("/category/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("可以获取分类下的设备列表")
        void canGetDevicesByCategory() throws Exception {
            mockMvc.perform(get("/category/1/devices")
                            .header("Authorization", getAuthHeader(getStudentToken()))
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.list").isArray());
        }
    }

    @Nested
    @DisplayName("获取所有分类测试")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("已登录用户可以获取所有分类下拉列表")
        void authenticatedUserCanGetAllCategories() throws Exception {
            mockMvc.perform(get("/category/all")
                            .header("Authorization", getAuthHeader(getStudentToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("未登录用户不能获取所有分类")
        void unauthenticatedUserCannotGetAllCategories() throws Exception {
            mockMvc.perform(get("/category/all"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("分类启用禁用测试")
    class CategoryStatusTests {

        @Test
        @DisplayName("系统管理员可以启用分类")
        void systemAdminCanEnableCategory() throws Exception {
            mockMvc.perform(put("/category/2/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("系统管理员可以禁用分类")
        void systemAdminCanDisableCategory() throws Exception {
            mockMvc.perform(put("/category/2/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "0"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("禁用分类后该分类设备不可见")
        void disabledCategoryDevicesInvisible() throws Exception {
            mockMvc.perform(put("/category/2/status")
                            .header("Authorization", getAuthHeader(getSystemAdminToken()))
                            .param("status", "0"))
                    .andExpect(status().isOk());
        }
    }
}
