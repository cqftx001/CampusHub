package com.campushub.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestUtils {

    public static final String REQUEST_ID = "requestId";

    private RequestUtils() {
    }

    public static String getOrCreateRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ID);
        if (requestId != null) {
            return requestId.toString();
        }

        String generated = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, generated);
        return generated;
    }


}