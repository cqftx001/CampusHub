package com.campushub.auth.service;

import com.campushub.auth.dto.RegisterRequest;
import com.campushub.auth.vo.RegisterAccountView;

public interface AuthService {

    RegisterAccountView register(RegisterRequest request);

}
