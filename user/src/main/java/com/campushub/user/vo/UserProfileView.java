package com.campushub.user.vo;

import com.campushub.user.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record UserProfileView(
        UUID accountId,
        String avatarUrl,
        Gender gender,
        LocalDate birthDate,
        String firstName,
        String lastName
) {
}
