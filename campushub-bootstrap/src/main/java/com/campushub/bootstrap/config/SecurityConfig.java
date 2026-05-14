package com.campushub.bootstrap.config;

import com.campushub.security.filter.JwtAuthenticationFilter;
import com.campushub.shared.context.RequestContext;
import com.campushub.shared.enums.CommonErrorCode;
import com.campushub.shared.enums.ErrorCode;
import com.campushub.shared.result.ResponseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(
                                        response,
                                        objectMapper,
                                        CommonErrorCode.UNAUTHORIZED,
                                        CommonErrorCode.UNAUTHORIZED.getMessage()
                                ))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(
                                        response,
                                        objectMapper,
                                        CommonErrorCode.FORBIDDEN,
                                        CommonErrorCode.FORBIDDEN.getMessage()
                                ))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // OpenAPI / Swagger
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                // Auth: 公开端点（注册、登录、邮箱验证、刷新、登出）
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/email/**",
                                "/api/auth/session/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties props){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.allowedOrigins());
        config.setAllowedMethods(props.allowedMethods());
        config.setAllowedHeaders(props.allowedHeaders());
        config.setExposedHeaders(props.exposedHeaders());
        config.setAllowCredentials(props.allowCredentials());
        config.setMaxAge(props.maxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ErrorCode errorCode,
            String message
    ) throws IOException {
        String requestId = RequestContext.getOrCreateRequestId();
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                ResponseResult.fail(errorCode, message, requestId)
        ));
    }
}
