package com.campushub.user.dto;

import com.campushub.user.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserProfileRequest(

        @Size(
                max = 2048,
                message = "Avatar URL must be at most 2048 characters"
        )
        @Pattern(
                regexp = "^https://\\S+$",
                message = "Avatar URL must use HTTPS"
        )
        String avatarUrl,

        Gender gender,

        @Past(
                message = "Birth date must be in the past"
        )
        LocalDate birthDate,

        @Size(
                max = 100,
                message = "First name must be at most 100 characters"
        )
        String firstName,

        @Size(
                max = 100,
                message = "Last name must be at most 100 characters"
        )
        String lastName
) {
}
