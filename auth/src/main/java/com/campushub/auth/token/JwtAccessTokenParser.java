package com.campushub.auth.token;

import com.campushub.auth.config.JwtProperties;
import com.campushub.auth.domain.RoleCode;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Component
public class JwtAccessTokenParser {

    private static final String SESSION_ID_CLAIM = "sid";
    private static final String ROLES_CLAIM = "roles";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final JwtParser jwtParser;
    private final Clock clock;

    public JwtAccessTokenParser(
            JwtProperties properties,
            JwtSigningKeyProvider signingKeyProvider,
            Clock clock
    ) {

        this.clock = clock;

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKeyProvider.signingKey())
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build();
    }

    public ParsedAccessToken parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        try {
            Jws<Claims> signedClaims = jwtParser.parseSignedClaims(rawToken);

            requireHs256(signedClaims);

            Claims claims = signedClaims.getPayload();

            validateStandardClaims(claims);

            UUID accountId = UUID.fromString(requireText(claims.getSubject()));
            UUID sessionId = UUID.fromString(requireText(claims.get(SESSION_ID_CLAIM)));
            Set<RoleCode> roles = parseRoles(claims);

            return new ParsedAccessToken(accountId, sessionId, roles);
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidToken();
        }
    }

    // --- helper ---
    private Set<RoleCode> parseRoles(Claims claims) {
        Object claim = claims.get(ROLES_CLAIM);
        if (!(claim instanceof Collection<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("Missing or empty roles claim");
        }

        Set<RoleCode> roles = new LinkedHashSet<>();

        for(Object value : values) {
            if (!(value instanceof String role)) {
                throw new IllegalArgumentException("Invalid roles claim");
            }
            roles.add(RoleCode.valueOf(role));
        }
        return roles;
    }

    private String requireText(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required claim");
        }
        return text;
    }

    private void validateStandardClaims(Claims claims) {
        if(claims.getExpiration() == null) throw new IllegalArgumentException("Missing expiration");

        if(claims.getIssuedAt() == null) throw new IllegalArgumentException("Missing issued-at");

        if(claims.getId() == null || claims.getId().isBlank()) throw new IllegalArgumentException("Missing JWT id");

        Instant latestAllowedIssueTime = clock.instant().plusSeconds(CLOCK_SKEW_SECONDS);

        if(claims.getIssuedAt().toInstant().isAfter(latestAllowedIssueTime)) {
            throw new IllegalArgumentException("JWt issued-at is in the future: " + claims.getIssuedAt());
        }
    }

    private void requireHs256(Jws<Claims> signedClaims) {
        String algorithm = signedClaims.getHeader().getAlgorithm();

        if(!Jwts.SIG.HS256.getId().equals(algorithm)) {
            throw new IllegalArgumentException("Unexpected JWT algorithm: " + algorithm);
        }
    }

    private AuthException invalidToken() {
        return new AuthException(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }
}
