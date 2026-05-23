package com.github.insight.controller;

import com.github.insight.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, WebRequest request) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
            400,
            "잘못된 요청입니다.",
            "INVALID_ARGUMENT",
            e.getMessage()
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException e, WebRequest request) {
        log.warn("IllegalStateException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
            503,
            "서비스가 일시적으로 사용 불가합니다.",
            "SERVICE_UNAVAILABLE",
            e.getMessage()
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException e, WebRequest request) {
        log.warn("Resource not found: {}", e.getRequestURL());
        ErrorResponse response = ErrorResponse.of(
            404,
            "요청한 리소스를 찾을 수 없습니다.",
            "NOT_FOUND"
        );
        response.setPath(e.getRequestURL());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, WebRequest request) {
        log.warn("Type mismatch: {} - {}", e.getName(), e.getValue());
        ErrorResponse response = ErrorResponse.of(
            400,
            "요청 파라미터 타입이 올바르지 않습니다.",
            "TYPE_MISMATCH",
            String.format("파라미터 '%s'는 %s 타입이어야 합니다.",
                e.getName(), e.getRequiredType().getSimpleName())
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException e, WebRequest request) {
        log.error("Unexpected RuntimeException: {}", e.getMessage(), e);
        ErrorResponse response = ErrorResponse.of(
            500,
            "서버 오류가 발생했습니다.",
            "INTERNAL_SERVER_ERROR"
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception e, WebRequest request) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        ErrorResponse response = ErrorResponse.of(
            500,
            "예상치 못한 오류가 발생했습니다.",
            "INTERNAL_SERVER_ERROR"
        );
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
