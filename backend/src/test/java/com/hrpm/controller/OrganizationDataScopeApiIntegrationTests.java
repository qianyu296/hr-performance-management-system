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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class OrganizationDataScopeApiIntegrationTests {
    private static final long ALL_USER_ID = 9_700_001L;
    private static final long DEPT_TREE_USER_ID = 9_700_002L;
    private static final long SELF_USER_ID = 9_700_003L;
    private static final long AUTHORIZED_DEPARTMENT_ID = 97102L;
    private static final long AUTHORIZED_CHILD_DEPARTMENT_ID = 97103L;
    private static final long OUTSIDE_DEPARTMENT_ID = 97104L;
    private static final long UNAUTHORIZED_SIBLING_DEPARTMENT_ID = 97105L;
    private static final long DEPT_TREE_EMPLOYEE_ID = 97201L;
    private static final long AUTHORIZED_EMPLOYEE_ID = 97202L;
    private static final long OUTSIDE_EMPLOYEE_ID = 97203L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void seedOrganizationScopes() {
        String password = new BCryptPasswordEncoder().encode("password");
        insertOrganization();
        insertEmployees();
        insertUsers(password);
        insertRolesAndPermissions();
    }

    @Test
    void allScopedUserCanReadAllSeededOrganizationData() throws Exception {
        mockMvc.perform(get("/departments").header("Authorization", allToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_ROOT')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_OUTSIDE')]").isNotEmpty());

        mockMvc.perform(get("/employees")
                        .header("Authorization", allToken())
                        .param("keyword", "ORG_SCOPE_")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.employeeNo == 'ORG_SCOPE_TREE_MANAGER')]").isNotEmpty())
                .andExpect(jsonPath("$.data.records[?(@.employeeNo == 'ORG_SCOPE_AUTHORIZED')]").isNotEmpty())
                .andExpect(jsonPath("$.data.records[?(@.employeeNo == 'ORG_SCOPE_OUTSIDE')]").isNotEmpty());
    }

    @Test
    void departmentTreeScopedUserReadsOnlyAuthorizedDepartmentBranch() throws Exception {
        mockMvc.perform(get("/departments").header("Authorization", departmentTreeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_ROOT')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_ROOT')].children[?(@.code == 'ORG_SCOPE_AUTH')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_ROOT')].children[?(@.code == 'ORG_SCOPE_AUTH')].children[?(@.code == 'ORG_SCOPE_AUTH_CHILD')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_ROOT')].children[?(@.code == 'ORG_SCOPE_UNAUTHORIZED_CHILD')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'ORG_SCOPE_OUTSIDE')]").isEmpty());
    }

    @Test
    void departmentTreeScopedUserReadsOnlyEmployeesInAuthorizedDepartments() throws Exception {
        mockMvc.perform(get("/employees").header("Authorization", departmentTreeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[?(@.id == '97203')]").isEmpty());
    }

    @Test
    void selfScopedUserReadsOnlyItsOwnEmployeeRecord() throws Exception {
        mockMvc.perform(get("/employees").header("Authorization", selfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value("97203"));

        mockMvc.perform(get("/employees/{id}", OUTSIDE_EMPLOYEE_ID).header("Authorization", selfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("97203"));
        mockMvc.perform(get("/employees/{id}", AUTHORIZED_EMPLOYEE_ID).header("Authorization", selfToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void departmentTreeScopedUserCannotUpdateEmployeeOutsideScope() throws Exception {
        mockMvc.perform(patch("/employees/{id}", OUTSIDE_EMPLOYEE_ID)
                        .header("Authorization", departmentTreeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileUpdate("Outside employee")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void ordinaryProfileEndpointRejectsDirectEmploymentTransfer() throws Exception {
        mockMvc.perform(patch("/employees/{id}", AUTHORIZED_EMPLOYEE_ID)
                        .header("Authorization", departmentTreeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdate("Authorized employee", OUTSIDE_DEPARTMENT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void departmentTreeScopedUserCannotCreateEmployeeInOutsideDepartment() throws Exception {
        mockMvc.perform(post("/employees")
                        .header("Authorization", departmentTreeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"ORG_SCOPE_NEW_OUTSIDE","name":"Outside employee","departmentId":"97104","positionId":"97301","rankId":"97302","employmentStatus":"PROBATION","hireDate":"2026-01-01"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void departmentTreeScopedUserCannotAssignManagerOutsideScopeWhenCreatingEmployee() throws Exception {
        mockMvc.perform(post("/employees")
                        .header("Authorization", departmentTreeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"ORG_SCOPE_NEW_MANAGER","name":"New employee","departmentId":"97103","positionId":"97301","rankId":"97302","managerEmployeeId":"97203","employmentStatus":"PROBATION","hireDate":"2026-01-01"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    private void insertOrganization() {
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, path, effective_date, status, sort_no)
                VALUES (97101, 'ORG_SCOPE_ROOT', '范围根部门', '/97101/', '2026-01-01', 'ACTIVE', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, 'ORG_SCOPE_AUTH', '授权部门', 97101, '/97101/97102/', '2026-01-01', 'ACTIVE', 1)
                """, AUTHORIZED_DEPARTMENT_ID);
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, 'ORG_SCOPE_AUTH_CHILD', '授权子部门', ?, '/97101/97102/97103/', '2026-01-01', 'ACTIVE', 1)
                """, AUTHORIZED_CHILD_DEPARTMENT_ID, AUTHORIZED_DEPARTMENT_ID);
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, 'ORG_SCOPE_UNAUTHORIZED_CHILD', '未授权同级部门', 97101, '/97101/97105/', '2026-01-01', 'ACTIVE', 2)
                """, UNAUTHORIZED_SIBLING_DEPARTMENT_ID);
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, path, effective_date, status, sort_no)
                VALUES (?, 'ORG_SCOPE_OUTSIDE', '范围外部门', '/97104/', '2026-01-01', 'ACTIVE', 2)
                """, OUTSIDE_DEPARTMENT_ID);
        jdbcTemplate.update("""
                INSERT INTO hr_position (id, code, name, status)
                VALUES (97301, 'ORG_SCOPE_POSITION', '范围测试岗位', 'ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO hr_rank (id, code, name, rank_order, status)
                VALUES (97302, 'ORG_SCOPE_RANK', '范围测试职级', 1, 'ACTIVE')
                """);
    }

    private void insertEmployees() {
        insertEmployee(DEPT_TREE_EMPLOYEE_ID, "ORG_SCOPE_TREE_MANAGER", "范围主管", AUTHORIZED_DEPARTMENT_ID);
        insertEmployee(AUTHORIZED_EMPLOYEE_ID, "ORG_SCOPE_AUTHORIZED", "授权员工", AUTHORIZED_CHILD_DEPARTMENT_ID);
        insertEmployee(OUTSIDE_EMPLOYEE_ID, "ORG_SCOPE_OUTSIDE", "范围外员工", OUTSIDE_DEPARTMENT_ID);
    }

    private void insertEmployee(long id, String employeeNo, String name, long departmentId) {
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, employment_status, hire_date)
                VALUES (?, ?, ?, ?, 97301, 97302, 'FORMAL', '2025-01-01')
                """, id, employeeNo, name, departmentId);
    }

    private void insertUsers(String password) {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, 'org-scope-all-user', ?, 'ACTIVE', 1)
                """, ALL_USER_ID, password);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
                VALUES (?, 'org-scope-dept-tree-user', ?, ?, 'ACTIVE', 1)
                """, DEPT_TREE_USER_ID, password, DEPT_TREE_EMPLOYEE_ID);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
                VALUES (?, 'org-scope-self-user', ?, ?, 'ACTIVE', 1)
                """, SELF_USER_ID, password, OUTSIDE_EMPLOYEE_ID);
    }

    private void insertRolesAndPermissions() {
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (97401, '范围组织读取', 'org:read', 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE deleted = 0, status = 'ACTIVE'
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (97402, '范围组织管理', 'org:manage', 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE deleted = 0, status = 'ACTIVE'
                """);
        Long readMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = 'org:read' AND deleted = 0", Long.class);
        Long manageMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = 'org:manage' AND deleted = 0", Long.class);

        insertRole(97501L, "ORG_SCOPE_ALL", "全量范围测试", "ALL", ALL_USER_ID, readMenuId, manageMenuId);
        insertRole(97502L, "ORG_SCOPE_DEPT_TREE", "部门树范围测试", "DEPT_TREE", DEPT_TREE_USER_ID, readMenuId, manageMenuId);
        insertRole(97503L, "ORG_SCOPE_SELF", "本人范围测试", "SELF", SELF_USER_ID, readMenuId);
    }

    private void insertRole(long roleId, String code, String name, String scopeType, long userId, Long... menuIds) {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, ?, ?, 'ACTIVE')", roleId, code, name);
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (id, role_id, scope_type) VALUES (?, ?, ?)", roleId + 100, roleId, scopeType);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", roleId + 200, userId, roleId);
        for (int index = 0; index < menuIds.length; index++) {
            jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", roleId * 10 + index, roleId, menuIds[index]);
        }
    }

    private String employeeUpdate(String name, long departmentId) {
        return """
                {"name":"%s","gender":"MALE","departmentId":"%d","positionId":"97301","rankId":"97302","hireDate":"2025-01-01","version":"0"}
                """.formatted(name, departmentId);
    }

    private String profileUpdate(String name) {
        return "{\"name\":\"%s\",\"gender\":\"MALE\",\"version\":\"0\"}".formatted(name);
    }

    private String allToken() {
        return token(ALL_USER_ID, "org-scope-all-user");
    }

    private String departmentTreeToken() {
        return token(DEPT_TREE_USER_ID, "org-scope-dept-tree-user");
    }

    private String selfToken() {
        return token(SELF_USER_ID, "org-scope-self-user");
    }

    private String token(long userId, String username) {
        return "Bearer " + tokenService.issue(userId, username, 1);
    }
}
