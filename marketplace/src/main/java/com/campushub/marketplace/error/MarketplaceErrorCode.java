package com.campushub.marketplace.error;

import com.campushub.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MarketplaceErrorCode implements ErrorCode {

    LISTING_NOT_FOUND(
            "MARKETPLACE_1002",
            "Listing was not found",
            HttpStatus.NOT_FOUND
    ),

    LISTING_ACCESS_DENIED(
            "MARKETPLACE_1003",
            "The current account does not own this listing",
            HttpStatus.FORBIDDEN
    ),

    INVALID_STATUS_TRANSITION(
            "MARKETPLACE_1004",
            "Listing status transition is not allowed",
            HttpStatus.CONFLICT
    ),

    LISTING_UPDATE_CONFLICT(
            "MARKETPLACE_1005",
            "Listing was updated concurrently",
            HttpStatus.CONFLICT
    ),

    INVALID_CATALOG_SLUG(
            "MARKETPLACE_1006",
            "Catalog slug is invalid",
            HttpStatus.BAD_REQUEST
    ),

    CATEGORY_NOT_FOUND(
            "MARKETPLACE_1007",
            "Category was not found",
            HttpStatus.NOT_FOUND
    ),

    CATEGORY_NOT_AVAILABLE(
            "MARKETPLACE_1008",
            "Category is not available for listing",
            HttpStatus.BAD_REQUEST
    ),

    BRAND_NOT_FOUND(
            "MARKETPLACE_1009",
            "Brand was not found",
            HttpStatus.NOT_FOUND
    ),

    BRAND_NOT_AVAILABLE(
            "MARKETPLACE_1010",
            "Brand is not available for listing",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_LISTING_DETAILS(
            "MARKETPLACE_1011",
            "Listing details are invalid",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_LISTING_FILTERS(
            "MARKETPLACE_1012",
            "Listing filters are invalid",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_IMAGE_UPLOAD(
            "MARKETPLACE_1013",
            "Image upload is invalid",
            HttpStatus.BAD_REQUEST
    ),

    IMAGE_STORAGE_UNAVAILABLE(
            "MARKETPLACE_1014",
            "Image storage is temporarily unavailable",
            HttpStatus.SERVICE_UNAVAILABLE
    )

    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    MarketplaceErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
}
