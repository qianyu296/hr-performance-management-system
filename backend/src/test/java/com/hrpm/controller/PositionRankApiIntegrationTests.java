package com.hrpm.controller;

import com.hrpm.service.TokenService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class PositionRankApiIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TokenService tokenService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM hr_position WHERE code LIKE 'TEST_%'");
        jdbcTemplate.update("DELETE FROM hr_rank WHERE code LIKE 'TEST_%'");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = 98001");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = 98002");
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = 98002");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = 98001");
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, status, session_version) VALUES (98001, 'org-admin', ?, 'ACTIVE', 1)",
                new BCryptPasswordEncoder().encode("password"));
    }

    @Test
    void organizationManagerCanCreateListAndUpdatePosition() throws Exception {
        grantPermissions();
        mockMvc.perform(post("/positions").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"TEST_ENGINEER","name":"Engineer","jobFamily":"Engineering","description":"IC","sortNo":10,"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("0"));

        String id = jdbcTemplate.queryForObject("SELECT CAST(id AS CHAR) FROM hr_position WHERE code='TEST_ENGINEER'", String.class);
        mockMvc.perform(get("/positions").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code", hasItem("TEST_ENGINEER")));
        mockMvc.perform(patch("/positions/{id}", id).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Senior Engineer","jobFamily":"Engineering","description":"Senior IC","sortNo":5,"status":"ACTIVE","version":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Senior Engineer"))
                .andExpect(jsonPath("$.data.version").value("1"));
    }

    @Test
    void organizationManagerCanCreateListAndUpdateRank() throws Exception {
        grantPermissions();
        mockMvc.perform(post("/ranks").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"TEST_P5","name":"P5","rankOrder":5,"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk());
        String id = jdbcTemplate.queryForObject("SELECT CAST(id AS CHAR) FROM hr_rank WHERE code='TEST_P5'", String.class);
        mockMvc.perform(get("/ranks").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code", hasItem("TEST_P5")));
        mockMvc.perform(patch("/ranks/{id}", id).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"name\":\"P5 Senior\",\"rankOrder\":6,\"status\":\"ACTIVE\",\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("1"));
    }

    @Test
    void stalePositionVersionReturnsConflict() throws Exception {
        grantPermissions();
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status, version) VALUES (98101, 'TEST_STALE', 'Stale', 'ACTIVE', 2)");
        mockMvc.perform(patch("/positions/98101").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Changed","sortNo":0,"status":"ACTIVE","version":"1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void userWithoutPermissionCannotReadPositions() throws Exception {
        mockMvc.perform(get("/positions").header("Authorization", token()))
                .andExpect(status().isForbidden());
    }

    private void grantPermissions() {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (98002, 'TEST_ORG', 'Test Org', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (98005, 98001, 98002)");
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (98903, 'Org read', 'org:read', 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (98904, 'Org manage', 'org:manage', 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE name = VALUES(name)
                """);
        Long readMenuId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:read' AND deleted=0", Long.class);
        Long manageMenuId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:manage' AND deleted=0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (98006, 98002, ?), (98007, 98002, ?)", readMenuId, manageMenuId);
    }

    private String token() {
        return "Bearer " + tokenService.issue(98001L, "org-admin", 1);
    }
}
