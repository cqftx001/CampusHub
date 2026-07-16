package com.campushub.auth.controller;

import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.security.JwtPrincipal;
import com.campushub.auth.service.AuthService;
import com.campushub.auth.vo.AuthResponse;
import com.campushub.auth.vo.AuthUserView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseResult<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request
    ) {
        AuthResponse response = authService.register(registerRequest);
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success(response, requestId));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseResult<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        AuthResponse response = authService.login(loginRequest);
        String requestId = RequestUtils.getOrCreateRequestId(request);
        return ResponseEntity
                .ok(ResponseResult.success(response, requestId));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseResult<AuthUserView>> currentUser(
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal,
            HttpServletRequest request
    ) {
        AuthUserView response = authService.currentUser(jwtPrincipal.accountId());
        String requestId = RequestUtils.getOrCreateRequestId(request);
        return ResponseEntity.ok(ResponseResult.success(response, requestId));
    }

    /**
     * TODO: Logout function
     * 后续加入refresh Token 加入logout方法
     */
}
