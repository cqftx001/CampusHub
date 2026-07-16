package com.campushub.auth.service;

import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.vo.AuthResponse;
import com.campushub.auth.vo.AuthUserView;

import java.security.Principal;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthUserView currentUser(UUID accountId);

}
