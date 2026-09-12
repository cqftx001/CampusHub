package com.campushub.auth.token;

import com.campushub.auth.domain.RefreshToken;
import com.campushub.auth.utils.SecureTokenGenerator;
import com.campushub.auth.utils.Sha256Hasher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class RefreshTokenIssuer {

    private final SecureTokenGenerator tokenGenerator;
    private final Sha256Hasher sha256Hasher;

    public RefreshTokenIssuer(
            SecureTokenGenerator tokenGenerator,
            Sha256Hasher sha256Hasher
    ) {
        this.tokenGenerator = tokenGenerator;
        this.sha256Hasher = sha256Hasher;
    }

    public IssuedRefreshToken issue(
            UUID sessionId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(expiresAt);

        String rawToken = tokenGenerator.generate();

        RefreshToken entity = new RefreshToken(
                sessionId,
                hash(rawToken),
                issuedAt,
                expiresAt
        );

        return new IssuedRefreshToken(
                rawToken,
                entity
        );
    }

    public String hash(String rawToken) {
        return sha256Hasher.hash(
                Objects.requireNonNull(
                        rawToken,
                        "rawToken must not be null"
                )
        );
    }
}