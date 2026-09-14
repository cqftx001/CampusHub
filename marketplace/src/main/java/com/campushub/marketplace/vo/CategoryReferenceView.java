package com.campushub.marketplace.vo;

public record CategoryReferenceView(
        String slug,
        String displayName,
        String parentSlug,
        String parentDisplayName
) {
}
