package com.campushub.marketplace.service;

import com.campushub.marketplace.vo.MarketplaceImageUploadView;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MarketplaceImageService {

    MarketplaceImageUploadView uploadListingImages(
            UUID accountId,
            List<MultipartFile> files
    );
}
