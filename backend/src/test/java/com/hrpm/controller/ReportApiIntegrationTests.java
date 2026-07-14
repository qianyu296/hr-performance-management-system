package com.hrpm.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpm.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.jwt-signing-key=test-signing-key-at-least-32-characters")
class ReportApiIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;

    @Test
    void administratorCanReadR1HeadcountAndPerformanceReports() throws Exception {
        String token = "Bearer " + tokenService.issue(9000001L, "admin", 0);
        mockMvc.perform(get("/reports/headcount-by-department").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/reports/performance-level-distribution").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
}
