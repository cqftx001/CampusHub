package com.campushub.auth.controller;

import com.campushub.auth.dto.*;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.service.EmailVerificationService;
import com.campushub.auth.vo.CurrentAccountView;
import com.campushub.auth.vo.LoginView;
import com.campushub.auth.vo.RegisterAccountView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.security.AuthenticatedAccount;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.coyote.Request;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseResult<RegisterAccountView>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ){
        RegisterAccountView account = authService.register(request);

        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success(account, requestId));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ResponseResult<Void>> confirmEmailVerification(
            @Valid @RequestBody ConfirmEmailVerificationRequest request,
            HttpServletRequest servletRequest
    ){
        emailVerificationService.confirm(request.token());
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(requestId));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<ResponseResult<Void>> resendEmailVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request,
            HttpServletRequest servletRequest
    ){
        emailVerificationService.resend(request.email());
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ResponseResult.success(requestId));
    }

    /**
     * Login: Identifier / Oauth
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseResult<LoginView>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ){
        LoginClientContext loginClientContext =
                new LoginClientContext(
                        request.getHeader("User-Agent"),
                        request.getRemoteAddr()
                );

        LoginView loginView = authService.login(loginRequest, loginClientContext);

        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(loginView, requestId));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseResult<Void>> logout(
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest servletRequest
    ){
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        authService.logout(account.accountId(), account.sessionId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(requestId));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ResponseResult<LoginView>> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest servletRequest
    ){
        LoginView view = authService.refresh(request);

        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(view, requestId));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseResult<CurrentAccountView>> currentAccount(
            @AuthenticationPrincipal AuthenticatedAccount authenticatedAccount,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);
        CurrentAccountView accountView = authService.getCurrentAccount(authenticatedAccount.accountId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(accountView, requestId));
    }


}
