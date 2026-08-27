package com.campushub.auth.token;

import com.campushub.auth.config.JwtProperties;
import com.campushub.auth.domain.RoleCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Component
public class JwtAccessTokenIssuer {

    static final String SESSION_ID_CLAIM = "sid";
    static final String ROLES_CLAIM = "roles";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtAccessTokenIssuer(JwtProperties properties, JwtSigningKeyProvider signingKeyProvider) {
        this.jwtProperties = Objects.requireNonNull(properties);
        this.signingKey = Objects.requireNonNull(signingKeyProvider).signingKey();
    }

    public IssuedAccessToken issue(
            UUID accountId,
            UUID sessionId,
            Set<RoleCode> roles,
            Instant issuedAt
    ) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(roles);
        Objects.requireNonNull(issuedAt);

        if (roles.isEmpty()) {
            throw new IllegalArgumentException(
                    "An access token must contain at least one role"
            );
        }

        Instant expiresAt =
                issuedAt.plus(jwtProperties.accessTokenTtl());

        List<String> roleClaims = roles.stream()
                .map(RoleCode::name)
                .sorted()
                .toList();

        String token = Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(accountId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(SESSION_ID_CLAIM, sessionId.toString())
                .claim(ROLES_CLAIM, roleClaims)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new IssuedAccessToken(token, expiresAt);
    }
}
