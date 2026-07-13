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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class EmployeeApiIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TokenService tokenService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM hr_employee WHERE employee_no LIKE 'EMP_TEST_%'");
        jdbcTemplate.update("DELETE FROM hr_rank WHERE code='EMP_TEST_RANK'");
        jdbcTemplate.update("DELETE FROM hr_position WHERE code='EMP_TEST_POSITION'");
        jdbcTemplate.update("DELETE FROM hr_department WHERE code='EMP_TEST_DEPT'");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id=99001");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id=99002");
        jdbcTemplate.update("DELETE FROM sys_role WHERE id=99002");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id=99001");
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (99101,'EMP_TEST_DEPT','Test Department','/99101/','2026-01-01','ACTIVE')");
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (99102,'EMP_TEST_POSITION','Test Position','ACTIVE')");
        jdbcTemplate.update("INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES (99103,'EMP_TEST_RANK','P5',5,'ACTIVE')");
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, status, session_version) VALUES (99001,'employee-admin',?,'ACTIVE',1)",
                new BCryptPasswordEncoder().encode("password"));
        grantPermissions();
    }

    @Test
    void authorizedUserCanFilterAndReadEmployeeDirectory() throws Exception {
        seedEmployee(99201L, "EMP_TEST_001", "Alice Zhang", "FORMAL", 0);
        seedEmployee(99202L, "EMP_TEST_002", "Bob Li", "PROBATION", 0);

        mockMvc.perform(get("/employees")
                        .header("Authorization", token())
                        .param("page", "1").param("pageSize", "20")
                        .param("keyword", "Alice").param("departmentId", "99101")
                        .param("employmentStatus", "FORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].employeeNo").value("EMP_TEST_001"))
                .andExpect(jsonPath("$.data.records[0].departmentName").value("Test Department"));

        mockMvc.perform(get("/employees/99201").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.version").value("0"));
    }

    @Test
    void organizationManagerCanCreateEmployee() throws Exception {
        mockMvc.perform(post("/employees").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"EMP_TEST_003","name":"Carol Wu","gender":"FEMALE","departmentId":"99101","positionId":"99102","rankId":"99103","employmentStatus":"PROBATION","hireDate":"2026-07-13"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("EMP_TEST_003"))
                .andExpect(jsonPath("$.data.version").value("0"));
    }

    @Test
    void invalidDepartmentIsRejected() throws Exception {
        mockMvc.perform(post("/employees").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"EMP_TEST_004","name":"Invalid","departmentId":"999999","positionId":"99102","employmentStatus":"FORMAL","hireDate":"2026-07-13"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void ordinaryUpdateDoesNotChangeEmploymentStatus() throws Exception {
        seedEmployee(99205L, "EMP_TEST_005", "Before", "FORMAL", 0);
        mockMvc.perform(patch("/employees/99205").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"After","gender":"MALE","departmentId":"99101","positionId":"99102","rankId":"99103","hireDate":"2025-01-01","version":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("After"))
                .andExpect(jsonPath("$.data.employmentStatus").value("FORMAL"))
                .andExpect(jsonPath("$.data.version").value("1"));
    }

    @Test
    void staleEmployeeVersionReturnsConflict() throws Exception {
        seedEmployee(99206L, "EMP_TEST_006", "Concurrent", "FORMAL", 2);
        mockMvc.perform(patch("/employees/99206").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Changed","departmentId":"99101","positionId":"99102","hireDate":"2025-01-01","version":"1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    private void seedEmployee(long id, String no, String name, String status, int version) {
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, employment_status, hire_date, version)
                VALUES (?, ?, ?, 99101, 99102, 99103, ?, '2025-01-01', ?)
                """, id, no, name, status, version);
    }

    private void grantPermissions() {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (99002,'EMP_TEST_ORG','Employee Test Org','ACTIVE')");
        jdbcTemplate.update("INSERT INTO sys_user_role (id,user_id,role_id) VALUES (99005,99001,99002)");
        jdbcTemplate.update("INSERT INTO sys_menu (id,name,permission_code,menu_type,status) VALUES (99903,'Org read','org:read','BUTTON','ACTIVE') ON DUPLICATE KEY UPDATE name=VALUES(name)");
        jdbcTemplate.update("INSERT INTO sys_menu (id,name,permission_code,menu_type,status) VALUES (99904,'Org manage','org:manage','BUTTON','ACTIVE') ON DUPLICATE KEY UPDATE name=VALUES(name)");
        Long readId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:read' AND deleted=0", Long.class);
        Long manageId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:manage' AND deleted=0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id,role_id,menu_id) VALUES (99006,99002,?),(99007,99002,?)", readId, manageId);
    }

    private String token() {
        return "Bearer " + tokenService.issue(99001L, "employee-admin", 1);
    }
}
