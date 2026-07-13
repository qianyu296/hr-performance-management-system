package com.hrpm.service;


import com.hrpm.common.exception.TokenValidationException;
import com.hrpm.security.AuthenticatedUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenServiceTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC);
    private final TokenService tokenService = new TokenService("test-signing-key-at-least-32-characters", Duration.ofMinutes(15), clock);

    @Test
    void issuesAndVerifiesTokenPayload() {
        String token = tokenService.issue(42L, "alice", 3);

        AuthenticatedUser user = tokenService.verify(token);

        assertEquals(42L, user.userId());
        assertEquals("alice", user.username());
        assertEquals(3, user.sessionVersion());
    }

    @Test
    void rejectsTokenWhoseSignatureWasChanged() {
        String token = tokenService.issue(42L, "alice", 3);
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThrows(TokenValidationException.class, () -> tokenService.verify(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        TokenService shortLived = new TokenService(
                "test-signing-key-at-least-32-characters",
                Duration.ofSeconds(-1),
                clock);

        String token = shortLived.issue(42L, "alice", 3);

        assertThrows(TokenValidationException.class, () -> shortLived.verify(token));
    }

    @Test
    void rejectsRefreshTokenForBusinessRequest() {
        String refreshToken = tokenService.issueRefresh(42L, "alice", 3);

        assertThrows(TokenValidationException.class, () -> tokenService.verify(refreshToken));
        assertEquals(42L, tokenService.verifyRefresh(refreshToken).userId());
    }
}
