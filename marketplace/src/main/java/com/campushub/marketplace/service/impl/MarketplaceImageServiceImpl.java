package com.campushub.marketplace.service.impl;

import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.service.MarketplaceImageService;
import com.campushub.marketplace.storage.MarketplaceImageStorage;
import com.campushub.marketplace.storage.StoredMarketplaceImage;
import com.campushub.marketplace.vo.MarketplaceImageUploadView;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MarketplaceImageServiceImpl implements MarketplaceImageService {

    public static final long MAXIMUM_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final int IMAGE_HEADER_LENGTH = 12;
    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] RIFF_SIGNATURE = {
            0x52, 0x49, 0x46, 0x46
    };
    private static final byte[] WEBP_SIGNATURE = {
            0x57, 0x45, 0x42, 0x50
    };

    private final MarketplaceImageStorage imageStorage;

    public MarketplaceImageServiceImpl(MarketplaceImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @Override
    public MarketplaceImageUploadView uploadListingImages(
            UUID accountId,
            List<MultipartFile> files
    ) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");

        List<ValidatedImage> images = validateImages(files);
        UUID uploadBatchId = UUID.randomUUID();
        List<StoredMarketplaceImage> storedImages = new ArrayList<>(images.size());

        try {
            for (ValidatedImage image : images) {
                UUID imageId = UUID.randomUUID();

                try (InputStream content = image.file().getInputStream()) {
                    storedImages.add(imageStorage.store(
                            accountId,
                            uploadBatchId,
                            imageId,
                            image.type().extension(),
                            image.type().contentType(),
                            image.file().getSize(),
                            content
                    ));
                }
            }
        } catch (IOException exception) {
            cleanUp(storedImages);
            throw invalidUpload("Image content could not be read");
        } catch (RuntimeException exception) {
            cleanUp(storedImages);
            throw exception;
        }

        List<String> imageUrls = storedImages.stream()
                .map(StoredMarketplaceImage::imageUrl)
                .toList();

        return new MarketplaceImageUploadView(uploadBatchId, imageUrls);
    }

    private List<ValidatedImage> validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw invalidUpload("At least one image is required");
        }

        if (files.size() > Listing.MAXIMUM_IMAGE_COUNT) {
            throw invalidUpload(
                    "A listing can contain at most " + Listing.MAXIMUM_IMAGE_COUNT + " images"
            );
        }

        List<ValidatedImage> images = new ArrayList<>(files.size());

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw invalidUpload("Image files cannot be empty");
            }

            if (file.getSize() > MAXIMUM_IMAGE_SIZE_BYTES) {
                throw invalidUpload("Each image must not exceed 5 MB");
            }

            images.add(new ValidatedImage(file, detectImageType(file)));
        }

        return images;
    }

    private SupportedImageType detectImageType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(IMAGE_HEADER_LENGTH);

            if (startsWith(header, JPEG_SIGNATURE)) {
                return SupportedImageType.JPEG;
            }

            if (startsWith(header, PNG_SIGNATURE)) {
                return SupportedImageType.PNG;
            }

            if (header.length >= IMAGE_HEADER_LENGTH
                    && matchesAt(header, RIFF_SIGNATURE, 0)
                    && matchesAt(header, WEBP_SIGNATURE, 8)) {
                return SupportedImageType.WEBP;
            }
        } catch (IOException exception) {
            throw invalidUpload("Image content could not be read");
        }

        throw invalidUpload("Only JPEG, PNG, and WebP images are supported");
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length
                && matchesAt(value, prefix, 0);
    }

    private boolean matchesAt(byte[] value, byte[] expected, int offset) {
        if (value.length < offset + expected.length) {
            return false;
        }

        return Arrays.equals(
                value,
                offset,
                offset + expected.length,
                expected,
                0,
                expected.length
        );
    }

    private void cleanUp(List<StoredMarketplaceImage> storedImages) {
        storedImages.forEach(image -> imageStorage.deleteBestEffort(image.objectKey()));
    }

    private MarketplaceException invalidUpload(String message) {
        return new MarketplaceException(
                MarketplaceErrorCode.INVALID_IMAGE_UPLOAD,
                message
        );
    }

    private record ValidatedImage(
            MultipartFile file,
            SupportedImageType type
    ) {
    }

    private enum SupportedImageType {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        SupportedImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String extension() {
            return extension;
        }

        public String contentType() {
            return contentType;
        }
    }
}
