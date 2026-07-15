package com.hrpm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import java.time.Instant;
import java.time.LocalDate;
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
class AttendanceMonthlySummaryApiIntegrationTests {
    private static final long EMPLOYEE_ID = 97001L;
    private static final long DEPARTMENT_ID = 97002L;
    private static final long POSITION_ID = 97003L;
    private static final long LEAVE_TYPE_ID = 97004L;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TokenService tokenService;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM rpt_attendance_month WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_balance_change WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_leave_balance WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_overtime_request WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_leave_request WHERE employee_id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM att_leave_type WHERE id = ?", LEAVE_TYPE_ID);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM hr_position WHERE id = ?", POSITION_ID);
        jdbcTemplate.update("DELETE FROM hr_department WHERE id = ?", DEPARTMENT_ID);
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (?, 'SUMMARY_TEST', '月度汇总测试部门', ?, ?, 'ACTIVE')", DEPARTMENT_ID, "/97002/", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (?, 'SUMMARY_POSITION', '月度汇总测试岗位', 'ACTIVE')", POSITION_ID);
        jdbcTemplate.update("INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date) VALUES (?, 'E-SUM-001', '月度汇总测试员工', ?, ?, 'FORMAL', ?)", EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, LocalDate.of(2025, 1, 1));
        jdbcTemplate.update("INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status) VALUES (?, 'SUMMARY_ANNUAL', '年假（汇总）', 1, 1.00, 80.00, 'ACTIVE')", LEAVE_TYPE_ID);
        jdbcTemplate.update("INSERT INTO att_leave_request (id, request_no, employee_id, leave_type_id, start_time, end_time, duration_hours, reason, status, organization_snapshot) VALUES (?, 'LR-SUM-APPROVED', ?, ?, ?, ?, 4.00, 'Approved leave', 'APPROVED', '{}')", 97010L, EMPLOYEE_ID, LEAVE_TYPE_ID, Instant.parse("2026-07-10T09:00:00Z"), Instant.parse("2026-07-10T13:00:00Z"));
        jdbcTemplate.update("INSERT INTO att_leave_request (id, request_no, employee_id, leave_type_id, start_time, end_time, duration_hours, reason, status, organization_snapshot) VALUES (?, 'LR-SUM-PENDING', ?, ?, ?, ?, 2.00, 'Pending leave', 'IN_PROGRESS', '{}')", 97011L, EMPLOYEE_ID, LEAVE_TYPE_ID, Instant.parse("2026-07-11T09:00:00Z"), Instant.parse("2026-07-11T11:00:00Z"));
        jdbcTemplate.update("INSERT INTO att_overtime_request (id, request_no, employee_id, start_time, end_time, duration_hours, reason, compensation_type, status, organization_snapshot) VALUES (?, 'OT-SUM-APPROVED', ?, ?, ?, 3.00, 'Approved overtime', 'TIME_OFF', 'APPROVED', '{}')", 97012L, EMPLOYEE_ID, Instant.parse("2026-07-12T18:00:00Z"), Instant.parse("2026-07-12T21:00:00Z"));
        jdbcTemplate.update("INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours) VALUES (?, ?, 'TIME_OFF', 2026, 3.00)", 97013L, EMPLOYEE_ID);
        jdbcTemplate.update("INSERT INTO att_balance_change (id, balance_id, employee_id, balance_type, delta_hours, before_hours, after_hours, source_type, source_id, reason, created_time) VALUES (?, ?, ?, 'TIME_OFF', 3.00, 0, 3.00, 'TIME_OFF', ?, 'Approved overtime', ?)", 97014L, 97013L, EMPLOYEE_ID, 97012L, Instant.parse("2026-07-12T21:00:00Z"));
    }

    @Test
    void adminCanRebuildAndReadIdempotentAttendanceMonthlySummary() throws Exception {
        String adminToken = "Bearer " + tokenService.issue(9000001L, "admin", 0);
        mockMvc.perform(post("/attendance/monthly-summaries/rebuild").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"month\":\"2026-07\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.affectedRows").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        mockMvc.perform(post("/attendance/monthly-summaries/rebuild").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"month\":\"2026-07\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/attendance/monthly-summaries").header("Authorization", adminToken)
                        .param("month", "2026-07").param("employeeId", Long.toString(EMPLOYEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].leaveHours").value(4.0))
                .andExpect(jsonPath("$.data[0].overtimeHours").value(3.0))
                .andExpect(jsonPath("$.data[0].timeOffDeltaHours").value(3.0))
                .andExpect(jsonPath("$.data[0].pendingRequestCount").value(1));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rpt_attendance_month WHERE employee_id = ? AND attendance_month = '2026-07-01'", Integer.class, EMPLOYEE_ID));
    }
}
