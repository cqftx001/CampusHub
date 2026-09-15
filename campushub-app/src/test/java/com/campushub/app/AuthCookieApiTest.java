package com.campushub.app;

import com.campushub.auth.dto.LoginClientContext;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RefreshTokenRequest;
import com.campushub.auth.security.RefreshTokenCookieManager;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.token.IssuedAuthTokens;
import com.campushub.shared.security.AuthenticatedAccount;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthCookieApiTest {

    private static final String ACCESS_TOKEN =
            "test-access-token";

    private static final String INITIAL_REFRESH_TOKEN =
            "initial-refresh-token";

    private static final String ROTATED_REFRESH_TOKEN =
            "rotated-refresh-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void loginSetsRefreshTokenInHttpOnlyCookie()
            throws Exception {

        Instant sessionExpiresAt =
                Instant.now().plus(30, ChronoUnit.DAYS);

        when(authService.login(
                any(LoginRequest.class),
                any(LoginClientContext.class)
        )).thenReturn(new IssuedAuthTokens(
                ACCESS_TOKEN,
                INITIAL_REFRESH_TOKEN,
                "Bearer",
                900,
                sessionExpiresAt
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "student@example.com",
                                  "password": "CampusHub!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().value(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        INITIAL_REFRESH_TOKEN
                ))
                .andExpect(cookie().httpOnly(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        true
                ))
                .andExpect(cookie().secure(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        false
                ))
                .andExpect(cookie().path(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        RefreshTokenCookieManager.COOKIE_PATH
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("HttpOnly"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/api/auth")
                        )
                ))
                .andExpect(jsonPath("$.data.accessToken")
                        .value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds")
                        .value(900))
                .andExpect(jsonPath("$.data.sessionExpiresAt")
                        .value(sessionExpiresAt.toString()))
                .andExpect(jsonPath("$.data.refreshToken")
                        .doesNotExist());

        verify(authService).login(
                any(LoginRequest.class),
                any(LoginClientContext.class)
        );
    }

    @Test
    void refreshReadsTokenFromCookieAndRotatesCookie()
            throws Exception {

        Instant sessionExpiresAt =
                Instant.now().plus(30, ChronoUnit.DAYS);

        when(authService.refresh(
                any(RefreshTokenRequest.class)
        )).thenReturn(new IssuedAuthTokens(
                "rotated-access-token",
                ROTATED_REFRESH_TOKEN,
                "Bearer",
                900,
                sessionExpiresAt
        ));

        Cookie refreshCookie = new Cookie(
                RefreshTokenCookieManager.COOKIE_NAME,
                INITIAL_REFRESH_TOKEN
        );

        mockMvc.perform(post("/api/auth/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().value(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        ROTATED_REFRESH_TOKEN
                ))
                .andExpect(cookie().httpOnly(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        true
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        allOf(
                                containsString("HttpOnly"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/api/auth")
                        )
                ))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("rotated-access-token"))
                .andExpect(jsonPath("$.data.refreshToken")
                        .doesNotExist());

        verify(authService).refresh(
                argThat(request ->
                        INITIAL_REFRESH_TOKEN.equals(
                                request.refreshToken()
                        )
                )
        );
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized()
            throws Exception {

        mockMvc.perform(post("/api/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_1015"));

        verifyNoInteractions(authService);
    }

    @Test
    void oversizedRefreshCookieReturnsUnauthorized()
            throws Exception {

        String oversizedToken =
                "a".repeat(
                        RefreshTokenRequest
                                .MAXIMUM_TOKEN_LENGTH + 1
                );

        Cookie refreshCookie = new Cookie(
                RefreshTokenCookieManager.COOKIE_NAME,
                oversizedToken
        );

        mockMvc.perform(post("/api/auth/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_1015"));

        verifyNoInteractions(authService);
    }

    @Test
    void logoutRevokesSessionAndClearsRefreshCookie()
            throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/logout")
                        .with(authenticatedAs(
                                accountId,
                                sessionId
                        )))
                .andExpect(status().isOk())
                .andExpect(cookie().value(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        ""
                ))
                .andExpect(cookie().maxAge(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        0
                ))
                .andExpect(cookie().httpOnly(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        true
                ))
                .andExpect(cookie().path(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        RefreshTokenCookieManager.COOKIE_PATH
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));

        verify(authService).logout(accountId, sessionId);
    }

    private RequestPostProcessor authenticatedAs(
            UUID accountId,
            UUID sessionId
    ) {
        AuthenticatedAccount principal =
                new AuthenticatedAccount(
                        accountId,
                        sessionId,
                        Set.of("USER")
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                )
                        )
                );

        return SecurityMockMvcRequestPostProcessors
                .authentication(authentication);
    }
}