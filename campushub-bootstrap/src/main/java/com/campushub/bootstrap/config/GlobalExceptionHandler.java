package com.campushub.bootstrap.config;

import com.campushub.shared.constant.GlobalConstant;
import com.campushub.shared.context.RequestContext;
import com.campushub.shared.enums.CommonErrorCode;
import com.campushub.shared.enums.ErrorCode;
import com.campushub.shared.exception.BaseException;
import com.campushub.shared.result.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResponseResult<Void>> handleBaseException(
            BaseException ex,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("Business exception occurred. requestId={}, code={}, status={}, message={}",
                requestId,
                errorCode.getCode(),
                errorCode.getHttpStatus(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ResponseResult.fail(errorCode.getCode(), ex.getMessage(), requestId));
    }

    // ==================== 框架异常 ====================
    /**
     * @Valid 校验失败 → 400
     *
     * 当 @RequestBody 的字段不满足 @NotBlank、@Email、@Size 等约束时，
     * Spring 抛出 MethodArgumentNotValidException。
     *
     * 我们把所有校验错误拼成一个字符串返回给前端，
     * 格式：「email: Invalid email format; password: Password is required」
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        String requestId = requestId(request);

        log.debug("Validation error. requestId={}, message={}", requestId, message);
        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ResponseResult.fail(
                        CommonErrorCode.VALIDATION_ERROR,
                        message,
                        requestId));
    }

    /**
     * 未实现的功能 → 501
     * 空壳模块抛出的 UnsupportedOperationException。
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ResponseResult<Void>> handleNotImplemented(
            UnsupportedOperationException e,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);

        return ResponseEntity
                .status(CommonErrorCode.NOT_IMPLEMENTED.getHttpStatus())
                .body(ResponseResult.fail(
                        CommonErrorCode.NOT_IMPLEMENTED,
                        CommonErrorCode.NOT_IMPLEMENTED.getMessage(),
                        requestId));
    }

    /**
     * 所有未预料到的异常 → 500
     * 这是最后的安全网——确保任何异常都不会以 Spring 默认的 HTML 错误页返回。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleUnexpected(
            Exception e,
            HttpServletRequest request
    ) {
        String requestId = requestId(request);
        log.error("Unexpected error. requestId={}", requestId, e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ResponseResult.fail(
                        CommonErrorCode.INTERNAL_ERROR,
                        "An unexpected error occurred",
                        requestId));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(GlobalConstant.REQUEST_ID);
        if (requestId != null && !requestId.toString().isBlank()) {
            return requestId.toString();
        }
        return RequestContext.getOrCreateRequestId();
    }
}
