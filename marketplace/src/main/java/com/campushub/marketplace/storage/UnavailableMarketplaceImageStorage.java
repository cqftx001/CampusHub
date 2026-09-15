package com.campushub.marketplace.storage;

import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;

import java.io.InputStream;
import java.util.UUID;

public class UnavailableMarketplaceImageStorage implements MarketplaceImageStorage {

    @Override
    public StoredMarketplaceImage store(
            UUID accountId,
            UUID uploadBatchId,
            UUID imageId,
            String extension,
            String contentType,
            long contentLength,
            InputStream content
    ) {
        throw new MarketplaceException(MarketplaceErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }

    @Override
    public void deleteBestEffort(String objectKey) {
        // No object was stored while OSS was disabled.
    }
}
