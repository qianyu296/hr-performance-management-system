package com.hrpm.config;


import com.hrpm.security.AuthenticatedUser;
import com.hrpm.security.PermissionResolver;
import com.hrpm.security.SessionValidator;
import com.hrpm.service.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.BDDMockito.given;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigurationWebTests.SecuredController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationWebTests.SecuredController.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5174",
        "app.security.jwt-signing-key=test-signing-key-at-least-32-characters"
})
class SecurityConfigurationWebTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockBean
    private SessionValidator sessionValidator;

    @MockBean
    private PermissionResolver permissionResolver;

    @BeforeEach
    void allowTestSession() {
        given(sessionValidator.isValid(org.mockito.ArgumentMatchers.any())).willReturn(true);
    }

    @Test
    void validTokenCanReachProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/secured")
                        .header("Authorization", "Bearer " + tokenService.issue(42L, "alice", 1)))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));
    }

    @Test
    void corsPreflightAllowsConfiguredLocalDevelopmentOrigins() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://127.0.0.1:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5174"));
    }

    @RestController
    static class SecuredController {
        @GetMapping("/secured")
        String secured(@AuthenticationPrincipal AuthenticatedUser user) {
            return user.username();
        }
    }
}
