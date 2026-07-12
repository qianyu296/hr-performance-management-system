package com.hrpm.common;


import com.hrpm.config.SecurityConfiguration;
import com.hrpm.config.TraceIdFilter;
import com.hrpm.controller.HealthController;
import com.hrpm.security.PermissionResolver;
import com.hrpm.security.SessionValidator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(HealthController.class)
@Import({TraceIdFilter.class, SecurityConfiguration.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.security.jwt-signing-key=test-signing-key-at-least-32-characters"
})
class TraceIdWebTests {
    @MockBean
    private SessionValidator sessionValidator;

    @MockBean
    private PermissionResolver permissionResolver;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthResponseContainsGeneratedTraceId() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void callerSuppliedTraceIdIsReturnedInHeaderAndResponse() throws Exception {
        mockMvc.perform(get("/health").header("X-Trace-Id", "trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-001"))
                .andExpect(jsonPath("$.traceId").value("trace-001"));
    }
}
