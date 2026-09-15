package com.campushub.marketplace.storage;

import java.io.InputStream;
import java.util.UUID;

public interface MarketplaceImageStorage {

    StoredMarketplaceImage store(
            UUID accountId,
            UUID uploadBatchId,
            UUID imageId,
            String extension,
            String contentType,
            long contentLength,
            InputStream content
    );

    void deleteBestEffort(String objectKey);
}
