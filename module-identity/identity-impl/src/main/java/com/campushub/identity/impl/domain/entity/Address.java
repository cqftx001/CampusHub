package com.campushub.identity.impl.domain.entity;

public record Address(
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        String country
) {
}