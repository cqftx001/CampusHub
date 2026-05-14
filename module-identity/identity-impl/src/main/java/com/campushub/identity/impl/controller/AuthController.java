package com.campushub.identity.impl.controller;

import com.campushub.identity.api.dto.EmailCodeLoginRequest;
import com.campushub.identity.api.dto.LoginRequest;
import com.campushub.identity.api.dto.RegisterRequest;
import com.campushub.identity.api.dto.SendEmailCodeRequest;
import com.campushub.identity.api.dto.VerifyEmailRequest;
import com.campushub.identity.api.vo.AuthResponse;
import com.campushub.identity.api.vo.UserView;
import com.campushub.identity.impl.service.AuthService;
import com.campushub.identity.impl.service.TokenPair;
import com.campushub.security.config.JwtProperties;
import com.campushub.shared.context.RequestContext;
import com.campushub.shared.result.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and Authorization")
public class AuthController{

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "用户注册", description = "创建账号并发送邮箱验证码，状态为 UNVERIFIED")
    @PostMapping("/register")
    public ResponseEntity<ResponseResult<UserView>> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ){
        UserView userView = authService.register(registerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success(userView, RequestContext.getOrCreateRequestId()));
    }

    @PostMapping("/email/register-code/resend")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "验证码发送成功"),
            @ApiResponse(responseCode = "403", description = "用户状态异常"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "409", description = "邮箱已验证")
    })
    public ResponseEntity<ResponseResult<Void>> resendRegisterCode(
            @Valid @RequestBody SendEmailCodeRequest request
    ) {
        authService.resendRegisterCode(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "邮箱验证", description = "验证注册验证码，激活账号")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "邮箱验证成功"),
            @ApiResponse(responseCode = "400", description = "验证码无效或过期"),
            @ApiResponse(responseCode = "403", description = "用户状态异常"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "409", description = "邮箱已验证")
    })
    @PostMapping("/email/verify")
    public ResponseEntity<ResponseResult<UserView>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        UserView userView = authService.verifyEmail(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(userView, RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "发送邮箱登录验证码")
    @PostMapping("/email/login-code")
    public ResponseEntity<ResponseResult<Void>> sendEmailLoginCode(
            @Valid @RequestBody SendEmailCodeRequest request
    ) {
        authService.sendEmailLoginCode(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "邮箱验证码登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "验证码错误"),
            @ApiResponse(responseCode = "403", description = "用户状态异常")
    })
    @PostMapping("/email/login")
    public ResponseEntity<ResponseResult<AuthResponse>> loginWithEmailCode(
            @Valid @RequestBody EmailCodeLoginRequest request,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.loginWithEmailCode(request);
        setRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(tokenPair.authResponse(), RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "邮箱/用户名 + 密码登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "用户状态异常")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseResult<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ){
        TokenPair tokenPair = authService.login(loginRequest);
        setRefreshTokenCookie(response, tokenPair.refreshToken());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(tokenPair.authResponse(), RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "刷新令牌", description = "从 HttpOnly Cookie 读取 refresh token，实现 token rotation")
    @PostMapping("/session/refresh")
    public ResponseEntity<ResponseResult<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = readRefreshTokenCookie(request);
        TokenPair tokenPair = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, tokenPair.refreshToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(tokenPair.authResponse(), RequestContext.getOrCreateRequestId()));
    }

    @Operation(summary = "退出登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出登录成功"),
            @ApiResponse(responseCode = "401", description = "令牌无效")
    })
    @PostMapping("/session/logout")
    public ResponseEntity<ResponseResult<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = readRefreshTokenCookie(request);
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(RequestContext.getOrCreateRequestId()));
    }

    private String readRefreshTokenCookie(HttpServletRequest request) {
        if(request.getCookies() == null) return null;

        String cookieName = jwtProperties.getRefreshTokenCookieName();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getRefreshTokenCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshTokenCookieSecure())
                .sameSite(jwtProperties.getRefreshTokenCookieSameSite())
                .path(jwtProperties.getRefreshTokenCookiePath())
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getRefreshTokenCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshTokenCookieSecure())
                .sameSite(jwtProperties.getRefreshTokenCookieSameSite())
                .path(jwtProperties.getRefreshTokenCookiePath())
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
