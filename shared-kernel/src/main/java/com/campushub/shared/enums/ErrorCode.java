package com.campushub.shared.enums;

/**
 * Stable error contract shared by all modules and future services.
 *
 * <p>The status is stored as an int to keep shared-kernel independent from
 * Spring Web while still allowing web adapters to map directly to HTTP.</p>
 */
public interface ErrorCode {

    String getCode();

    String getMessage();

    int getHttpStatus();
}
