package com.hrpm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class PersonnelChangeApiIntegrationTests {
    private static final long HR_USER_ID = 99321L;
    private static final long HR_EMPLOYEE_ID = 99311L;
    private static final long EMPLOYEE_USER_ID = 99322L;
    private static final long EMPLOYEE_ID = 99312L;
    private static final long CURRENT_DEPARTMENT_ID = 99301L;
    private static final long TARGET_DEPARTMENT_ID = 99302L;
    private static final long HR_POSITION_ID = 99303L;
    private static final long EMPLOYEE_POSITION_ID = 99304L;
    private static final long TARGET_POSITION_ID = 99305L;
    private static final long EMPLOYEE_RANK_ID = 99306L;
    private static final long TARGET_RANK_ID = 99307L;
    private static final long TEMPLATE_ID = 99340L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM wf_action_log");
        jdbcTemplate.update("DELETE FROM wf_task");
        jdbcTemplate.update("DELETE FROM wf_instance");
        jdbcTemplate.update("DELETE FROM wf_template_node WHERE template_id = ?", TEMPLATE_ID);
        jdbcTemplate.update("DELETE FROM wf_template_scope WHERE template_id = ?", TEMPLATE_ID);
        jdbcTemplate.update("DELETE FROM wf_template WHERE id = ?", TEMPLATE_ID);

        jdbcTemplate.update("DELETE FROM hr_exit_handover_item");
        jdbcTemplate.update("DELETE FROM hr_exit_handover");
        jdbcTemplate.update("DELETE FROM hr_employee_history");
        jdbcTemplate.update("DELETE FROM hr_personnel_change");

        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN (?, ?)", HR_USER_ID, EMPLOYEE_USER_ID);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id IN (?, ?)", HR_USER_ID, EMPLOYEE_USER_ID);
        jdbcTemplate.update("DELETE FROM hr_employee WHERE id IN (?, ?)", HR_EMPLOYEE_ID, EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM hr_rank WHERE id IN (?, ?)", EMPLOYEE_RANK_ID, TARGET_RANK_ID);
        jdbcTemplate.update("DELETE FROM hr_position WHERE id IN (?, ?, ?)", HR_POSITION_ID, EMPLOYEE_POSITION_ID, TARGET_POSITION_ID);
        jdbcTemplate.update("DELETE FROM hr_department WHERE id IN (?, ?)", CURRENT_DEPARTMENT_ID, TARGET_DEPARTMENT_ID);

        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (?, 'PC_CUR', '人员当前部门', ?, ?, 'ACTIVE')",
                CURRENT_DEPARTMENT_ID, "/99301/", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("INSERT INTO hr_department (id, code, name, path, effective_date, status) VALUES (?, 'PC_TGT', '人员目标部门', ?, ?, 'ACTIVE')",
                TARGET_DEPARTMENT_ID, "/99302/", LocalDate.of(2026, 1, 1));
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (?, 'PC_HR_POS', '人事岗位', 'ACTIVE')", HR_POSITION_ID);
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (?, 'PC_EMP_POS', '当前岗位', 'ACTIVE')", EMPLOYEE_POSITION_ID);
        jdbcTemplate.update("INSERT INTO hr_position (id, code, name, status) VALUES (?, 'PC_TGT_POS', '目标岗位', 'ACTIVE')", TARGET_POSITION_ID);
        jdbcTemplate.update("INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES (?, 'PC_P4', 'P4', 4, 'ACTIVE')", EMPLOYEE_RANK_ID);
        jdbcTemplate.update("INSERT INTO hr_rank (id, code, name, rank_order, status) VALUES (?, 'PC_P5', 'P5', 5, 'ACTIVE')", TARGET_RANK_ID);

        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, employment_status, hire_date)
                VALUES (?, 'PC-HR-001', '人事用户', ?, ?, ?, 'FORMAL', ?)
                """, HR_EMPLOYEE_ID, CURRENT_DEPARTMENT_ID, HR_POSITION_ID, TARGET_RANK_ID, LocalDate.of(2025, 1, 1));
        jdbcTemplate.update("""
                INSERT INTO hr_employee (id, employee_no, name, department_id, position_id, rank_id, manager_employee_id, employment_status, hire_date, probation_start_date, probation_end_date)
                VALUES (?, 'PC-EMP-001', '待异动员工', ?, ?, ?, ?, 'PROBATION', ?, ?, ?)
                """, EMPLOYEE_ID, CURRENT_DEPARTMENT_ID, EMPLOYEE_POSITION_ID, EMPLOYEE_RANK_ID, HR_EMPLOYEE_ID,
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 1), LocalDate.of(2025, 8, 1));

        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version) VALUES (?, 'personnel-hr', ?, ?, 'ACTIVE', 0)",
                HR_USER_ID, new BCryptPasswordEncoder().encode("unused"), HR_EMPLOYEE_ID);
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version) VALUES (?, 'personnel-employee', ?, ?, 'ACTIVE', 0)",
                EMPLOYEE_USER_ID, new BCryptPasswordEncoder().encode("unused"), EMPLOYEE_ID);
        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (99331, ?, 9000002)", HR_USER_ID);

        jdbcTemplate.update("INSERT INTO wf_template (id, code, name, business_type, priority, template_version, status) VALUES (?, 'TEST_PERSONNEL', '测试人事流程', 'PERSONNEL_CHANGE', 10, 1, 'ACTIVE')", TEMPLATE_ID);
        jdbcTemplate.update("INSERT INTO wf_template_node (id, template_id, node_no, node_type, approver_rule) VALUES (99341, ?, 1, 'SPECIFIC_USER', JSON_OBJECT('userId', ?))",
                TEMPLATE_ID, HR_USER_ID);
    }

    @Test
    void draftCanBeEditedAgainAfterWorkflowReturn() throws Exception {
        String token = bearer(HR_USER_ID, "personnel-hr");
        JsonNode created = createTransferChange(token, LocalDate.now());
        long changeId = created.path("data").path("id").asLong();

        mockMvc.perform(patch("/personnel-changes/{id}", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"TRANSFER",
                                  "effectiveDate":"%s",
                                  "reason":"draft edited",
                                  "afterAssignment":{
                                    "departmentId":"99302",
                                    "positionId":"99305",
                                    "rankId":"99307",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"FORMAL"
                                  },
                                  "version":"0"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason").value("draft edited"))
                .andExpect(jsonPath("$.data.version").value("1"));

        mockMvc.perform(post("/personnel-changes/{id}/submit", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.version").value("2"));

        mockMvc.perform(patch("/personnel-changes/{id}", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"TRANSFER",
                                  "effectiveDate":"%s",
                                  "reason":"should fail",
                                  "afterAssignment":{
                                    "departmentId":"99302",
                                    "positionId":"99305",
                                    "rankId":"99307",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"FORMAL"
                                  },
                                  "version":"2"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM hr_personnel_change WHERE id = ?)", Long.class, changeId);
        mockMvc.perform(post("/workflow/tasks/{id}/return", taskId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"comment\":\"need more info\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        mockMvc.perform(patch("/personnel-changes/{id}", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"TRANSFER",
                                  "effectiveDate":"%s",
                                  "reason":"edited after return",
                                  "afterAssignment":{
                                    "departmentId":"99302",
                                    "positionId":"99305",
                                    "rankId":"99307",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"FORMAL"
                                  },
                                  "version":"3"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.reason").value("edited after return"));
    }

    @Test
    void withdrawnChangeCannotBeEffectived() throws Exception {
        String token = bearer(HR_USER_ID, "personnel-hr");
        JsonNode created = createTransferChange(token, LocalDate.now());
        long changeId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/personnel-changes/{id}/submit", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/personnel-changes/{id}/withdraw", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data.version").value("2"));

        mockMvc.perform(post("/personnel-changes/{id}/effective", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void currentDateApprovalImmediatelyWritesEmployeeHistoryAndBlocksDuplicateEffective() throws Exception {
        String token = bearer(HR_USER_ID, "personnel-hr");
        JsonNode created = createConfirmChange(token, LocalDate.now());
        long changeId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/personnel-changes/{id}/submit", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());

        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM hr_personnel_change WHERE id = ?)", Long.class, changeId);
        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"comment\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/personnel-changes/{id}", changeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EFFECTIVE"))
                .andExpect(jsonPath("$.data.version").value("3"));

        mockMvc.perform(get("/employees/{id}/history", EMPLOYEE_ID).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("CONFIRM"));

        assertEquals("FORMAL", jdbcTemplate.queryForObject("SELECT employment_status FROM hr_employee WHERE id = ?", String.class, EMPLOYEE_ID));

        mockMvc.perform(post("/personnel-changes/{id}/effective", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"3\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void futureEffectiveDateStopsAtApprovedAndCannotBeExecutedEarly() throws Exception {
        String token = bearer(HR_USER_ID, "personnel-hr");
        JsonNode created = createTransferChange(token, LocalDate.now().plusDays(1));
        long changeId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/personnel-changes/{id}/submit", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());

        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM hr_personnel_change WHERE id = ?)", Long.class, changeId);
        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"comment\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/personnel-changes/{id}", changeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.version").value("2"));

        assertEquals(CURRENT_DEPARTMENT_ID, jdbcTemplate.queryForObject("SELECT department_id FROM hr_employee WHERE id = ?", Long.class, EMPLOYEE_ID));

        mockMvc.perform(post("/personnel-changes/{id}/effective", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void terminationRequiresConfirmedRequiredHandoverAndDisablesAccountAfterApproval() throws Exception {
        String token = bearer(HR_USER_ID, "personnel-hr");
        String employeeToken = bearer(EMPLOYEE_USER_ID, "personnel-employee");
        JsonNode created = createTerminationChange(token, LocalDate.now());
        long changeId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/personnel-changes/{id}/handover-items", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemType":"ACCOUNT",
                                  "receiverEmployeeId":"99311",
                                  "required":true,
                                  "remark":"disable account"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handoverItems[0].status").value("PENDING"));

        mockMvc.perform(post("/personnel-changes/{id}/submit", changeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\"}"))
                .andExpect(status().isOk());

        long taskId = jdbcTemplate.queryForObject("SELECT id FROM wf_task WHERE instance_id = (SELECT workflow_instance_id FROM hr_personnel_change WHERE id = ?)", Long.class, changeId);
        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"comment\":\"blocked\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        assertEquals("ACTIVE", jdbcTemplate.queryForObject("SELECT status FROM sys_user WHERE id = ?", String.class, EMPLOYEE_USER_ID));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT session_version FROM sys_user WHERE id = ?", Integer.class, EMPLOYEE_USER_ID));

        JsonNode detail = objectMapper.readTree(mockMvc.perform(get("/personnel-changes/{id}", changeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long itemId = detail.path("data").path("handoverItems").get(0).path("id").asLong();

        mockMvc.perform(post("/personnel-changes/{id}/handover-items/{itemId}/confirm", changeId, itemId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"0\",\"remark\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handoverItems[0].status").value("CONFIRMED"));

        mockMvc.perform(post("/workflow/tasks/{id}/approve", taskId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"comment\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertEquals("TERMINATED", jdbcTemplate.queryForObject("SELECT employment_status FROM hr_employee WHERE id = ?", String.class, EMPLOYEE_ID));
        assertEquals("DISABLED", jdbcTemplate.queryForObject("SELECT status FROM sys_user WHERE id = ?", String.class, EMPLOYEE_USER_ID));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT session_version FROM sys_user WHERE id = ?", Integer.class, EMPLOYEE_USER_ID));

        mockMvc.perform(get("/me").header("Authorization", employeeToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_INVALID"));

        String summary = jdbcTemplate.queryForObject("""
                SELECT summary
                FROM sys_operation_log
                WHERE module = 'PERSONNEL'
                  AND object_type = 'PERSONNEL_CHANGE'
                  AND object_id = ?
                ORDER BY created_time DESC, id DESC
                LIMIT 1
                """, String.class, changeId);
        JsonNode audit = objectMapper.readTree(summary);
        assertEquals("TERMINATION", audit.path("changeType").asText());
        assertEquals("DISABLED", audit.path("accountAction").asText());
        org.junit.jupiter.api.Assertions.assertFalse(summary.toLowerCase().contains("password"));
    }

    private JsonNode createTransferChange(String token, LocalDate effectiveDate) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/personnel-changes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"TRANSFER",
                                  "effectiveDate":"%s",
                                  "reason":"组织调整",
                                  "afterAssignment":{
                                    "departmentId":"99302",
                                    "positionId":"99305",
                                    "rankId":"99307",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"FORMAL"
                                  }
                                }
                                """.formatted(effectiveDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode createConfirmChange(String token, LocalDate effectiveDate) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/personnel-changes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"CONFIRM",
                                  "effectiveDate":"%s",
                                  "reason":"试用期转正",
                                  "afterAssignment":{
                                    "departmentId":"99301",
                                    "positionId":"99304",
                                    "rankId":"99306",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"FORMAL",
                                    "hireDate":"2025-02-01",
                                    "probationStartDate":"2025-02-01",
                                    "probationEndDate":"2025-08-01"
                                  }
                                }
                                """.formatted(effectiveDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode createTerminationChange(String token, LocalDate effectiveDate) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/personnel-changes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId":"99312",
                                  "changeType":"TERMINATION",
                                  "effectiveDate":"%s",
                                  "reason":"离职",
                                  "afterAssignment":{
                                    "departmentId":"99301",
                                    "positionId":"99304",
                                    "rankId":"99306",
                                    "managerEmployeeId":"99311",
                                    "employmentStatus":"TERMINATED",
                                    "terminationDate":"%s"
                                  }
                                }
                                """.formatted(effectiveDate, effectiveDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString());
    }

    private String bearer(long userId, String username) {
        return "Bearer " + tokenService.issue(userId, username, 0);
    }
}
