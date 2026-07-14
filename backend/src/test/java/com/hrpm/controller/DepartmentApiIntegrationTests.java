package com.hrpm.controller;


import com.hrpm.entity.Department;
import com.hrpm.service.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        jdbcTemplate.update("DELETE FROM sys_data_scope_dept WHERE scope_id = ?", 90014L);
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id = ?", 90012L);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 90012L);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ? AND role_id = ?", 90001L, 90012L);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 90012L);
        jdbcTemplate.update("DELETE FROM sys_data_scope WHERE id = ?", 90014L);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", 90001L);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 90002L);
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id = ?", 90002L);
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

    @Test
    void updateRejectsStaleDepartmentVersion() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92001L, "VERSIONED", "Versioned", null, "/92001/", "ACTIVE");

        mockMvc.perform(patch("/departments/{id}", 92001L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","sortNo":1,"status":"ACTIVE","effectiveDate":"2026-01-01","version":"7"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void moveRejectsTargetParentInsideDepartmentSubtree() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92011L, "MOVE_ROOT", "Move root", null, "/92011/", "ACTIVE");
        insertDepartment(92012L, "MOVE_CHILD", "Move child", 92011L, "/92011/92012/", "ACTIVE");

        mockMvc.perform(post("/departments/{id}/move", 92011L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"parentId\":\"92012\",\"version\":\"0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void disableRejectsDepartmentWithActiveEmployeeOrActiveChild() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92021L, "DISABLE_PARENT", "Disable parent", null, "/92021/", "ACTIVE");
        insertDepartment(92022L, "DISABLE_CHILD", "Disable child", 92021L, "/92021/92022/", "ACTIVE");

        mockMvc.perform(post("/departments/{id}/disable", 92021L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        jdbcTemplate.update("UPDATE hr_department SET status = 'INACTIVE' WHERE id = ?", 92022L);
        insertActiveEmployee(92023L, "DEPT_DISABLE_EMPLOYEE", 92021L);

        mockMvc.perform(post("/departments/{id}/disable", 92021L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void disableMarksDepartmentInactiveWhenNoActiveReferencesRemain() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92031L, "DISABLE_OK", "Disable ok", null, "/92031/", "ACTIVE");

        mockMvc.perform(post("/departments/{id}/disable", 92031L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void moveUpdatesEveryDescendantPath() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92041L, "MOVE_TARGET", "Move target", null, "/92041/", "ACTIVE");
        insertDepartment(92042L, "MOVE_SOURCE", "Move source", null, "/92042/", "ACTIVE");
        insertDepartment(92043L, "MOVE_DESCENDANT", "Move descendant", 92042L, "/92042/92043/", "ACTIVE");

        mockMvc.perform(post("/departments/{id}/move", 92042L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"92041\",\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.path").value("/92041/92042/"));

        org.junit.jupiter.api.Assertions.assertEquals("/92041/92042/92043/",
                jdbcTemplate.queryForObject("SELECT path FROM hr_department WHERE id = ?", String.class, 92043L));
    }

    @Test
    void moveRejectsWhenAnyAffectedDescendantIsOutsideWritableScope() throws Exception {
        insertDepartment(92051L, "SCOPE_MOVE_SOURCE", "Scope move source", null, "/92051/", "ACTIVE");
        insertDepartment(92052L, "SCOPE_MOVE_CHILD", "Scope move child", 92051L, "/92051/92052/", "ACTIVE");
        insertDepartment(92053L, "SCOPE_MOVE_TARGET", "Scope move target", null, "/92053/", "ACTIVE");
        grantCustomOrganizationManagementPermission(92051L, 92053L);

        mockMvc.perform(post("/departments/{id}/move", 92051L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"92053\",\"version\":\"0\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void reactivatingChildRequiresAnActiveParent() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92061L, "INACTIVE_PARENT", "Inactive parent", null, "/92061/", "INACTIVE");
        insertDepartment(92062L, "INACTIVE_CHILD", "Inactive child", 92061L, "/92061/92062/", "INACTIVE");

        mockMvc.perform(patch("/departments/{id}", 92062L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Inactive child","sortNo":1,"status":"ACTIVE","effectiveDate":"2026-01-01","version":"0"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createAndMoveRejectMissingOrInactiveParentsWithStableResponses() throws Exception {
        grantOrganizationManagementPermission();
        insertDepartment(92071L, "MOVE_STABLE", "Move stable", null, "/92071/", "ACTIVE");
        insertDepartment(92072L, "INACTIVE_MOVE_PARENT", "Inactive move parent", null, "/92072/", "INACTIVE");

        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"MISSING_PARENT\",\"name\":\"Missing parent\",\"parentId\":\"99999\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"INACTIVE_PARENT\",\"name\":\"Inactive parent\",\"parentId\":\"92072\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/departments/{id}/move", 92071L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"99999\",\"version\":\"0\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(post("/departments/{id}/move", 92071L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"92072\",\"version\":\"0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void restrictedManagerCannotAssignLeaderOutsideWritableScope() throws Exception {
        insertDepartment(92081L, "LEADER_SCOPE", "Leader scope", null, "/92081/", "ACTIVE");
        insertDepartment(92082L, "LEADER_OUTSIDE", "Leader outside", null, "/92082/", "ACTIVE");
        insertActiveEmployee(92083L, "LEADER_OUTSIDE_EMPLOYEE", 92082L);
        grantCustomOrganizationManagementPermission(92081L);

        mockMvc.perform(patch("/departments/{id}", 92081L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Leader scope","leaderEmployeeId":"92083","sortNo":1,"status":"ACTIVE","effectiveDate":"2026-01-01","version":"0"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void createAndMoveDenyOutOfScopeParentBeforeCheckingItsState() throws Exception {
        insertDepartment(92091L, "OUT_SCOPE_MOVE", "Out scope move", null, "/92091/", "ACTIVE");
        insertDepartment(92092L, "OUT_SCOPE_PARENT", "Out scope parent", null, "/92092/", "INACTIVE");
        grantCustomOrganizationManagementPermission(92091L);

        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OUT_SCOPE_CREATE\",\"name\":\"Out scope create\",\"parentId\":\"99999\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
        mockMvc.perform(post("/departments/{id}/move", 92091L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"92092\",\"version\":\"0\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void reactivationDeniesOutOfScopeParentBeforeCheckingItsState() throws Exception {
        insertDepartment(92101L, "OUT_SCOPE_REACTIVATION_PARENT", "Out scope reactivation parent", null, "/92101/", "INACTIVE");
        insertDepartment(92102L, "OUT_SCOPE_REACTIVATION_CHILD", "Out scope reactivation child", 92101L, "/92101/92102/", "INACTIVE");
        grantCustomOrganizationManagementPermission(92102L);

        mockMvc.perform(patch("/departments/{id}", 92102L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Out scope reactivation child","sortNo":1,"status":"ACTIVE","effectiveDate":"2026-01-01","version":"0"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    @Test
    void createCannotProbeDuplicateCodeBeforeRootScopeAuthorization() throws Exception {
        insertDepartment(92111L, "EXISTING_OUT_OF_SCOPE_CODE", "Existing outside scope", null, "/92111/", "ACTIVE");
        insertDepartment(92112L, "AUTHORIZED_SCOPE_ANCHOR", "Authorized scope anchor", null, "/92112/", "ACTIVE");
        grantCustomOrganizationManagementPermission(92112L);

        mockMvc.perform(post("/departments")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"EXISTING_OUT_OF_SCOPE_CODE\",\"name\":\"Probe attempt\",\"status\":\"ACTIVE\",\"effectiveDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_DENIED"));
    }

    private void insertDepartment(long id, String code, String name, Long parentId, String path, String status) {
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status, sort_no)
                VALUES (?, ?, ?, ?, ?, '2026-01-01', ?, 1)
                """, id, code, name, parentId, path, status);
    }

    private void insertActiveEmployee(long id, String employeeNo, long departmentId) {
        jdbcTemplate.update("""
                INSERT INTO hr_position (id, code, name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                ON DUPLICATE KEY UPDATE deleted = 0, status = 'ACTIVE'
                """, id, "POS_" + id, "Test position");
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date)
                VALUES (?, ?, ?, ?, ?, 'FORMAL', '2026-01-01')
                ON DUPLICATE KEY UPDATE employee_no = VALUES(employee_no), name = VALUES(name),
                    department_id = VALUES(department_id), position_id = VALUES(position_id),
                    employment_status = 'FORMAL', deleted = 0
                """, id, employeeNo, "Test employee", departmentId, id);
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

    private void grantCustomOrganizationManagementPermission(long... departmentIds) {
        long roleId = 90012L;
        long scopeId = 90014L;
        jdbcTemplate.update("DELETE FROM sys_data_scope_dept WHERE scope_id = ?", scopeId);
        jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ? AND role_id = ?", 90001L, roleId);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", roleId);
        jdbcTemplate.update("DELETE FROM sys_data_scope WHERE id = ?", scopeId);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, ?, ?, 'ACTIVE')", roleId, "TEST_CUSTOM_ORG_MANAGER", "Custom organization manager");
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (?, ?, 'org:manage', 'BUTTON', 'ACTIVE')
                ON DUPLICATE KEY UPDATE deleted = 0, status = 'ACTIVE'
                """, 90013L, "Organization manage");
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = 'org:manage' AND deleted = 0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_data_scope (id, scope_type, name) VALUES (?, 'CUSTOM', ?)", scopeId, "Department move scope");
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (id, role_id, scope_type, scope_id) VALUES (?, ?, 'CUSTOM', ?)", 90015L, roleId, scopeId);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 90016L, 90001L, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 90017L, roleId, menuId);
        for (int index = 0; index < departmentIds.length; index++) {
            jdbcTemplate.update("INSERT INTO sys_data_scope_dept (id, scope_id, department_id) VALUES (?, ?, ?)",
                    90020L + index, scopeId, departmentIds[index]);
        }
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
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (id, role_id, scope_type) VALUES (?, ?, 'ALL')", 90006L, 90002L);
    }
}
