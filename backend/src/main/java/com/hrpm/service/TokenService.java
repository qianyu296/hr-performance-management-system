package com.hrpm.service;


import com.hrpm.common.exception.TokenValidationException;
import com.hrpm.security.AuthenticatedUser;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] signingKey;
    private final Duration ttl;
    private final Clock clock;

    public TokenService(String signingKey, Duration ttl, Clock clock) {
        if (signingKey == null || signingKey.length() < 32) {
            throw new IllegalArgumentException("JWT signing key must contain at least 32 characters");
        }
        this.signingKey = signingKey.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
        this.clock = clock;
    }

    public String issue(long userId, String username, int sessionVersion) {
        if (userId <= 0 || username == null || username.isBlank() || username.contains("\n") || sessionVersion < 0) {
            throw new IllegalArgumentException("Invalid token subject");
        }
        long expiresAt = clock.instant().plus(ttl).getEpochSecond();
        String payload = userId + "\n" + username + "\n" + sessionVersion + "\n" + expiresAt;
        String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + ENCODER.encodeToString(sign(encodedPayload));
    }

    public AuthenticatedUser verify(String token) {
        if (token == null) {
            throw invalid();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalid();
        }
        byte[] suppliedSignature;
        try {
            suppliedSignature = DECODER.decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        if (!MessageDigest.isEqual(sign(parts[0]), suppliedSignature)) {
            throw invalid();
        }
        String[] fields;
        try {
            fields = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8).split("\\n", -1);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        if (fields.length != 4 || fields[1].isBlank()) {
            throw invalid();
        }
        try {
            long userId = Long.parseLong(fields[0]);
            int sessionVersion = Integer.parseInt(fields[2]);
            long expiresAt = Long.parseLong(fields[3]);
            if (userId <= 0 || sessionVersion < 0 || clock.instant().getEpochSecond() >= expiresAt) {
                throw invalid();
            }
            return new AuthenticatedUser(userId, fields[1], sessionVersion);
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign session token", exception);
        }
    }

    private TokenValidationException invalid() {
        return new TokenValidationException("Session token is invalid or expired");
    }
}
