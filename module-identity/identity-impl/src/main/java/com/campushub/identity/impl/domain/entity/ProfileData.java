package com.campushub.identity.impl.domain.entity;

import com.campushub.identity.impl.domain.entity.Address;

public record ProfileData(
        String firstName,
        String lastName,
        String phone,
        String avatarUrl,
        String bio,
        String schoolId,
        String major,
        String graduationYear,
        String preferredContactMethod,
        Address address
) {
}