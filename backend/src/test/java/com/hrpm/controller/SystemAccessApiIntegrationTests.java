package com.hrpm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class SystemAccessApiIntegrationTests {
    private static final long ADMIN_USER_ID = 87001L;
    private static final long TARGET_USER_ID = 87002L;
    private static final long ADMIN_ROLE_ID = 87011L;
    private static final long INITIAL_ROLE_ID = 87012L;
    private static final long REPLACEMENT_ROLE_ID = 87013L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    private long systemManageMenuId;

    @BeforeEach
    void setUp() {
        clearFixtures();

        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, status, session_version, version) VALUES (?, 'system-access-admin', 'unused', 'ACTIVE', 0, 0)", ADMIN_USER_ID);
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, status, session_version, version) VALUES (?, 'system-access-target', 'unused', 'ACTIVE', 0, 0)", TARGET_USER_ID);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status, version) VALUES (?, 'SYSTEM_ACCESS_ADMIN', '权限管理测试管理员', 'ACTIVE', 0)", ADMIN_ROLE_ID);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status, version) VALUES (?, 'SYSTEM_ACCESS_INITIAL', '初始测试角色', 'ACTIVE', 0)", INITIAL_ROLE_ID);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status, version) VALUES (?, 'SYSTEM_ACCESS_REPLACEMENT', '替换测试角色', 'ACTIVE', 0)", REPLACEMENT_ROLE_ID);
        systemManageMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = 'system:manage' AND deleted = 0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 87031L, ADMIN_ROLE_ID, systemManageMenuId);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 87041L, ADMIN_USER_ID, ADMIN_ROLE_ID);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 87042L, TARGET_USER_ID, INITIAL_ROLE_ID);
    }

    @AfterEach
    void tearDown() {
        clearFixtures();
    }

    private void clearFixtures() {
        jdbcTemplate.update("DELETE d FROM sys_data_scope_dept d JOIN sys_role_data_scope s ON s.scope_id = d.scope_id WHERE s.role_id IN (?, ?, ?)",
                ADMIN_ROLE_ID, INITIAL_ROLE_ID, REPLACEMENT_ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id IN (?, ?, ?)",
                ADMIN_ROLE_ID, INITIAL_ROLE_ID, REPLACEMENT_ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN (?, ?, ?)", ADMIN_ROLE_ID, INITIAL_ROLE_ID, REPLACEMENT_ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (?, ?)", ADMIN_USER_ID, TARGET_USER_ID);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id IN (?, ?, ?)", ADMIN_ROLE_ID, INITIAL_ROLE_ID, REPLACEMENT_ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id IN (?, ?)", ADMIN_USER_ID, TARGET_USER_ID);
    }

    @Test
    void administratorCanUpdateRolePermissionsWhenHistoricalDeletedMenuBindingExists() throws Exception {
        String adminToken = tokenService.issueAccess(ADMIN_USER_ID, "system-access-admin", 0);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id, deleted, version) VALUES (?, ?, ?, 1, 0)",
                87032L, ADMIN_ROLE_ID, systemManageMenuId);

        mockMvc.perform(put("/system/roles/{id}", ADMIN_ROLE_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"权限管理测试管理员\"," +
                                "\"status\":\"ACTIVE\"," +
                                "\"dataScopeType\":\"ALL\"," +
                                "\"menuIds\":[\"" + systemManageMenuId + "\"]," +
                                "\"departmentIds\":[]," +
                                "\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(Long.toString(ADMIN_ROLE_ID)))
                .andExpect(jsonPath("$.data.menuIds[0]").value(Long.toString(systemManageMenuId)))
                .andExpect(jsonPath("$.data.version").value("1"));

        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ?", Integer.class, ADMIN_ROLE_ID));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id = ? AND deleted = 0",
                Integer.class, ADMIN_ROLE_ID, systemManageMenuId));
    }

    @Test
    void administratorCanReplaceRolesAndInvalidateTargetSession() throws Exception {
        String adminToken = tokenService.issueAccess(ADMIN_USER_ID, "system-access-admin", 0);
        String targetToken = tokenService.issueAccess(TARGET_USER_ID, "system-access-target", 0);

        mockMvc.perform(get("/system/roles").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());

        mockMvc.perform(put("/system/users/{id}/roles", TARGET_USER_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[\"" + REPLACEMENT_ROLE_ID + "\"],\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleIds[0]").value(Long.toString(REPLACEMENT_ROLE_ID)))
                .andExpect(jsonPath("$.data.version").value("1"));

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_INVALID"));
    }
}