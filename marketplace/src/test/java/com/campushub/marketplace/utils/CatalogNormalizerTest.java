package com.campushub.marketplace.utils;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogNormalizerTest {

    @Test
    void uppercaseSlugIsNormalized() {
        String normalized =
                CatalogNormalizer.normalizeSlug(
                        "  NVIDIA  "
                );

        assertThat(normalized)
                .isEqualTo("nvidia");
    }

    @Test
    void hyphenatedSlugIsAccepted() {
        String normalized =
                CatalogNormalizer.normalizeSlug(
                        "  GRAPHIC-CARDS  "
                );

        assertThat(normalized)
                .isEqualTo("graphic-cards");
    }

    @Test
    void slugContainingSpacesIsRejected() {
        MarketplaceException exception =
                assertThrows(
                        MarketplaceException.class,
                        () -> CatalogNormalizer.normalizeSlug(
                                "graphic cards"
                        )
                );

        assertEquals(
                MarketplaceErrorCode.INVALID_CATALOG_SLUG,
                exception.getErrorCode()
        );
    }

    @Test
    void blankDisplayNameIsRejected() {
        assertThatThrownBy(
                () -> CatalogNormalizer.normalizeDisplayName(
                        "   "
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Display name cannot be empty"
                );
    }

    @Test
    void negativeDisplayOrderIsRejected() {
        assertThatThrownBy(
                () -> CatalogNormalizer.requireDisplayOrder(
                        -1
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Display order cannot be negative"
                );
    }
}