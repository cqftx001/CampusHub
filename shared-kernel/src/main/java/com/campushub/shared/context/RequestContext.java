package com.campushub.shared.context;

import java.util.Optional;
import java.util.UUID;

public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static Optional<String> getRequestId() {
        return Optional.ofNullable(REQUEST_ID.get());
    }

    public static String getOrCreateRequestId() {
        String requestId = REQUEST_ID.get();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            REQUEST_ID.set(requestId);
        }
        return requestId;
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
