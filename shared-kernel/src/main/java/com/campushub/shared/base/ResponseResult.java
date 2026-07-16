package com.campushub.shared.base;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ResponseResult<T> {

    private final String code;
    private final String message;
    private final T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    private final String requestId;

    private ResponseResult(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ResponseResult<T> success(T data, String requestId) {
        return new ResponseResult<>("0000", "Success", data, requestId);
    }

    public static <T> ResponseResult<T> success(String requestId) {
        return new ResponseResult<>("0000", "Success", null, requestId);
    }

    public static <T> ResponseResult<T> fail(String code, String message, String requestId) {
        return new ResponseResult<>(code, message, null, requestId);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRequestId() {
        return requestId;
    }
}