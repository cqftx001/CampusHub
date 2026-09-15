package com.campushub.marketplace.controller;

import com.campushub.marketplace.service.MarketplaceImageService;
import com.campushub.marketplace.vo.MarketplaceImageUploadView;
import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.security.AuthenticatedAccount;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/images")
public class MarketplaceImageController {

    private final MarketplaceImageService imageService;

    public MarketplaceImageController(MarketplaceImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseResult<MarketplaceImageUploadView>> uploadImages(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestPart(name = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        MarketplaceImageUploadView upload = imageService.uploadListingImages(
                account.accountId(),
                files
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success(upload, requestId));
    }
}
