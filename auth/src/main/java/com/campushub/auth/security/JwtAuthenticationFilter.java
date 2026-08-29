package com.campushub.auth.security;

import com.campushub.auth.domain.RoleCode;
import com.campushub.auth.error.AuthException;
import com.campushub.auth.token.JwtAccessTokenParser;
import com.campushub.auth.token.ParsedAccessToken;
import com.campushub.shared.security.AuthenticatedAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.campushub.auth.error.AuthErrorCode;
import com.campushub.auth.token.AccessTokenRegistry;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtAccessTokenParser jwtAccessTokenParser;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final AccessTokenRegistry accessTokenRegistry;

    public JwtAuthenticationFilter(
            JwtAccessTokenParser jwtAccessTokenParser,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            AccessTokenRegistry accessTokenRegistry){
        this.jwtAccessTokenParser = jwtAccessTokenParser;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.accessTokenRegistry = accessTokenRegistry;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ParsedAccessToken parsedAccessToken = jwtAccessTokenParser.parse(token);

            boolean currentToken = accessTokenRegistry.isCurrent(
                    parsedAccessToken.accountId(),
                    parsedAccessToken.sessionId(),
                    parsedAccessToken.tokenId()
            );

            if (!currentToken) {
                throw new AuthException(AuthErrorCode.ACCESS_TOKEN_INVALID);
            }

            Set<String> roleNames = parsedAccessToken
                    .roles()
                    .stream()
                    .map(RoleCode::name)
                    .collect(Collectors.toUnmodifiableSet());

            AuthenticatedAccount principal = new AuthenticatedAccount(
                    parsedAccessToken.accountId(),
                    parsedAccessToken.sessionId(),
                    roleNames
            );

            List<SimpleGrantedAuthority> authorities = parsedAccessToken
                    .roles()
                    .stream()
                    .map(RoleCode::name)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();

            restAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid access token", e)
            );
        }

    }

    // --- helper ---
    private String resolveBearerToken(HttpServletRequest request){
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null) return null;

        if (!authorization.regionMatches(true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length())) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).strip();
    }
}
