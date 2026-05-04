package com.geminid.watermark.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE,
                "File is too large. Maximum allowed size is 100 MB.", "FILE_TOO_LARGE");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName(), "MISSING_PARAMETER");
    }

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<?> handleIoException(java.io.IOException ex) {
        log.error("PDF processing error: {}", ex.getMessage());
        if ("PDF has no pages.".equals(ex.getMessage())) {
            return error(HttpStatus.BAD_REQUEST,
                    "The uploaded PDF has no pages and cannot be watermarked.", "EMPTY_PDF");
        }
        if (ex.getMessage() != null && ex.getMessage().contains("password-protected")) {
            return error(HttpStatus.BAD_REQUEST, ex.getMessage(), "PASSWORD_PROTECTED_PDF");
        }
        return error(HttpStatus.BAD_REQUEST,
                "The file could not be processed. It may be corrupted or not a valid PDF.", "CORRUPT_PDF");
    }

    @ExceptionHandler(software.amazon.awssdk.core.exception.SdkClientException.class)
    public ResponseEntity<?> handleAwsException(software.amazon.awssdk.core.exception.SdkClientException ex) {
        log.error("Storage service error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Storage service is unavailable. Is MiniStack running?", "STORAGE_UNAVAILABLE");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", "INTERNAL_SERVER_ERROR");
    }

    private ResponseEntity<?> error(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(
                Map.of("success", false, "error", message, "code", code));
    }
}