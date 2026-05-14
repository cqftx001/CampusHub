package com.campushub.security.filter;

import com.campushub.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        String token = extractToken(request);
        // 没有token -> 放行(可能是公开接口 / 未登陆)
        if(token == null){
            filterChain.doFilter(request, response);
            return;
        }

        try{
            Claims claims = jwtUtils.parseToken(token);
            String userId = claims.getSubject();
            String roles = jwtUtils.extractRoles(claims);

            List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated user: {}, roles: {}", userId, roles);
        } catch(ExpiredJwtException e){
            SecurityContextHolder.clearContext();
            log.debug("Token expired: {}", e.getMessage());
        } catch(JwtException e){
            SecurityContextHolder.clearContext();
            log.warn("Invalid JWT: {}", e.getMessage());
        }

        // 无论验证成功还是失败，都放行到下一个 Filter
        // 成功：SecurityContext 有认证信息，后续正常处理
        // 失败：SecurityContext 为空，如果接口需要认证，Spring Security 会返回 401
        filterChain.doFilter(request, response);
    }

    // -- helper
    private String extractToken(HttpServletRequest request){
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if(header != null && header.startsWith(TOKEN_PREFIX)){
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
