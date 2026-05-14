package com.campushub.shared.result;

import com.campushub.shared.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult<T> {

    private static final String SUCCESS_CODE = "0";
    private static final String SUCCESS_MESSAGE = "Success";

    private final String code;
    private final String message;
    private final T data;
    private final Instant timestamp;
    private final String requestId;

    // Constructor
    private ResponseResult(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
        this.requestId = requestId;
    }

    // ==================== 成功响应工厂方法 ====================
    public static <T> ResponseResult<T> success(String requestId) {
        return new ResponseResult<>(
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                null,
                requestId
        );
    }

    public static <T> ResponseResult<T> success(T data, String requestId) {
        return new ResponseResult<>(
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data,
                requestId
        );
    }

    // ==================== 失败响应工厂方法 ====================
    public static <T> ResponseResult<T> fail(String code, String requestId) {
        return new ResponseResult<>(
                code,
                code,
                null,
                requestId
        );
    }

    public static <T> ResponseResult<T> fail(
            String code,
            String customMessage,
            String requestId
    ) {
        return new ResponseResult<>(
                code,
                customMessage,
                null,
                requestId
        );
    }

    public static <T> ResponseResult<T> fail(
            ErrorCode errorCode,
            String customMessage
    ) {
        return fail(errorCode, customMessage, null);
    }

    public static <T> ResponseResult<T> fail(
            ErrorCode errorCode,
            String customMessage,
            String requestId
    ) {
        return new ResponseResult<>(
                errorCode.getCode(),
                customMessage,
                null,
                requestId
        );
    }

    public static <T> ResponseResult<T> fail(
            ErrorCode errorCode,
            String customMessage,
            T data,
            String requestId
    ) {
        return new ResponseResult<>(
                errorCode.getCode(),
                customMessage,
                data,
                requestId
        );
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRequestId() {
        return requestId;
    }
}
