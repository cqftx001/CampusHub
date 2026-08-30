package com.campushub.auth.token;

import com.campushub.auth.config.JwtProperties;
import com.campushub.auth.domain.RoleCode;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.error.AuthException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenTest {

    private static final String ISSUER =
            "campushub-test";

    private static final String AUDIENCE =
            "campushub-api";

    private static final String SIGNING_SECRET =
            encodeSecret(
                    "campushub-primary-signing-key-"
                            + "32-bytes-minimum"
            );

    private static final String DIFFERENT_SECRET =
            encodeSecret(
                    "campushub-different-signing-key-"
                            + "32-bytes-minimum"
            );

    private static final Instant ISSUED_AT =
            Instant.parse(
                    "2026-08-26T12:00:00Z"
            );

    private static final Instant SESSION_EXPIRES_AT =
            ISSUED_AT.plus(
                    Duration.ofDays(30)
            );

    @Test
    void issuedAccessTokenCanBeParsed() {
        JwtProperties properties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtAccessTokenIssuer issuer =
                createIssuer(properties);

        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        IssuedAccessToken issuedToken =
                issuer.issue(
                        accountId,
                        sessionId,
                        Set.of(
                                RoleCode.USER,
                                RoleCode.MANAGER
                        ),
                        ISSUED_AT,
                        SESSION_EXPIRES_AT
                );

        JwtAccessTokenParser parser =
                createParser(
                        properties,
                        ISSUED_AT.plusSeconds(1)
                );

        ParsedAccessToken parsedToken =
                parser.parse(
                        issuedToken.value()
                );

        assertThat(parsedToken.accountId())
                .isEqualTo(accountId);

        assertThat(parsedToken.sessionId())
                .isEqualTo(sessionId);

        assertThat(parsedToken.tokenId())
                .isEqualTo(
                        issuedToken.tokenId()
                );

        assertThat(parsedToken.roles())
                .containsExactlyInAnyOrder(
                        RoleCode.USER,
                        RoleCode.MANAGER
                );

        assertThat(issuedToken.expiresAt())
                .isEqualTo(
                        ISSUED_AT.plus(
                                Duration.ofMinutes(15)
                        )
                );
    }

    @Test
    void accessTokenExpirationIsCappedBySessionExpiration() {
        JwtProperties properties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        Instant sessionExpiresSoon =
                ISSUED_AT.plus(
                        Duration.ofMinutes(2)
                );

        IssuedAccessToken issuedToken =
                createIssuer(properties).issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        sessionExpiresSoon
                );

        assertThat(issuedToken.expiresAt())
                .isEqualTo(sessionExpiresSoon);

        JwtAccessTokenParser parser =
                createParser(
                        properties,
                        ISSUED_AT.plusSeconds(1)
                );

        assertThat(
                parser.parse(
                        issuedToken.value()
                )
        ).isNotNull();
    }

    @Test
    void accessTokenCannotBeIssuedForExpiredSession() {
        JwtAccessTokenIssuer issuer =
                createIssuer(
                        properties(
                                ISSUER,
                                AUDIENCE,
                                SIGNING_SECRET
                        )
                );

        assertThatThrownBy(() ->
                issuer.issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        ISSUED_AT
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Session expiration"
                );
    }

    @Test
    void expiredAccessTokenIsRejected() {
        JwtProperties properties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        IssuedAccessToken issuedToken =
                createIssuer(properties).issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        SESSION_EXPIRES_AT
                );

        JwtAccessTokenParser parser =
                createParser(
                        properties,
                        ISSUED_AT.plus(
                                Duration.ofMinutes(16)
                        )
                );

        assertInvalidToken(() ->
                parser.parse(
                        issuedToken.value()
                )
        );
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtProperties expectedProperties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtProperties differentKeyProperties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        DIFFERENT_SECRET
                );

        IssuedAccessToken issuedToken =
                createIssuer(
                        differentKeyProperties
                ).issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        SESSION_EXPIRES_AT
                );

        JwtAccessTokenParser parser =
                createParser(
                        expectedProperties,
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(
                        issuedToken.value()
                )
        );
    }

    @Test
    void tokenFromDifferentIssuerIsRejected() {
        JwtProperties expectedProperties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtProperties differentIssuerProperties =
                properties(
                        "unexpected-issuer",
                        AUDIENCE,
                        SIGNING_SECRET
                );

        IssuedAccessToken issuedToken =
                createIssuer(
                        differentIssuerProperties
                ).issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        SESSION_EXPIRES_AT
                );

        JwtAccessTokenParser parser =
                createParser(
                        expectedProperties,
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(
                        issuedToken.value()
                )
        );
    }

    @Test
    void tokenForDifferentAudienceIsRejected() {
        JwtProperties expectedProperties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtProperties differentAudienceProperties =
                properties(
                        ISSUER,
                        "another-api",
                        SIGNING_SECRET
                );

        IssuedAccessToken issuedToken =
                createIssuer(
                        differentAudienceProperties
                ).issue(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Set.of(RoleCode.USER),
                        ISSUED_AT,
                        SESSION_EXPIRES_AT
                );

        JwtAccessTokenParser parser =
                createParser(
                        expectedProperties,
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(
                        issuedToken.value()
                )
        );
    }

    @Test
    void blankAccessTokenIsRejected() {
        JwtProperties properties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtAccessTokenParser parser =
                createParser(
                        properties,
                        ISSUED_AT
                );

        assertInvalidToken(() ->
                parser.parse(" ")
        );
    }

    @Test
    void tokenWithoutSessionIdIsRejected() {
        String token = createSignedToken(
                ISSUED_AT,
                null,
                List.of(RoleCode.USER.name())
        );

        JwtAccessTokenParser parser =
                createParser(
                        properties(
                                ISSUER,
                                AUDIENCE,
                                SIGNING_SECRET
                        ),
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(token)
        );
    }

    @Test
    void tokenWithoutRolesIsRejected() {
        String token = createSignedToken(
                ISSUED_AT,
                UUID.randomUUID().toString(),
                null
        );

        JwtAccessTokenParser parser =
                createParser(
                        properties(
                                ISSUER,
                                AUDIENCE,
                                SIGNING_SECRET
                        ),
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(token)
        );
    }

    @Test
    void tokenWithUnknownRoleIsRejected() {
        String token = createSignedToken(
                ISSUED_AT,
                UUID.randomUUID().toString(),
                List.of("SUPER_ADMIN")
        );

        JwtAccessTokenParser parser =
                createParser(
                        properties(
                                ISSUER,
                                AUDIENCE,
                                SIGNING_SECRET
                        ),
                        ISSUED_AT.plusSeconds(1)
                );

        assertInvalidToken(() ->
                parser.parse(token)
        );
    }

    @Test
    void tokenIssuedBeyondClockSkewIsRejected() {
        Instant futureIssueTime =
                ISSUED_AT.plusSeconds(31);

        String token = createSignedToken(
                futureIssueTime,
                UUID.randomUUID().toString(),
                List.of(RoleCode.USER.name())
        );

        JwtAccessTokenParser parser =
                createParser(
                        properties(
                                ISSUER,
                                AUDIENCE,
                                SIGNING_SECRET
                        ),
                        ISSUED_AT
                );

        assertInvalidToken(() ->
                parser.parse(token)
        );
    }

    private JwtAccessTokenIssuer createIssuer(
            JwtProperties properties
    ) {
        JwtSigningKeyProvider signingKeyProvider =
                new JwtSigningKeyProvider(
                        properties
                );

        return new JwtAccessTokenIssuer(
                properties,
                signingKeyProvider
        );
    }

    private JwtAccessTokenParser createParser(
            JwtProperties properties,
            Instant currentTime
    ) {
        JwtSigningKeyProvider signingKeyProvider =
                new JwtSigningKeyProvider(
                        properties
                );

        Clock clock = Clock.fixed(
                currentTime,
                ZoneOffset.UTC
        );

        return new JwtAccessTokenParser(
                properties,
                signingKeyProvider,
                clock
        );
    }

    private JwtProperties properties(
            String issuer,
            String audience,
            String secret
    ) {
        return new JwtProperties(
                issuer,
                audience,
                secret,
                Duration.ofMinutes(15)
        );
    }

    private void assertInvalidToken(
            Runnable action
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AuthException.class)
                .satisfies(exception ->
                        assertThat(
                                ((AuthException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                AuthErrorCode
                                        .ACCESS_TOKEN_INVALID
                        )
                );
    }

    private static String encodeSecret(
            String secret
    ) {
        return Base64.getEncoder()
                .encodeToString(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    private String createSignedToken(
            Instant issuedAt,
            String sessionId,
            Object roles
    ) {
        JwtProperties properties =
                properties(
                        ISSUER,
                        AUDIENCE,
                        SIGNING_SECRET
                );

        JwtSigningKeyProvider signingKeyProvider =
                new JwtSigningKeyProvider(
                        properties
                );

        JwtBuilder builder = Jwts.builder()
                .issuer(ISSUER)
                .audience()
                .add(AUDIENCE)
                .and()
                .subject(
                        UUID.randomUUID().toString()
                )
                .id(
                        UUID.randomUUID().toString()
                )
                .issuedAt(
                        Date.from(issuedAt)
                )
                .expiration(
                        Date.from(
                                issuedAt.plus(
                                        Duration.ofMinutes(15)
                                )
                        )
                );

        if (sessionId != null) {
            builder.claim(
                    JwtAccessTokenIssuer
                            .SESSION_ID_CLAIM,
                    sessionId
            );
        }

        if (roles != null) {
            builder.claim(
                    JwtAccessTokenIssuer.ROLES_CLAIM,
                    roles
            );
        }

        return builder
                .signWith(
                        signingKeyProvider.signingKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }
}