package com.hrpm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class PerformanceExecutionApiIntegrationTests {
    private static final long HR_USER = 97101L, MANAGER_USER = 97102L, EMPLOYEE_USER = 97103L;
    private static final long HR_EMPLOYEE = 97201L, MANAGER_EMPLOYEE = 97202L, EMPLOYEE = 97203L;
    private static final long TASK = 97601L, TASK_ITEM = 97602L, CYCLE = 97501L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenService tokenService;

    @BeforeEach
    void seedFixture() {
        cleanFixture();
        jdbc.update("INSERT INTO hr_department (id,code,name,path,effective_date,status) VALUES (97110,'PERF_TEST','绩效测试部门','/97110/',?,'ACTIVE')", LocalDate.of(2026, 1, 1));
        jdbc.update("INSERT INTO hr_position (id,code,name,status) VALUES (97111,'PERF_TEST','绩效测试岗位','ACTIVE')");
        employee(HR_EMPLOYEE, "PERF-HR", "绩效人事", null); employee(MANAGER_EMPLOYEE, "PERF-MGR", "绩效主管", null); employee(EMPLOYEE, "PERF-EMP", "绩效员工", MANAGER_EMPLOYEE);
        user(HR_USER, "perf-hr", HR_EMPLOYEE); user(MANAGER_USER, "perf-manager", MANAGER_EMPLOYEE); user(EMPLOYEE_USER, "perf-employee", EMPLOYEE);
        jdbc.update("INSERT INTO sys_role (id,code,name,status) VALUES (97120,'PERF_HR','绩效人事','ACTIVE')");
        jdbc.update("INSERT INTO sys_user_role (id,user_id,role_id) VALUES (97121,?,97120)", HR_USER);
        jdbc.update("INSERT INTO sys_role_menu (id,role_id,menu_id) SELECT 97122,97120,id FROM sys_menu WHERE permission_code='performance:config' AND deleted=0");
        jdbc.update("INSERT INTO perf_metric (id,code,name,metric_type,score_method,score_config,status) VALUES (97301,'PERF_SCORE','绩效评分','QUANTITATIVE','MANUAL','{}','ACTIVE')");
        jdbc.update("INSERT INTO perf_scheme (id,code,name,applicability_rule,status) VALUES (97302,'PERF_SCHEME','绩效方案','{}','ACTIVE')");
        jdbc.update("INSERT INTO perf_scheme_version (id,scheme_id,version_no,evaluation_stages,snapshot,status) VALUES (97303,97302,1,'[\"SELF\",\"MANAGER\"]','{}','ENABLED')");
        jdbc.update("INSERT INTO perf_level_rule (id,scheme_version_id,level_code,min_score,max_score,include_min,include_max) VALUES (97304,97303,'A',90,100,1,1),(97305,97303,'B',0,90,1,0)");
        jdbc.update("INSERT INTO perf_cycle (id,code,name,scheme_version_id,start_date,end_date,self_deadline,manager_deadline,applicability_rule,status) VALUES (?,?,?,97303,?,?,?,?,?,'STARTED')", CYCLE, "PERF_CYCLE", "绩效周期", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), "{}");
        jdbc.update("INSERT INTO perf_task (id,cycle_id,employee_id,manager_employee_id,scheme_version_id,organization_snapshot,status) VALUES (?,?,?, ?,97303,'{}','PENDING_SELF_ASSESSMENT')", TASK, CYCLE, EMPLOYEE, MANAGER_EMPLOYEE);
        jdbc.update("INSERT INTO perf_task_item (id,task_id,metric_snapshot,weight,stage_snapshot) VALUES (?,?,'{\"name\":\"绩效评分\"}',100,'[\"SELF\",\"MANAGER\"]')", TASK_ITEM, TASK);
    }

    @AfterEach void tearDown() { cleanFixture(); }

    @Test
    void selfAssessmentManagerScoreAndPublicationCompletePerformanceCycle() throws Exception {
        mockMvc.perform(post("/performance/tasks/{id}/self-assessment", TASK).header("Authorization", token(EMPLOYEE_USER, "perf-employee"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"items\":[{\"taskItemId\":97602,\"rawScore\":80,\"comment\":\"自评\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING_MANAGER_SCORE"));
        mockMvc.perform(post("/performance/tasks/{id}/manager-score", TASK).header("Authorization", token(MANAGER_USER, "perf-manager"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"items\":[{\"taskItemId\":97602,\"rawScore\":90,\"comment\":\"主管评分\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING_PUBLISH"))
                .andExpect(jsonPath("$.data.totalScore").value("90"));
        mockMvc.perform(post("/performance/cycles/{id}/publish", CYCLE).header("Authorization", token(HR_USER, "perf-hr"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publishedCount").value(1));
        mockMvc.perform(get("/performance/tasks/mine").header("Authorization", token(EMPLOYEE_USER, "perf-employee")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[0].levelCode").value("A"));
    }

    private void employee(long id, String no, String name, Long managerId) { jdbc.update("INSERT INTO hr_employee (id,employee_no,name,department_id,position_id,manager_employee_id,employment_status,hire_date) VALUES (?,?,?,?,?,?, 'FORMAL',?)", id, no, name, 97110, 97111, managerId, LocalDate.of(2025, 1, 1)); }
    private void user(long id, String username, long employeeId) { jdbc.update("INSERT INTO sys_user (id,username,password_hash,employee_id,status,session_version) VALUES (?,?, 'unused',?,'ACTIVE',0)", id, username, employeeId); }
    private String token(long id, String username) { return "Bearer " + tokenService.issue(id, username, 0); }
    private void cleanFixture() {
        jdbc.update("DELETE FROM perf_result_version WHERE result_id IN (SELECT id FROM perf_result WHERE task_id=?)", TASK);
        jdbc.update("DELETE FROM perf_result WHERE task_id=?", TASK);
        jdbc.update("DELETE FROM perf_score WHERE task_item_id=?", TASK_ITEM);
        jdbc.update("DELETE FROM perf_task_item WHERE task_id=?", TASK);
        jdbc.update("DELETE FROM perf_task WHERE id=?", TASK);
        jdbc.update("DELETE FROM perf_cycle WHERE id=?", CYCLE);
        jdbc.update("DELETE FROM perf_level_rule WHERE scheme_version_id=97303");
        jdbc.update("DELETE FROM perf_scheme_version WHERE id=97303");
        jdbc.update("DELETE FROM perf_scheme WHERE id=97302");
        jdbc.update("DELETE FROM perf_metric WHERE id=97301");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id=97120"); jdbc.update("DELETE FROM sys_user_role WHERE role_id=97120"); jdbc.update("DELETE FROM sys_role WHERE id=97120");
        jdbc.update("DELETE FROM sys_user WHERE id BETWEEN 97101 AND 97103"); jdbc.update("DELETE FROM hr_employee WHERE id BETWEEN 97201 AND 97203");
        jdbc.update("DELETE FROM hr_position WHERE id=97111"); jdbc.update("DELETE FROM hr_department WHERE id=97110");
    }
}
