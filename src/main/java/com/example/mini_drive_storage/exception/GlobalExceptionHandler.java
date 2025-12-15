package com.example.mini_drive_storage.exception;

import com.example.mini_drive_storage.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
// This class help all error handling process be grouped
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> buildError(
            String message,
            HttpStatus status,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(request.getRequestURI())
                        .build());
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            InvalidRequestException ex,
            HttpServletRequest request
    ) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            PermissionDeniedException ex,
            HttpServletRequest request
    ) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildError(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }
}
