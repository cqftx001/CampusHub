package com.campushub.user.controller;

import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.security.AuthenticatedAccount;
import com.campushub.shared.utils.RequestUtils;
import com.campushub.user.dto.UpdateUserProfileRequest;
import com.campushub.user.service.UserProfileService;
import com.campushub.user.vo.UserProfileView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(
            UserProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ResponseResult<UserProfileView>> getProfile(
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest servletRequest
    ) {
        UserProfileView profile =
                userProfileService.getProfile(account.accountId());

        String requestId =
                RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity.ok(
                ResponseResult.success(profile, requestId)
        );
    }

    @PutMapping
    public ResponseEntity<ResponseResult<UserProfileView>> updateProfile(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody UpdateUserProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        UserProfileView profile =
                userProfileService.updateProfile(
                        account.accountId(),
                        request
                );

        String requestId =
                RequestUtils.getOrCreateRequestId(servletRequest);

        return ResponseEntity.ok(
                ResponseResult.success(profile, requestId)
        );
    }
}