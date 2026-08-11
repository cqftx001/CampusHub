package com.campushub.auth.controller;

import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    /**
     * Email Service: confirm / resend email verification
     */
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<ResponseResult<AuthAccountView>> confirmEmailVerification(

    ){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(null, null));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<ResponseResult<AuthAccountView>> resendEmailVerification(){
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ResponseResult.success(null, null));
    }

    /**
     * Login: Identifier / Oauth
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseResult<AuthAccountView>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ){
        String requestId = RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseResult.success(null, requestId));
    }



}
