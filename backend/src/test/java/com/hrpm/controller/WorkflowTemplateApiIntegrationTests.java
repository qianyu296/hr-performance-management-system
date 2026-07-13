package com.hrpm.controller;

import com.hrpm.service.TokenService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
class WorkflowTemplateApiIntegrationTests {
    private static final long DEPARTMENT_ID = 88001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        jdbcTemplate.update("""
                INSERT INTO hr_department (id, code, name, path, effective_date, status)
                VALUES (?, 'WORKFLOW_TEST', 'Workflow Test', ?, ?, 'ACTIVE')
                """, DEPARTMENT_ID, "/88001/", LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void administratorCanCreateReadAndUpdateWorkflowTemplate() throws Exception {
        String token = "Bearer " + tokenService.issue(9000001L, "admin", 0);
        String createBody = """
                {
                  "code":"LEAVE_HR",
                  "name":"Leave manager and HR",
                  "businessType":"LEAVE",
                  "priority":20,
                  "templateVersion":1,
                  "status":"ACTIVE",
                  "departmentIds":["88001"],
                  "nodes":[
                    {"nodeNo":1,"nodeType":"SPECIFIC_USER","approverRule":{"userId":9000001}},
                    {"nodeNo":2,"nodeType":"HR","approverRule":{"roleCode":"SUPER_ADMIN"}}
                  ]
                }
                """;

        mockMvc.perform(post("/workflow/templates")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("LEAVE_HR"))
                .andExpect(jsonPath("$.data.departmentIds[0]").value("88001"))
                .andExpect(jsonPath("$.data.nodes[1].nodeType").value("HR"))
                .andExpect(jsonPath("$.data.nodes[1].approverRule.roleCode").value("SUPER_ADMIN"));

        long templateId = jdbcTemplate.queryForObject("SELECT id FROM wf_template WHERE code = 'LEAVE_HR' AND deleted = 0", Long.class);
        mockMvc.perform(get("/workflow/templates/{id}", templateId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes.length()").value(2));

        String updateBody = """
                {
                  "name":"Leave manager and HR v2",
                  "businessType":"LEAVE",
                  "priority":30,
                  "status":"INACTIVE",
                  "departmentIds":[],
                  "nodes":[{"nodeNo":1,"nodeType":"SPECIFIC_USER","approverRule":{"type":"SPECIFIC_USER","userId":9000001}}],
                  "version":"0"
                }
                """;
        mockMvc.perform(put("/workflow/templates/{id}", templateId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Leave manager and HR v2"))
                .andExpect(jsonPath("$.data.priority").value(30))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.nodes.length()").value(1))
                .andExpect(jsonPath("$.data.version").value("1"));
    }

    private void cleanFixtures() {
        jdbcTemplate.update("DELETE FROM wf_template_node WHERE template_id IN (SELECT id FROM wf_template WHERE code = 'LEAVE_HR')");
        jdbcTemplate.update("DELETE FROM wf_template_scope WHERE template_id IN (SELECT id FROM wf_template WHERE code = 'LEAVE_HR')");
        jdbcTemplate.update("DELETE FROM wf_template WHERE code = 'LEAVE_HR'");
        jdbcTemplate.update("DELETE FROM hr_department WHERE id = ?", DEPARTMENT_ID);
    }
}
