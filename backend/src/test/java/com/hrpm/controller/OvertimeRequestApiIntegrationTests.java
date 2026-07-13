package com.hrpm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import java.time.LocalDate;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class OvertimeRequestApiIntegrationTests {
    private static final long USER_ID = 96001L;
    private static final long EMPLOYEE_ID = 96002L;
    private static final long DEPARTMENT_ID = 96003L;
    private static final long POSITION_ID = 96004L;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TokenService tokenService;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM wf_action_log");
        jdbcTemplate.update("DELETE FROM wf_task");
        jdbcTemplate.update("DELETE FROM wf_instance");
        jdbcTemplate.update("DELETE FROM wf_template_node WHERE template_id = ?", 96010L);
        jdbcTemplate.update("DELETE FROM wf_template_scope WHERE template_id = ?", 96010L);
        jdbcTemplate.update("DELETE FROM wf_template WHERE id = ?", 96010L);
        jdbcTemplate.update("DELETE FROM att_balance_change WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_leave_balance WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_overtime_request WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 96005L);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 96005L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM hr_position WHERE id = ?", POSITION_ID);
        jdbcTemplate.update("DELETE FROM hr_department WHERE id = ?", DEPARTMENT_ID);
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (?, 'OT_TEST', 'Overtime Test', ?, ?, 'ACTIVE')", DEPARTMENT_ID, "/96003/", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (?, 'OT_POSITION', 'Overtime Position', 'ACTIVE')", POSITION_ID);
        jdbcTemplate.update("INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date) VALUES (?, 'E-OT-001', 'Overtime Employee', ?, ?, 'FORMAL', ?)", EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, LocalDate.of(2025, 1, 1));
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version) VALUES (?, 'overtime-employee', ?, ?, 'ACTIVE', 1)", USER_ID, new BCryptPasswordEncoder().encode("correct-password"), EMPLOYEE_ID);
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, 'OT_SUBMIT', 'Overtime submit', 'ACTIVE')", 96005L);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 96007L, USER_ID, 96005L);
        long attendanceSubmitMenuId = jdbcTemplate.queryForObject("SELECT id FROM sys_menu WHERE permission_code = 'attendance:submit' AND deleted = 0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 96008L, 96005L, attendanceSubmitMenuId);
        jdbcTemplate.update("INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status) VALUES (?, 'TEST_OVERTIME', 'Test overtime workflow', 'OVERTIME', 10, 1, 'ACTIVE')", 96010L);
        jdbcTemplate.update("INSERT INTO wf_template_scope (id, template_id, department_id) VALUES (?, ?, ?)", 96011L, 96010L, DEPARTMENT_ID);
        jdbcTemplate.update("INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule) VALUES (?, ?, 1, 'SPECIFIC_USER', JSON_OBJECT('userId', ?))", 96012L, 96010L, USER_ID);
    }

    @Test
    void approvedTimeOffOvertimeCreatesAndCancellationReversesImmutableBalanceChanges() throws Exception {
        String body = "{\"startTime\":\"2026-07-18T09:00:00Z\",\"endTime\":\"2026-07-18T11:00:00Z\",\"reason\":\"Release support\",\"compensationType\":\"TIME_OFF\"}";
        String token = "Bearer " + tokenService.issue(USER_ID, "overtime-employee", 1);
        String requestId = mockMvc.perform(post("/overtime-requests").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.durationHours").value(2.0)).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestId).path("data").path("id").asText());
        mockMvc.perform(post("/overtime-requests/{id}/submit", id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":\"0\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM att_overtime_request WHERE id = ?)", Long.class, id);
        mockMvc.perform(get("/workflow/tasks").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].businessType").value("OVERTIME"));
        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"comment\":\"approved\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("2.00"), jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE employee_id = ? AND balance_type = 'TIME_OFF'", java.math.BigDecimal.class, EMPLOYEE_ID));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'TIME_OFF' AND source_id = ?", Integer.class, id));
        mockMvc.perform(post("/overtime-requests/{id}/cancel", id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"version\":\"2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("0.00"), jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE employee_id = ? AND balance_type = 'TIME_OFF'", java.math.BigDecimal.class, EMPLOYEE_ID));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'OVERTIME_CANCELLATION' AND source_id = ?", Integer.class, id));
    }
}
