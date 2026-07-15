package com.hrpm.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrpm.service.TokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class ApiTestSupportTests {
    private static final long DEPARTMENT_ID = 8_810_001L;
    private static final long POSITION_ID = 8_810_002L;
    private static final long RANK_ID = 8_810_003L;
    private static final long EMPLOYEE_ID = 8_810_004L;
    private static final long USER_ID = 8_810_005L;
    private static final long ROLE_ID = 8_810_006L;
    private static final long MENU_ID = 8_810_007L;
    private static final long SCOPE_ID = 8_810_008L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @Test
    void createsAuthorizationFixturesAndCleansThemUp() {
        ApiTestSupport support = new ApiTestSupport(jdbcTemplate, tokenService);
        support.insertDepartment(DEPARTMENT_ID, "API_SUPPORT_DEPT", "API support department", null);
        support.insertPosition(POSITION_ID, "API_SUPPORT_POSITION", "API support position");
        support.insertRank(RANK_ID, "API_SUPPORT_RANK", "API support rank", 1);
        support.insertEmployee(EMPLOYEE_ID, "API_SUPPORT_EMPLOYEE", "API support employee", DEPARTMENT_ID, POSITION_ID, RANK_ID);
        support.insertUser(USER_ID, "api-support-user", "test-password", EMPLOYEE_ID, 3);
        support.insertRole(ROLE_ID, "API_SUPPORT_ROLE", "API support role");
        support.insertMenu(MENU_ID, "API support permission", "api:support:read");
        support.assignUserRole(USER_ID, ROLE_ID);
        support.assignRoleMenu(ROLE_ID, MENU_ID);
        support.insertDataScope(SCOPE_ID, "CUSTOM", "API support custom scope");
        support.assignRoleScope(ROLE_ID, "CUSTOM", SCOPE_ID);
        support.assignScopeDepartment(SCOPE_ID, DEPARTMENT_ID);

        assertThat(support.authorizationHeader(USER_ID, "api-support-user", 3)).startsWith("Bearer ");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_data_scope_dept WHERE scope_id = ? AND department_id = ? AND deleted = 0",
                Integer.class, SCOPE_ID, DEPARTMENT_ID)).isEqualTo(1);

        support.cleanup(new ApiTestSupport.FixtureIds(
                List.of(USER_ID), List.of(ROLE_ID), List.of(MENU_ID), List.of(SCOPE_ID),
                List.of(EMPLOYEE_ID), List.of(POSITION_ID), List.of(RANK_ID), List.of(DEPARTMENT_ID)));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id = ?", Integer.class, USER_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hr_employee WHERE id = ?", Integer.class, EMPLOYEE_ID)).isZero();
    }
}
