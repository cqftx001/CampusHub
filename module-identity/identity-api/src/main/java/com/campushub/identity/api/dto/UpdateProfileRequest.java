package com.campushub.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新用户资料请求")
public record UpdateProfileRequest(
        String firstName, String lastName, String phone,
        String schoolId, String major, String graduationYear,
        String avatarUrl, String bio, String preferredContactMethod,
        String addressLine1, String addressLine2, String city,
        String state, String zipCode, String country
) {}
