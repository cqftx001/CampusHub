package com.campushub.auth.token;

import com.campushub.auth.config.JwtProperties;
import com.campushub.auth.domain.RoleCode;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAccessTokenIssuer {

    static final String SESSION_ID_CLAIM = "sid";
    static final String ROLES_CLAIM = "roles";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtAccessTokenIssuer(
            JwtProperties jwtProperties,
            JwtSigningKeyProvider signingKeyProvider
    ) {
        this.jwtProperties =
                Objects.requireNonNull(jwtProperties);

        this.signingKey =
                Objects.requireNonNull(
                        signingKeyProvider
                ).signingKey();
    }

    public IssuedAccessToken issue(
            UUID accountId,
            UUID sessionId,
            Set<RoleCode> roles,
            Instant issuedAt,
            Instant sessionExpiresAt
    ) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(roles);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(sessionExpiresAt);

        if (roles.isEmpty()) {
            throw new IllegalArgumentException(
                    "An access token must contain "
                            + "at least one role"
            );
        }

        if (!sessionExpiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "Session expiration must be "
                            + "after JWT issuance"
            );
        }

        Instant configuredExpiration =
                issuedAt.plus(
                        jwtProperties.accessTokenTtl()
                );

        Instant expiresAt =
                configuredExpiration.isBefore(
                        sessionExpiresAt
                )
                        ? configuredExpiration
                        : sessionExpiresAt;

        List<String> roleClaims =
                roles.stream()
                        .map(RoleCode::name)
                        .sorted()
                        .toList();

        UUID tokenId = UUID.randomUUID();

        String token = Jwts.builder()
                .issuer(jwtProperties.issuer())
                .audience()
                .add(jwtProperties.audience())
                .and()
                .subject(accountId.toString())
                .id(tokenId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(
                        SESSION_ID_CLAIM,
                        sessionId.toString()
                )
                .claim(
                        ROLES_CLAIM,
                        roleClaims
                )
                .signWith(
                        signingKey,
                        Jwts.SIG.HS256
                )
                .compact();

        return new IssuedAccessToken(
                token,
                tokenId,
                expiresAt
        );
    }
}