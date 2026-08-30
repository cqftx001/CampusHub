package com.campushub.auth.security;

import com.campushub.auth.token.AccessTokenRegistry;
import com.campushub.auth.token.JwtAccessTokenParser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtAccessTokenParser jwtAccessTokenParser;

    @Mock
    private RestAuthenticationEntryPoint
            authenticationEntryPoint;

    @Mock
    private AccessTokenRegistry
            accessTokenRegistry;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtAccessTokenParser,
                authenticationEntryPoint,
                accessTokenRegistry
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/email-verification/confirm",
            "/api/auth/email-verification/resend",
            "/api/auth/token/refresh"
    })
    void publicPostEndpointsSkipJwtAuthentication(
            String path
    ) {
        MockHttpServletRequest request =
                request("POST", path);

        assertThat(
                filter.shouldNotFilter(request)
        ).isTrue();
    }

    @Test
    void protectedEndpointDoesNotSkipJwtAuthentication() {
        MockHttpServletRequest request =
                request(
                        "GET",
                        "/api/auth/me"
                );

        assertThat(
                filter.shouldNotFilter(request)
        ).isFalse();
    }

    @Test
    void differentHttpMethodDoesNotSkipAuthentication() {
        MockHttpServletRequest request =
                request(
                        "GET",
                        "/api/auth/login"
                );

        assertThat(
                filter.shouldNotFilter(request)
        ).isFalse();
    }

    @Test
    void refreshRequestWithStaleBearerStillReachesEndpoint()
            throws Exception {

        MockHttpServletRequest request =
                request(
                        "POST",
                        "/api/auth/token/refresh"
                );

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer expired-or-rotated-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(filterChain).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtAccessTokenParser,
                accessTokenRegistry,
                authenticationEntryPoint
        );
    }

    private MockHttpServletRequest request(
            String method,
            String path
    ) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        path
                );

        request.setServletPath(path);

        return request;
    }
}