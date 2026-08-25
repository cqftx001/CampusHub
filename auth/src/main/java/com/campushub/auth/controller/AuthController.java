package com.campushub.auth.controller;

import com.campushub.auth.dto.ConfirmEmailVerificationRequest;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.dto.ResendEmailVerificationRequest;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.service.EmailVerificationService;
import com.campushub.auth.vo.RegisterAccountView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ResponseResult<Void>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ){
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(null, requestId));
    }



}
