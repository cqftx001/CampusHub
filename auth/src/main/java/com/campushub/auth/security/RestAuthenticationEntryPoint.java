package com.campushub.auth.security;

import com.campushub.auth.error.AuthErrorCode;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.utils.RequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.campushub.auth.error.AuthException;
import com.campushub.shared.error.ErrorCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        // 分类处理ErrorCode
        ErrorCode errorCode = resolveErrorCode(authException);
        String requestId = RequestUtils.getOrCreateRequestId(request);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(), ResponseResult.fail(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        requestId)
        );
    }

    private ErrorCode resolveErrorCode(AuthenticationException authenticationException) {
        Throwable current = authenticationException;

        while (current != null) {
            if (current instanceof AuthException authException) {
                return authException.getErrorCode();
            }

            current = current.getCause();
        }

        return AuthErrorCode.ACCESS_TOKEN_INVALID;
    }
}
