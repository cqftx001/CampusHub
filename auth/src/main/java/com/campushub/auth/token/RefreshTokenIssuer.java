package com.campushub.auth.token;

import com.campushub.auth.domain.RefreshToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Component
public class RefreshTokenIssuer {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    public IssuedRefreshToken issue(
            UUID sessionId,
            Instant issuedAt,
            Instant expiresAt
    ){
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(issuedAt);

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        random.nextBytes(tokenBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        RefreshToken entity = new RefreshToken(
                sessionId,
                hash(rawToken),
                issuedAt,
                expiresAt
        );

        return new IssuedRefreshToken(rawToken, entity);
    }

    // --- helper ---
    public String hash(String rawToken) {
        String requiredToken = Objects.requireNonNull(
                rawToken,
                "rawToken must not be null"
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    requiredToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

}
