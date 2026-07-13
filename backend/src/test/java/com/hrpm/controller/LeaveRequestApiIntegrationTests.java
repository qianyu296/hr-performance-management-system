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
import org.junit.jupiter.api.AfterEach;
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
        cleanWorkCalendarFixture();
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
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", 92006L);
        jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", 92003L);
        jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", 92007L);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 92002L);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", 92006L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 90002L);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", 90001L);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id = ?", 91004L);
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

    @AfterEach
    void cleanWorkCalendarAfterTest() {
        cleanWorkCalendarFixture();
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
    void workCalendarControlsLeaveDurationAndMinimumUnit() throws Exception {
        grantLeaveSubmitPermission();
        String adminToken = "Bearer " + tokenService.issue(9000001L, "admin", 0);
        mockMvc.perform(post("/work-calendars")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calendarYear":2026,"name":"2026 Test Calendar","timeZone":"UTC","status":"ACTIVE","days":[
                                  {"workDate":"2026-07-27","workday":true,"workHours":4.00},
                                  {"workDate":"2026-07-28","workday":false,"workHours":0.00,"holidayName":"Test Holiday"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].workHours").value(4.0))
                .andExpect(jsonPath("$.data.days[1].workday").value(false));

        mockMvc.perform(post("/leave-requests")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-calendar-duration-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leaveTypeId":"92001","startTime":"2026-07-27T09:00:00Z","endTime":"2026-07-28T17:00:00Z","reason":"Calendar duration"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationHours").value(4.0));

        jdbcTemplate.update("UPDATE att_leave_type SET min_unit_hours = 2.00 WHERE id = ?", 92001L);
        mockMvc.perform(post("/leave-requests")
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-calendar-min-unit-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leaveTypeId":"92001","startTime":"2026-07-27T09:00:00Z","endTime":"2026-07-27T10:00:00Z","reason":"Invalid unit"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void authorizedUserCanReadAndAdjustLeaveBalanceWithImmutableChange() throws Exception {
        grantBalanceAdjustmentPermission();
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);

        mockMvc.perform(get("/leave-balances").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("93001"))
                .andExpect(jsonPath("$.data[0].availableHours").value(16.0));

        mockMvc.perform(post("/leave-balances/{id}/adjust", 93001L)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deltaHours\":\"2.00\",\"direction\":\"INCREASE\",\"reason\":\"Annual leave entitlement correction\",\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableHours").value(18.0))
                .andExpect(jsonPath("$.data.version").value("1"));

        mockMvc.perform(get("/leave-balances/{id}/changes", 93001L).header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceType").value("MANUAL_ADJUSTMENT"))
                .andExpect(jsonPath("$.data[0].deltaHours").value(2.0))
                .andExpect(jsonPath("$.data[0].reason").value("Annual leave entitlement correction"));
    }

    @Test
    void directManagerRuleResolvesManagerAccountWhenLeaveIsSubmitted() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("UPDATE wf_template_node SET node_type = 'DIRECT_MANAGER', approver_rule = JSON_OBJECT('type', 'DIRECT_MANAGER') WHERE id = ?", 95003L);
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, employment_status, hire_date)
                VALUES (?, 'E-TEST-MANAGER', 'Test Manager', ?, ?, 'FORMAL', ?)
                """, 91004L, 91003L, 91002L, LocalDate.of(2024, 1, 1));
        jdbcTemplate.update("UPDATE hr_employee SET manager_employee_id = ? WHERE id = ?", 91004L, 91001L);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version)
                VALUES (?, 'test-manager', ?, ?, 'ACTIVE', 1)
                """, 90002L, new BCryptPasswordEncoder().encode("correct-password"), 91004L);
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        long requestId = 94010L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (
                    id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94010', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, 91001L, 92001L,
                Instant.parse("2026-07-22T09:00:00Z"), Instant.parse("2026-07-22T17:00:00Z"));

        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-submit-direct-manager-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        org.junit.jupiter.api.Assertions.assertEquals(90002L, jdbcTemplate.queryForObject("""
                SELECT assignee_user_id FROM wf_task
                WHERE instance_id = (SELECT workflow_instance_id FROM att_leave_request WHERE id = ?)
                """, Long.class, requestId));
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
        jdbcTemplate.update("UPDATE wf_template_node SET approver_rule = JSON_OBJECT('userId', 90001) WHERE id = ?", 95004L);

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
    void workflowParticipantsCanReadInstanceHistoryButOtherUsersCannot() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94011L, "LR94011", "2026-07-23T09:00:00Z", "2026-07-23T17:00:00Z");
        long instanceId = findInstanceId(requestId);

        mockMvc.perform(get("/workflow/tasks/instances/{id}", instanceId).header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.history[0].action").value("SUBMIT"));

        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, 'workflow-outsider', ?, 'ACTIVE', 1)
                """, 90002L, new BCryptPasswordEncoder().encode("correct-password"));
        mockMvc.perform(get("/workflow/tasks/instances/{id}", instanceId)
                        .header("Authorization", "Bearer " + tokenService.issue(90002L, "workflow-outsider", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void approverCanReturnLeaveAndOwnerCanResubmitSameWorkflowInstance() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94012L, "LR94012", "2026-07-24T09:00:00Z", "2026-07-24T17:00:00Z");
        long taskId = findTaskId(requestId);
        long instanceId = findInstanceId(requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/return", taskId)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"please add handover details\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
        org.junit.jupiter.api.Assertions.assertEquals("DRAFT",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));

        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-resubmit-returned-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        org.junit.jupiter.api.Assertions.assertEquals(instanceId, findInstanceId(requestId));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wf_instance WHERE business_id = ?", Integer.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_task WHERE instance_id = ? AND status = 'PENDING'", Integer.class, instanceId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_action_log WHERE instance_id = ? AND action = 'RESUBMIT'", Integer.class, instanceId));
    }

    @Test
    void ownerCanWithdrawPendingWorkflowAndRestoreLeaveDraft() throws Exception {
        long requestId = seedSubmittedLeaveRequest(94013L, "LR94013", "2026-07-25T09:00:00Z", "2026-07-25T17:00:00Z");
        long instanceId = findInstanceId(requestId);

        mockMvc.perform(post("/workflow/tasks/instances/{id}/withdraw", instanceId)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"request withdrawn by applicant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        org.junit.jupiter.api.Assertions.assertEquals("DRAFT",
                jdbcTemplate.queryForObject("SELECT status FROM att_leave_request WHERE id = ?", String.class, requestId));
        org.junit.jupiter.api.Assertions.assertEquals("WITHDRAWN",
                jdbcTemplate.queryForObject("SELECT status FROM wf_instance WHERE id = ?", String.class, instanceId));
        org.junit.jupiter.api.Assertions.assertEquals("WITHDRAWN",
                jdbcTemplate.queryForObject("SELECT status FROM wf_task WHERE instance_id = ?", String.class, instanceId));
    }

    @Test
    void workflowIntervenerCanTransferPendingTask() throws Exception {
        grantLeaveSubmitPermission();
        seedLeaveWorkflowTemplate();
        jdbcTemplate.update("UPDATE wf_template_node SET approver_rule = JSON_OBJECT('userId', 9000001) WHERE id = ?", 95003L);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password_hash, status, session_version)
                VALUES (?, 'transferred-approver', ?, 'ACTIVE', 1)
                """, 90002L, new BCryptPasswordEncoder().encode("correct-password"));
        jdbcTemplate.update("""
                INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours)
                VALUES (?, ?, 'ANNUAL', 2026, 16.00)
                """, 93001L, 91001L);
        long requestId = 94014L;
        jdbcTemplate.update("""
                INSERT INTO att_leave_request (id, request_no, employee_id, leave_type_id, start_time, end_time,
                    duration_hours, reason, status, organization_snapshot)
                VALUES (?, 'LR94014', ?, ?, ?, ?, 8.00, 'Annual leave', 'DRAFT', '{}')
                """, requestId, 91001L, 92001L, Instant.parse("2026-07-26T09:00:00Z"), Instant.parse("2026-07-26T17:00:00Z"));
        mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
                        .header("Authorization", bearerToken())
                        .header("Idempotency-Key", "leave-submit-transfer-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());
        long taskId = findTaskId(requestId);

        mockMvc.perform(post("/workflow/tasks/{id}/transfer", taskId)
                        .header("Authorization", "Bearer " + tokenService.issue(9000001L, "admin", 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"comment\":\"reassign for coverage\",\"transferToUserId\":90002}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        org.junit.jupiter.api.Assertions.assertEquals("TRANSFERRED",
                jdbcTemplate.queryForObject("SELECT status FROM wf_task WHERE id = ?", String.class, taskId));
        org.junit.jupiter.api.Assertions.assertEquals(90002L, jdbcTemplate.queryForObject(
                "SELECT assignee_user_id FROM wf_task WHERE instance_id = ? AND status = 'PENDING'", Long.class, findInstanceId(requestId)));
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

    private void grantBalanceAdjustmentPermission() {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (?, 'TEST_BALANCE_ADJUST', 'Test balance adjust', 'ACTIVE')", 92006L);
        long balanceAdjustMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_menu WHERE permission_code = 'attendance:balance:adjust' AND deleted = 0", Long.class);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", 92008L, 90001L, 92006L);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", 92009L, 92006L, balanceAdjustMenuId);
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

    private long findInstanceId(long requestId) {
        return jdbcTemplate.queryForObject("SELECT workflow_instance_id FROM att_leave_request WHERE id = ?", Long.class, requestId);
    }

    private void cleanWorkCalendarFixture() {
        jdbcTemplate.update("""
                DELETE d FROM att_work_calendar_day d
                JOIN att_work_calendar c ON c.id = d.calendar_id
                WHERE c.name = '2026 Test Calendar'
                """);
        jdbcTemplate.update("DELETE FROM att_work_calendar WHERE name = '2026 Test Calendar'");
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
