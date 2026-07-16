package com.hrpm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import java.time.LocalDate;
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
class LeaveTypeApiIntegrationTests {
    private static final long HR_USER_ID = 98001L;
    private static final long EMPLOYEE_ID = 98002L;
    private static final long ROLE_ID = 98003L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void seedHrWithAttendanceManagementPermission() {
        cleanFixture();
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (98005, 'LT_TEST', '请假类型测试部门', '/98005/', ?, 'ACTIVE')", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (98006, 'LT_TEST', '请假类型测试岗位', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date) VALUES (?, 'LT-TEST-001', '请假类型人事专员', 98005, 98006, 'FORMAL', ?)", EMPLOYEE_ID, LocalDate.of(2025, 1, 1));
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version) VALUES (?, 'leave-type-hr', 'unused', ?, 'ACTIVE', 0)", HR_USER_ID, EMPLOYEE_ID);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, 'LEAVE_TYPE_HR', '请假类型人事专员', 'ACTIVE')", ROLE_ID);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (98007, ?, ?)", HR_USER_ID, ROLE_ID);
        jdbcTemplate.update("""
                INSERT INTO sys_role_menu (id, role_id, menu_id)
                SELECT 98008, ?, id FROM sys_menu
                WHERE permission_code = 'attendance:manage' AND deleted = 0
                """, ROLE_ID);
    }

    @AfterEach
    void cleanAfterTest() {
        cleanFixture();
    }

    @Test
    void hrCanCreateBalanceDeductingTypeAndInitializesCurrentYearBalance() throws Exception {
        mockMvc.perform(post("/leave-types")
                        .header("Authorization", "Bearer " + tokenService.issue(HR_USER_ID, "leave-type-hr", 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"HR_ANNUAL","name":"HR Annual Leave","deductBalance":true,"annualQuota":80.00,"minUnitHours":1.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("HR_ANNUAL"))
                .andExpect(jsonPath("$.data.annualQuota").value(80.0));

        Integer balanceCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM att_leave_balance
                WHERE employee_id = ? AND balance_type = 'HR_ANNUAL' AND balance_year = ? AND available_hours = 80.00
                """, Integer.class, EMPLOYEE_ID, LocalDate.now().getYear());
        assertEquals(1, balanceCount);
    }

    @Test
    void changingAnnualQuotaDoesNotRewriteExistingBalances() throws Exception {
        jdbcTemplate.update("INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status) VALUES (98009, 'HR_ANNUAL', 'HR Annual Leave', 1, 1.00, 80.00, 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours) VALUES (98010, ?, 'HR_ANNUAL', ?, 80.00)", EMPLOYEE_ID, LocalDate.now().getYear());

        mockMvc.perform(patch("/leave-types/98009")
                        .header("Authorization", "Bearer " + tokenService.issue(HR_USER_ID, "leave-type-hr", 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"HR Annual Leave Updated","deductBalance":true,"annualQuota":96.00,"minUnitHours":2.00,"version":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.annualQuota").value(96.0));

        assertEquals("80.00", jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = 98010", String.class));
    }

    @Test
    void hrCanDisableLeaveType() throws Exception {
        jdbcTemplate.update("INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status) VALUES (98009, 'HR_ANNUAL', 'HR Annual Leave', 1, 1.00, 80.00, 'ACTIVE')");

        mockMvc.perform(post("/leave-types/98009/disable")
                        .header("Authorization", "Bearer " + tokenService.issue(HR_USER_ID, "leave-type-hr", 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void seededSystemMenuUsesChineseDisplayName() {
        assertEquals("系统管理", jdbcTemplate.queryForObject(
                "SELECT name FROM sys_menu WHERE permission_code = 'system:manage' AND deleted = 0", String.class));
    }

    private void cleanFixture() {
        jdbcTemplate.update("DELETE FROM att_leave_balance WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_leave_type WHERE code = 'HR_ANNUAL'");
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", HR_USER_ID);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", ROLE_ID);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", HR_USER_ID);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM hr_position WHERE id = 98006");
        jdbcTemplate.update("DELETE FROM hr_department WHERE id = 98005");
    }
}
