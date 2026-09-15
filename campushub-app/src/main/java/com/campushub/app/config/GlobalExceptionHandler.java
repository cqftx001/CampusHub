package com.campushub.app.config;

import com.campushub.shared.base.ResponseResult;
import com.campushub.shared.error.BaseException;
import com.campushub.shared.error.CommonErrorCode;
import com.campushub.shared.error.ErrorCode;
import com.campushub.shared.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResponseResult<Void>> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ResponseResult.fail(errorCode.getCode(), exception.getMessage(), requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(ResponseResult.fail(CommonErrorCode.VALIDATION_ERROR.getCode(), message, requestId));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseResult<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .badRequest()
                .body(ResponseResult.fail(
                        CommonErrorCode.VALIDATION_ERROR.getCode(),
                        exception.getMessage(),
                        requestId
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseResult<Void>> handleNoResourceFoundException(
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .status(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ResponseResult.fail(
                        CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                        CommonErrorCode.RESOURCE_NOT_FOUND.getMessage(),
                        requestId
                ));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ResponseResult<Void>> handleInvalidRequestFormat(
            Exception e,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseResult.fail(CommonErrorCode.VALIDATION_ERROR.getCode(), e.getMessage(), requestId));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseResult<Void>> handleMaxUploadSizeExceeded(
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);

        return ResponseEntity
                .badRequest()
                .body(ResponseResult.fail(
                        CommonErrorCode.VALIDATION_ERROR.getCode(),
                        "Each uploaded file must not exceed 5 MB",
                        requestId
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        String requestId = RequestUtils.getOrCreateRequestId(request);
        LOGGER.error("Unhandled exception; requestId={}", requestId, exception);

        return ResponseEntity
                .internalServerError()
                .body(ResponseResult.fail(
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        requestId
                ));
    }
}
