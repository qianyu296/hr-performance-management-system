package com.hrpm.controller;

import com.hrpm.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'emp_test_%')");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'emp_test_%'");
        jdbcTemplate.update("DELETE FROM hr_employee WHERE employee_no LIKE 'EMP_TEST_%'");
        jdbcTemplate.update("DELETE FROM hr_rank WHERE code='EMP_TEST_RANK'");
        jdbcTemplate.update("DELETE FROM hr_position WHERE code='EMP_TEST_POSITION'");
        jdbcTemplate.update("DELETE FROM hr_department WHERE code='EMP_TEST_DEPT'");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id=99001");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id=99002");
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id=99002");
        jdbcTemplate.update("DELETE FROM sys_role WHERE id=99002");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id=99001");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id=99011");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id=99012");
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id=99012");
        jdbcTemplate.update("DELETE FROM sys_role WHERE id=99012");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id=99011");
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (99101,'EMP_TEST_DEPT','测试部门','/99101/','2026-01-01','ACTIVE')");
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (99102,'EMP_TEST_POSITION','测试岗位','ACTIVE')");
        jdbcTemplate.update("INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES (99103,'EMP_TEST_RANK','五级职级',5,'ACTIVE')");
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
                .andExpect(jsonPath("$.data.records[0].departmentName").value("测试部门"));

        mockMvc.perform(get("/employees/99201").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.version").value("0"));
    }

    @Test
    void organizationManagerCanCreateEmployeeAndProvisionSelfServiceAccount() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status)
                VALUES (99104, 'EMP_TEST_ANNUAL', '员工测试年假', 1, 1.00, 80.00, 'ACTIVE')
                ON DUPLICATE KEY UPDATE annual_quota = VALUES(annual_quota), status = 'ACTIVE', deleted = 0
                """);
        String response = mockMvc.perform(post("/employees").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"EMP_TEST_003","name":"Carol Wu","gender":"FEMALE","departmentId":"99101","positionId":"99102","rankId":"99103","employmentStatus":"PROBATION","hireDate":"2026-07-13"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employee.employeeNo").value("EMP_TEST_003"))
                .andExpect(jsonPath("$.data.initialUsername").value("emp_test_003"))
                .andExpect(jsonPath("$.data.initialPassword").value("123456"))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM att_leave_balance b
                JOIN hr_employee e ON e.id = b.employee_id
                WHERE e.employee_no = 'EMP_TEST_003' AND b.balance_type = 'EMP_TEST_ANNUAL'
                  AND b.balance_year = ? AND b.available_hours = 80.00 AND b.deleted = 0
                """, Integer.class, java.time.LocalDate.now().getYear()));

        String password = objectMapper.readTree(response).at("/data/initialPassword").asText();
        String loginResponse = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"emp_test_003\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).at("/data/accessToken").asText();
        mockMvc.perform(get("/me/permissions").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        String changedResponse = mockMvc.perform(post("/auth/change-password").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + password + "\",\"newPassword\":\"EmployeeNewPassword!2026\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
                .andReturn().getResponse().getContentAsString();
        String changedToken = objectMapper.readTree(changedResponse).at("/data/accessToken").asText();
        mockMvc.perform(get("/me/permissions").header("Authorization", "Bearer " + changedToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@ == 'attendance:submit')]").isNotEmpty());
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
    void ordinaryProfileUpdateDoesNotChangeEmploymentStatus() throws Exception {
        seedEmployee(99205L, "EMP_TEST_005", "Before", "FORMAL", 0);
        mockMvc.perform(patch("/employees/99205").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"After","gender":"MALE","version":"0"}
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
                                {"name":"Changed","version":"1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void ordinaryProfileUpdateRejectsEmploymentFields() throws Exception {
        seedEmployee(99207L, "EMP_TEST_007", "Protected", "FORMAL", 0);
        for (String payload : new String[] {
                "{\"name\":\"Protected\",\"version\":\"0\",\"departmentId\":\"99101\"}",
                "{\"name\":\"Protected\",\"version\":\"0\",\"positionId\":\"99102\"}",
                "{\"name\":\"Protected\",\"version\":\"0\",\"rankId\":\"99103\"}",
                "{\"name\":\"Protected\",\"version\":\"0\",\"managerEmployeeId\":\"99205\"}",
                "{\"name\":\"Protected\",\"version\":\"0\",\"employmentStatus\":\"SUSPENDED\"}"
        }) {
            mockMvc.perform(patch("/employees/99207").header("Authorization", token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void createRejectsUnknownEmploymentStatus() throws Exception {
        mockMvc.perform(post("/employees").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"EMP_TEST_008","name":"Invalid status","departmentId":"99101","positionId":"99102","employmentStatus":"UNKNOWN","hireDate":"2026-07-13"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void selfScopeCannotEnumerateOrReadAnotherEmployeeById() throws Exception {
        seedEmployee(99210L, "EMP_TEST_SELF", "Self User", "FORMAL", 0);
        seedEmployee(99211L, "EMP_TEST_OTHER", "Other User", "FORMAL", 0);
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version) VALUES (99011, 'self-scope-user', ?, 99210, 'ACTIVE', 1)", new BCryptPasswordEncoder().encode("password"));
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (99012, 'EMP_TEST_SELF', '员工本人数据测试', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO sys_user_role (id,user_id,role_id) VALUES (99013,99011,99012)");
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (id,role_id,scope_type) VALUES (99014,99012,'SELF')");
        jdbcTemplate.update("INSERT INTO sys_role_menu (id,role_id,menu_id) SELECT 99015,99012,id FROM sys_menu WHERE permission_code='org:read' AND deleted=0 LIMIT 1");

        String selfToken = "Bearer " + tokenService.issue(99011L, "self-scope-user", 1);
        mockMvc.perform(get("/employees").header("Authorization", selfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value("99210"));
        mockMvc.perform(get("/employees/99211").header("Authorization", selfToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/employees/99210").header("Authorization", selfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Self User"));
    }

    private void seedEmployee(long id, String no, String name, String status, int version) {
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, employment_status, hire_date, version)
                VALUES (?, ?, ?, 99101, 99102, 99103, ?, '2025-01-01', ?)
                """, id, no, name, status, version);
    }

    private void grantPermissions() {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (99002,'EMP_TEST_ORG','员工组织测试','ACTIVE')");
        jdbcTemplate.update("INSERT INTO sys_user_role (id,user_id,role_id) VALUES (99005,99001,99002)");
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (id,role_id,scope_type) VALUES (99008,99002,'ALL')");
        jdbcTemplate.update("INSERT INTO sys_menu (id,name,permission_code,menu_type,status) VALUES (99903,'组织读取','org:read','BUTTON','ACTIVE') ON DUPLICATE KEY UPDATE name=VALUES(name)");
        jdbcTemplate.update("INSERT INTO sys_menu (id,name,permission_code,menu_type,status) VALUES (99904,'组织管理','org:manage','BUTTON','ACTIVE') ON DUPLICATE KEY UPDATE name=VALUES(name)");
        Long readId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:read' AND deleted=0", Long.class);
        Long manageId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code='org:manage' AND deleted=0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id,role_id,menu_id) VALUES (99006,99002,?),(99007,99002,?)", readId, manageId);
    }

    private String token() {
        return "Bearer " + tokenService.issue(99001L, "employee-admin", 1);
    }
}
