package com.campushub.user.service;

import com.campushub.user.dto.UpdateUserProfileRequest;
import com.campushub.user.vo.UserProfileView;

import java.util.UUID;

public interface UserProfileService {

    UserProfileView getProfile(UUID accountId);

    UserProfileView updateProfile(UUID accountId, UpdateUserProfileRequest request);
}
