package com.hrpm.security;


import com.hrpm.service.TokenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class TokenAuthenticationFilterTests {
    private final TokenService tokenService = new TokenService(
            "test-signing-key-at-least-32-characters",
            Duration.ofMinutes(15),
            Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC));
    private final TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenService, user -> true, userId -> List.of(), new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenEstablishesAuthenticatedUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.addHeader("Authorization", "Bearer " + tokenService.issue(42L, "alice", 3));

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertInstanceOf(AuthenticatedUser.class, principal);
            assertEquals("alice", ((AuthenticatedUser) principal).username());
        });
    }

    @Test
    void invalidBearerTokenReturnsStableAuthenticationError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.addHeader("Authorization", "Bearer invalid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run for invalid tokens");
        });

        assertEquals(401, response.getStatus());
        assertEquals("AUTH_SESSION_INVALID", new ObjectMapper().readTree(response.getContentAsString()).get("code").asText());
    }

    @Test
    void requestWithoutBearerTokenStaysAnonymousForSecurityToHandle() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    @Test
    void disabledOrSessionInvalidatedUserCannotUsePreviouslySignedToken() throws Exception {
        TokenAuthenticationFilter invalidatingFilter = new TokenAuthenticationFilter(tokenService, user -> false, userId -> List.of(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.addHeader("Authorization", "Bearer " + tokenService.issue(42L, "alice", 3));
        MockHttpServletResponse response = new MockHttpServletResponse();

        invalidatingFilter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run for an invalidated session");
        });

        assertEquals(401, response.getStatus());
        assertEquals("AUTH_SESSION_INVALID", new ObjectMapper().readTree(response.getContentAsString()).get("code").asText());
    }
}
