package com.campushub.user.service.impl;

import com.campushub.user.domain.UserProfile;
import com.campushub.user.dto.UpdateUserProfileRequest;
import com.campushub.user.error.UserErrorCode;
import com.campushub.user.error.UserException;
import com.campushub.user.repository.UserProfileRepository;
import com.campushub.user.service.UserProfileService;
import com.campushub.user.vo.UserProfileView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public UserProfileView getProfile(UUID accountId) {
        return userProfileRepository.findByAccountId(accountId)
                .map(this::toView)
                .orElseGet(() -> emptyView(accountId));

    }

    @Override
    @Transactional
    public UserProfileView updateProfile(
            UUID accountId,
            UpdateUserProfileRequest request
    ) {

        UserProfile profile = userProfileRepository
                .findByAccountId(accountId)
                .orElseGet(() -> new UserProfile(accountId));


        profile.replaceProfile(
                request.avatarUrl(),
                request.gender(),
                request.birthDate(),
                request.firstName(),
                request.lastName()
        );

        try{
            UserProfile savedProfile = userProfileRepository.saveAndFlush(profile);

            return toView(savedProfile);
        } catch(DataIntegrityViolationException | ObjectOptimisticLockingFailureException e){
            throw new UserException(UserErrorCode.PROFILE_UPDATE_CONFLICT);
        }
    }

    // --- helper ---
    private UserProfileView toView(UserProfile profile) {
        return new UserProfileView(
                profile.getAccountId(),
                profile.getAvatarUrl(),
                profile.getGender(),
                profile.getBirthDate(),
                profile.getFirstName(),
                profile.getLastName()
        );
    }

    private UserProfileView emptyView(UUID accountId){
        return new UserProfileView(
                accountId,
                null,
                null,
                null,
                null,
                null
        );
    }
}
