package com.campushub.app.marketplace;

import com.campushub.marketplace.dto.ChangeListingStatusRequest;
import com.campushub.marketplace.vo.BrandReferenceView;
import com.campushub.marketplace.vo.CategoryReferenceView;
import com.campushub.marketplace.vo.ListingView;
import com.campushub.marketplace.domain.DeliveryMethod;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.service.ListingService;
import com.campushub.shared.security.AuthenticatedAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceSellerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListingService listingService;

    @Test
    void changeStatusWithoutAuthenticationReturnsUnauthorized() throws Exception {
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/marketplace/listings/{listingId}/status",
                        listingId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "WITHDRAWN",
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(listingService);
    }

    @Test
    void ownerCanChangeListingStatus() throws Exception {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        when(listingService.changeListingStatus(
                eq(sellerAccountId),
                eq(listingId),
                any(ChangeListingStatusRequest.class)
        )).thenReturn(listingView(
                listingId,
                sellerAccountId,
                ListingStatus.WITHDRAWN,
                1L
        ));

        mockMvc.perform(patch(
                        "/api/marketplace/listings/{listingId}/status",
                        listingId
                )
                        .with(authenticatedAs(sellerAccountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "WITHDRAWN",
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(listingId.toString()))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data.version").value(1));

        verify(listingService).changeListingStatus(
                eq(sellerAccountId),
                eq(listingId),
                argThat(request ->
                        request.status() == ListingStatus.WITHDRAWN
                                && request.expectedVersion() == 0L
                )
        );
    }

    @Test
    void changingAnotherSellersListingReturnsForbidden() throws Exception {
        UUID currentAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        when(listingService.changeListingStatus(
                eq(currentAccountId),
                eq(listingId),
                any(ChangeListingStatusRequest.class)
        )).thenThrow(new MarketplaceException(
                MarketplaceErrorCode.LISTING_ACCESS_DENIED
        ));

        mockMvc.perform(patch(
                        "/api/marketplace/listings/{listingId}/status",
                        listingId
                )
                        .with(authenticatedAs(currentAccountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "WITHDRAWN",
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value(MarketplaceErrorCode.LISTING_ACCESS_DENIED.getCode()));
    }

    @Test
    void staleListingVersionReturnsConflict() throws Exception {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        when(listingService.changeListingStatus(
                eq(sellerAccountId),
                eq(listingId),
                any(ChangeListingStatusRequest.class)
        )).thenThrow(new MarketplaceException(MarketplaceErrorCode.LISTING_UPDATE_CONFLICT));

        mockMvc.perform(patch(
                        "/api/marketplace/listings/{listingId}/status",
                        listingId
                )
                        .with(authenticatedAs(sellerAccountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD",
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value(MarketplaceErrorCode.LISTING_UPDATE_CONFLICT.getCode()));
    }

    @Test
    void missingExpectedVersionReturnsBadRequest() throws Exception {
        UUID sellerAccountId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(put(
                        "/api/marketplace/listings/{listingId}",
                        listingId
                )
                        .with(authenticatedAs(sellerAccountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categorySlug": "graphics-cards",
                                  "brandSlug": "NVIDIA",
                                  "title": "RTX 5090 Graphics Card",
                                  "description": "Used graphics card in excellent condition.",
                                  "manufactureYear": 2025,
                                  "condition": "EXCELLENT",
                                  "price": 1800.00,
                                  "location": "Main Campus",
                                  "deliveryMethod": "PICKUP",
                                  "imageUrls": [
                                    "https://example.com/rtx-5090.jpg"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listingService);
    }

    private RequestPostProcessor authenticatedAs(UUID accountId) {
        AuthenticatedAccount principal = new AuthenticatedAccount(
                accountId,
                UUID.randomUUID(),
                Set.of("USER")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private ListingView listingView(
            UUID listingId,
            UUID sellerAccountId,
            ListingStatus status,
            long version
    ) {
        Instant now = Instant.now();

        return new ListingView(
                listingId,
                sellerAccountId,
                new CategoryReferenceView(
                        "graphics-cards",
                        "Graphics Cards",
                        "graphics-cards",
                        "graphics-cards"
                ),
                new BrandReferenceView(
                        "NVIDIA",
                        "nvidia"
                ),
                "RTX 5090 Graphics Card",
                "Used graphics card in excellent condition.",
                2025,
                ListingCondition.GOOD,
                new BigDecimal("1800.00"),
                "USD",
                "Main Campus",
                DeliveryMethod.LOCAL_PICKUP,
                List.of("https://example.com/rtx-5090.jpg"),
                status,
                version,
                now,
                now
        );
    }
}