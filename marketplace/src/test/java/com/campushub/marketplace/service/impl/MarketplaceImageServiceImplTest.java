package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.storage.MarketplaceImageStorage;
import com.campushub.marketplace.storage.StoredMarketplaceImage;
import com.campushub.marketplace.vo.MarketplaceImageUploadView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceImageServiceImplTest {

    @Mock
    private MarketplaceImageStorage imageStorage;

    private MarketplaceImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        imageService = new MarketplaceImageServiceImpl(imageStorage);
    }

    @Test
    void validImagesAreUploadedInRequestOrderUsingDetectedTypes() {
        UUID accountId = UUID.randomUUID();
        MockMultipartFile jpeg = image(
                "first.jpg",
                "application/octet-stream",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
        );
        MockMultipartFile png = image(
                "second.png",
                "text/plain",
                new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                }
        );

        when(imageStorage.store(
                eq(accountId),
                any(UUID.class),
                any(UUID.class),
                eq("jpg"),
                eq("image/jpeg"),
                eq(jpeg.getSize()),
                any(InputStream.class)
        )).thenReturn(new StoredMarketplaceImage("first-key", "https://assets.example/first.jpg"));

        when(imageStorage.store(
                eq(accountId),
                any(UUID.class),
                any(UUID.class),
                eq("png"),
                eq("image/png"),
                eq(png.getSize()),
                any(InputStream.class)
        )).thenReturn(new StoredMarketplaceImage("second-key", "https://assets.example/second.png"));

        MarketplaceImageUploadView result = imageService.uploadListingImages(
                accountId,
                List.of(jpeg, png)
        );

        assertThat(result.uploadBatchId()).isNotNull();
        assertThat(result.imageUrls()).containsExactly(
                "https://assets.example/first.jpg",
                "https://assets.example/second.png"
        );

        InOrder order = inOrder(imageStorage);
        order.verify(imageStorage).store(
                eq(accountId),
                eq(result.uploadBatchId()),
                any(UUID.class),
                eq("jpg"),
                eq("image/jpeg"),
                eq(jpeg.getSize()),
                any(InputStream.class)
        );
        order.verify(imageStorage).store(
                eq(accountId),
                eq(result.uploadBatchId()),
                any(UUID.class),
                eq("png"),
                eq("image/png"),
                eq(png.getSize()),
                any(InputStream.class)
        );
    }

    @Test
    void browserContentTypeCannotBypassSignatureValidation() {
        MockMultipartFile fakeJpeg = image(
                "fake.jpg",
                "image/jpeg",
                "not-an-image".getBytes()
        );

        MarketplaceException exception = assertThrows(
                MarketplaceException.class,
                () -> imageService.uploadListingImages(
                        UUID.randomUUID(),
                        List.of(fakeJpeg)
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(MarketplaceErrorCode.INVALID_IMAGE_UPLOAD);
        assertThat(exception)
                .hasMessage("Only JPEG, PNG, and WebP images are supported");
        verifyNoInteractions(imageStorage);
    }

    @Test
    void imageLargerThanFiveMegabytesIsRejectedBeforeStorage() {
        byte[] oversizedImage = new byte[
                (int) MarketplaceImageServiceImpl.MAXIMUM_IMAGE_SIZE_BYTES + 1
        ];
        oversizedImage[0] = (byte) 0xFF;
        oversizedImage[1] = (byte) 0xD8;
        oversizedImage[2] = (byte) 0xFF;

        MarketplaceException exception = assertThrows(
                MarketplaceException.class,
                () -> imageService.uploadListingImages(
                        UUID.randomUUID(),
                        List.of(image("large.jpg", "image/jpeg", oversizedImage))
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(MarketplaceErrorCode.INVALID_IMAGE_UPLOAD);
        assertThat(exception)
                .hasMessage("Each image must not exceed 5 MB");
        verifyNoInteractions(imageStorage);
    }

    @Test
    void storedObjectsAreCleanedUpWhenLaterUploadFails() {
        UUID accountId = UUID.randomUUID();
        MockMultipartFile first = jpeg("first.jpg");
        MockMultipartFile second = jpeg("second.jpg");

        MarketplaceException storageFailure = new MarketplaceException(
                MarketplaceErrorCode.IMAGE_STORAGE_UNAVAILABLE
        );

        when(imageStorage.store(
                eq(accountId),
                any(UUID.class),
                any(UUID.class),
                eq("jpg"),
                eq("image/jpeg"),
                anyLong(),
                any(InputStream.class)
        )).thenReturn(
                new StoredMarketplaceImage("first-key", "https://assets.example/first.jpg")
        ).thenThrow(storageFailure);

        MarketplaceException exception = assertThrows(
                MarketplaceException.class,
                () -> imageService.uploadListingImages(
                        accountId,
                        List.of(first, second)
                )
        );

        assertThat(exception).isSameAs(storageFailure);
        verify(imageStorage).deleteBestEffort("first-key");
        verify(imageStorage, never()).deleteBestEffort("second-key");
    }

    private MockMultipartFile jpeg(String filename) {
        return image(
                filename,
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
        );
    }

    private MockMultipartFile image(
            String filename,
            String contentType,
            byte[] content
    ) {
        return new MockMultipartFile(
                "files",
                filename,
                contentType,
                content
        );
    }
}
