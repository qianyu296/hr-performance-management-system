package com.hrpm.controller;


import com.hrpm.entity.Department;
import com.hrpm.service.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class DepartmentApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDepartments() {
        jdbcTemplate.update("DELETE FROM hr_department");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", 90001L);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 90002L);
        jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", 90003L);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 90002L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 90001L);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, ?, ?, 'ACTIVE', ?)
                """, 90001L, "department-test-admin", new BCryptPasswordEncoder().encode("correct-password"), 2);
    }

    @Test
    void authenticatedUserCanCreateRootAndChildDepartmentsWithMaterializedPath() throws Exception {
        grantOrganizationManagementPermission();
        MvcResult rootResult = mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "create-root-Department-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"HQ","name":"总部","status":"ACTIVE","effectiveDate":"2026-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.path").isNotEmpty())
                .andReturn();
        JsonNode root = objectMapper.readTree(rootResult.getResponse().getContentAsString()).path("data");

        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "create-child-Department-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ENG","name":"研发部","parentId":"%s","status":"ACTIVE","effectiveDate":"2026-01-01"}
                                """.formatted(root.path("id").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.path", startsWith(root.path("path").asText())));
    }

    @Test
    void unauthenticatedCallerCannotCreateDepartment() throws Exception {
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HQ\",\"name\":\"总部\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedCallerWithoutOrganizationPermissionCannotCreateDepartment() throws Exception {
        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "unprivileged-Department-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HQ\",\"name\":\"总部\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedUserCanReadDepartmentTree() throws Exception {
        grantOrganizationReadPermission();
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, path, effective_date, status, sort_no)
                VALUES (?, 'HQ', '总部', '/91001/', '2026-01-01', 'ACTIVE', 1)
                """, 91001L);
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, 'ENG', '研发部', ?, '/91001/91002/', '2026-01-01', 'ACTIVE', 1)
                """, 91002L, 91001L);

        mockMvc.perform(get("/departments")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("HQ"))
                .andExpect(jsonPath("$.data[0].children[0].code").value("ENG"));
    }

    @Test
    void userWithoutOrganizationReadPermissionCannotReadDepartmentTree() throws Exception {
        mockMvc.perform(get("/departments")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isForbidden());
    }

    private String bearerToken() {
        return "Bearer " + tokenService.issue(90001L, "department-test-admin", 2);
    }

    private void grantOrganizationManagementPermission() {
        grantPermission("org:manage", "组织管理");
    }

    private void grantOrganizationReadPermission() {
        grantPermission("org:read", "组织查看");
    }

    private void grantPermission(String permissionCode, String permissionName) {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, ?, ?, 'ACTIVE')", 90002L, "TEST_ORG_MANAGER", "组织管理员测试");
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (?, ?, ?, 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """, 90003L, permissionName, permissionCode);
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = ? AND deleted = 0", Long.class, permissionCode);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 90004L, 90001L, 90002L);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 90005L, 90002L, menuId);
    }
}
