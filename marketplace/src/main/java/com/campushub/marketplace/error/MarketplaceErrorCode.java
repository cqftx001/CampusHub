package com.campushub.marketplace.error;

import com.campushub.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MarketplaceErrorCode implements ErrorCode {

    INVALID_LABEL_SELECTION(
            "MARKETPLACE_1001",
            "Label selection is invalid",
            HttpStatus.BAD_REQUEST
    ),

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
