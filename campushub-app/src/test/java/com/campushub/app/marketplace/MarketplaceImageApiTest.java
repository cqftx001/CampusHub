package com.campushub.app.marketplace;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.service.MarketplaceImageService;
import com.campushub.marketplace.vo.MarketplaceImageUploadView;
import com.campushub.shared.security.AuthenticatedAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceImageApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketplaceImageService imageService;

    @Test
    void uploadWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/marketplace/images")
                        .file(jpeg()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(imageService);
    }

    @Test
    void authenticatedAccountCanUploadImages() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID uploadBatchId = UUID.randomUUID();

        when(imageService.uploadListingImages(
                eq(accountId),
                argThat(files -> files.size() == 1
                        && "listing.jpg".equals(files.getFirst().getOriginalFilename()))
        )).thenReturn(new MarketplaceImageUploadView(
                uploadBatchId,
                List.of("https://assets.example/listing.jpg")
        ));

        mockMvc.perform(multipart("/api/marketplace/images")
                        .file(jpeg())
                        .with(authenticatedAs(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uploadBatchId")
                        .value(uploadBatchId.toString()))
                .andExpect(jsonPath("$.data.imageUrls[0]")
                        .value("https://assets.example/listing.jpg"));

        verify(imageService).uploadListingImages(
                eq(accountId),
                argThat(files -> files.size() == 1)
        );
    }

    @Test
    void invalidImageReturnsMarketplaceValidationError() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(imageService.uploadListingImages(eq(accountId), argThat(files -> true)))
                .thenThrow(new MarketplaceException(
                        MarketplaceErrorCode.INVALID_IMAGE_UPLOAD,
                        "Only JPEG, PNG, and WebP images are supported"
                ));

        mockMvc.perform(multipart("/api/marketplace/images")
                        .file(jpeg())
                        .with(authenticatedAs(accountId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(MarketplaceErrorCode.INVALID_IMAGE_UPLOAD.getCode()))
                .andExpect(jsonPath("$.message")
                        .value("Only JPEG, PNG, and WebP images are supported"));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile(
                "files",
                "listing.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
        );
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
}
