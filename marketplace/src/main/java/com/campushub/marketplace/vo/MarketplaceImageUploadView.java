package com.campushub.marketplace.vo;

import java.util.List;
import java.util.UUID;

public record MarketplaceImageUploadView(
        UUID uploadBatchId,
        List<String> imageUrls
) {
    public MarketplaceImageUploadView {
        imageUrls = List.copyOf(imageUrls);
    }
}
