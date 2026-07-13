package com.hrpm.controller;


import com.hrpm.entity.Department;
import com.hrpm.service.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.Instant;
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
class LeaveRequestApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void seedEmployeeAndLeaveType() {
        jdbcTemplate.update("DELETE FROM wf_action_log");
        jdbcTemplate.update("DELETE FROM wf_task");
        jdbcTemplate.update("DELETE FROM wf_instance");
        jdbcTemplate.update("DELETE FROM wf_template_node");
        jdbcTemplate.update("DELETE FROM wf_template_scope");
        jdbcTemplate.update("DELETE FROM wf_template");
        jdbcTemplate.update("DELETE FROM att_balance_change");
        jdbcTemplate.update("DELETE FROM att_leave_balance");
        jdbcTemplate.update("DELETE FROM att_leave_request");
        jdbcTemplate.update("DELETE FROM att_leave_type WHERE id = ?", 92001L);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", 90001L);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 92002L);
        jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", 92003L);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 92002L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 90002L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 90001L);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", 91001L);
        jdbcTemplate.update("DELETE FROM hr_position WHERE id = ?", 91002L);
        jdbcTemplate.update("DELETE FROM hr_department WHERE id = ?", 91003L);

        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, path, effective_date, status)
                VALUES (?, 'TEST', 'Test Department', ?, ?, 'ACTIVE')
                """, 91003L, "/91003/", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("""
                INSERT INTO hr_position (id, code, name, status)
                VALUES (?, 'TEST_POSITION', 'Test Position', 'ACTIVE')
                """, 91002L);
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date)
                VALUES (?, 'E-TEST-001', 'Test Employee', ?, ?, 'FORMAL', ?)
                """, 91001L, 91003L, 91002L, LocalDate.of(2025, 1, 1));
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
                VALUES (?, 'test-employee', ?, ?, 'ACTIVE', 2)
                """, 90001L, new BCryptPasswordEncoder().encode("correct-password"), 91001L);
        jdbcTemplate.update("""
                INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, status)
                VALUES (?, 'ANNUAL', 'Annual Leave', 1, 1.00, 'ACTIVE')
                """, 92001L);
    }

    @Test
    void authorizedEmployeeCanCreateLeaveDraftWithServerCalculatedDuration() throws Exception {
        grantLeaveSubmitPermission();

        mockMvc.perform(post("/leave-requests")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-draft-create-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leaveTypeId":"92001","startTime":"2026-07-13T09:00:00Z","endTime":"2026-07-13T17:00:00Z","reason":"Annual leave"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.durationHours").value(8.0));
    }

    @Test
    void employeeWithoutAttendanceSubmitPermissionCannotCreateLeaveDraft() throws Exception {
        mockMvc.perform(post("/leave-requests")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-draft-denied-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leaveTypeId":"92001","startTime":"2026-07-13T09:00:00Z","endTime":"2026-07-13T17:00:00Z","reason":"Annual leave"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanListActiveLeaveTypes() throws Exception {
        mockMvc.perform(get("/leave-types")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("92001"))
                .andExpect(jsonPath("$.data[0].code").value("ANNUAL"))
                .andExpect(jsonPath("$.data[0].name").value("Annual Leave"));
    }

    @Test
    void ownerCanListOwnLeaveRequests() throws Exception {
        grantLeaveSubmitPermission();
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94007', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, 94007L, 91001L, 92001L,
                Instant.parse("2026-07-19T09:00:00Z"), Instant.parse("2026-07-19T17:00:00Z"));

        mockMvc.perform(get("/leave-requests")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("94007"))
                .andExpect(jsonPath("$.data[0].requestNo").value("LR94007"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].leaveTypeName").value("Annual Leave"));
    }

    @Test
    void assigneeCanListPendingWorkflowTasks() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94008L, "LR94008", "2026-07-20T09:00:00Z", "2026-07-20T17:00:00Z");
        long taskId = findTaskId(requestId);

        mockMvc.perform(get("/workflow/tasks")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(Long.toString(taskId)))
                .andExpect(jsonPath("$.data[0].businessId").value("94008"))
                .andExpect(jsonPath("$.data[0].businessType").value("LEAVE"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].requestNo").value("LR94008"));
    }

    @Test
    void ownerCanSubmitDraftAndCreateInitialWorkflowTask() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        long requestId = 94001L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94001', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, 91001L, 92001L,
                Instant.parse("2026-07-13T09:00:00Z"), Instant.parse("2026-07-13T17:00:00Z"));

        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-submit-success-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wf_instance", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wf_task WHERE status = 'PENDING'", Integer.class));
    }

    @Test
    void assigneeApprovalCompletesLeaveExactlyOnce() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        long requestId = 94002L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94002', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, 91001L, 92001L,
                Instant.parse("2026-07-14T09:00:00Z"), Instant.parse("2026-07-14T17:00:00Z"));
        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-submit-approval-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());
        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM att_leave_request WHERE id = ?)", Long.class, requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-approve-success-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        org.junit.jupiter.api.Assertions.assertEquals("APPROVED",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("8.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'LEAVE_APPROVAL' AND source_id = ?", Integer.class, requestId));
    }

    @Test
    void multiNodeApprovalKeepsLeaveInProgressUntilFinalApproverActs() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, 'second-approver', ?, 'ACTIVE', 1)
                """, 90002L, new BCryptPasswordEncoder().encode("correct-password"));
        jdbcTemplate.update("""
                INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule)
                VALUES (?, ?, 2, 'SPECIFIC_USER', JSON_OBJECT('userId', 90002))
                """, 95004L, 95001L);
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        long requestId = 94009L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94009', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, 91001L, 92001L,
                Instant.parse("2026-07-21T09:00:00Z"), Instant.parse("2026-07-21T17:00:00Z"));

        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-submit-multi-node-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());
        long firstTaskId = jdbcTemplate.queryForObject("""
                SELECT id FROM wf_task
                WHERE instance_id = (SELECT workflow_instance_id FROM att_leave_request WHERE id = ?)
                  AND node_no = 1
                """, Long.class, requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/approve", firstTaskId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-approve-multi-node-first-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"manager approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("16.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        long secondTaskId = jdbcTemplate.queryForObject("""
                SELECT id FROM wf_task
                WHERE instance_id = (SELECT workflow_instance_id FROM att_leave_request WHERE id = ?)
                  AND node_no = 2 AND status = 'PENDING'
                """, Long.class, requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/approve", secondTaskId)
                        .header("Authorization", "Bearer " + tokenService.issue(90002L, "second-approver", 1))
                        .header("Idempotency-Key", "leave-approve-multi-node-final-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"hr approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        org.junit.jupiter.api.Assertions.assertEquals("APPROVED",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("8.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'LEAVE_APPROVAL' AND source_id = ?", Integer.class, requestId));
    }

    @Test
    void nonAssigneeCannotApprovePendingWorkflowTask() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94004L, "LR94004", "2026-07-16T09:00:00Z", "2026-07-16T17:00:00Z");
        long taskId = findTaskId(requestId);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, 'other-user', ?, 'ACTIVE', 1)
                """, 90002L, new BCryptPasswordEncoder().encode("correct-password"));

        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId)
                        .header("Authorization", "Bearer " + tokenService.issue(90002L, "other-user", 1))
                        .header("Idempotency-Key", "leave-approve-non-assignee-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"not my task\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TASK_INVALID"));

        org.junit.jupiter.api.Assertions.assertEquals("PENDING",
                jdbcTemplate.queryForObject("SELECT status FROM wf_task WHERE id = ?", String.class, taskId));
        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("16.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'LEAVE_APPROVAL' AND source_id = ?", Integer.class, requestId));
    }

    @Test
    void duplicateApprovalDoesNotDeductBalanceTwice() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94005L, "LR94005", "2026-07-17T09:00:00Z", "2026-07-17T17:00:00Z");
        long taskId = findTaskId(requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-approve-duplicate-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"approved\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-approve-duplicate-0002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"1\",\"comment\":\"approved again\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TASK_INVALID"));

        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("8.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'LEAVE_APPROVAL' AND source_id = ?", Integer.class, requestId));
    }

    @Test
    void assigneeCanRejectLeaveWithoutChangingBalance() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94006L, "LR94006", "2026-07-18T09:00:00Z", "2026-07-18T17:00:00Z");
        long taskId = findTaskId(requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/reject", taskId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-reject-success-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"insufficient handover\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        org.junit.jupiter.api.Assertions.assertEquals("REJECTED",
                jdbcTemplate.queryForObject("SELECT status FROM wf_task WHERE id = ?", String.class, taskId));
        org.junit.jupiter.api.Assertions.assertEquals("REJECTED",
                jdbcTemplate.queryForObject("SELECT status FROM wf_instance WHERE business_id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals("REJECTED",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("16.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_action_log WHERE task_id = ? AND action = 'REJECT'", Integer.class, taskId));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_id = ?", Integer.class, requestId));
    }

    @Test
    void ownerCanCancelApprovedLeaveExactlyOnce() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours, version) VALUES (?, ?, 'ANNUAL', 2026, 8.00, 1)", 93001L, 91001L);
        long requestId = 94003L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (id, request_no, employee_id, leave_type_id, start_time, end_time, duration_hours, reason, status, organization_snapshot, version)
                VALUES (?, 'LR94003', ?, ?, ?, ?, 8.00, 'Annual leave', 'APPROVED', '{}', 2)
                """, requestId, 91001L, 92001L, Instant.parse("2026-07-15T09:00:00Z"), Instant.parse("2026-07-15T17:00:00Z"));

        mockMvc.perform(post("/leave-requests/{id}/cancel", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-cancel-success-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("16.00"),
                jdbcTemplate.queryForObject("SELECT available_hours FROM att_leave_balance WHERE id = ?", java.math.BigDecimal.class, 93001L));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM att_balance_change WHERE source_type = 'LEAVE_CANCELLATION' AND source_id = ?", Integer.class, requestId));
    }

    private String bearerToken() {
        return "Bearer " + tokenService.issue(90001L, "test-employee", 2);
    }

    private void grantLeaveSubmitPermission() {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, 'TEST_ATTENDANCE_SUBMIT', 'Test attendance submit', 'ACTIVE')", 92002L);
        jdbcTemplate.update("""
                INSERT INTO sys_menu (id, name, permission_code, menu_type, status)
                VALUES (?, 'Leave submit', 'attendance:submit', 'BUTTON', 'ACTIVE')
                """, 92003L);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 92004L, 90001L, 92002L);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 92005L, 92002L, 92003L);
    }

    private long seedSubmittedLeaveRequest(long requestId, String requestNo, String startTime, String endTime) throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, ?, ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, requestNo, 91001L, 92001L, Instant.parse(startTime), Instant.parse(endTime));
        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", requestNo + "-submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());
        return requestId;
    }

    private long findTaskId(long requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM att_leave_request WHERE id = ?)",
                Long.class, requestId);
    }

    private void seedLeaveWorkflowTemplate() {
        jdbcTemplate.update("""
                INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status)
                VALUES (?, 'TEST_LEAVE', 'Test leave workflow', 'LEAVE', 10, 1, 'ACTIVE')
                """, 95001L);
        jdbcTemplate.update("INSERT INTO wf_template_scope (id, template_id, department_id) VALUES (?, ?, ?)",
                95002L, 95001L, 91003L);
        jdbcTemplate.update("""
                INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule)
                VALUES (?, ?, 1, 'SPECIFIC_USER', JSON_OBJECT('userId', 90001))
                """, 95003L, 95001L);
    }
}
