package com.geminid.watermark.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Global Exception Handler — Middleware Pattern.
 *
 * Every unhandled exception is caught here.
 * All errors return the same structured shape:
 *   { "success": false, "error": "human message", "code": "MACHINE_CODE" }
 *
 * This means the client always knows what to expect — no vague 500 HTML pages.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** File exceeds the max upload size set in application.properties */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE,
                "File is too large. Maximum allowed size is 100 MB.", "FILE_TOO_LARGE");
    }

    /** Required parameter (name or pdf) missing from the request */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName(), "MISSING_PARAMETER");
    }

    /** PDF has no pages or is corrupted — thrown by PdfService */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<?> handleIoException(java.io.IOException ex) {
        log.error("PDF processing error: {}", ex.getMessage());
        if ("PDF has no pages.".equals(ex.getMessage())) {
            return error(HttpStatus.BAD_REQUEST,
                    "The uploaded PDF has no pages and cannot be watermarked.", "EMPTY_PDF");
        }
        return error(HttpStatus.BAD_REQUEST,
                "The file could not be processed. It may be corrupted or not a valid PDF.", "CORRUPT_PDF");
    }

    /** S3 / LocalStack not reachable */
    @ExceptionHandler(software.amazon.awssdk.core.exception.SdkClientException.class)
    public ResponseEntity<?> handleAwsException(software.amazon.awssdk.core.exception.SdkClientException ex) {
        log.error("Storage service error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Storage service is unavailable. Is LocalStack running?", "STORAGE_UNAVAILABLE");
    }

    /** Catch-all — something unexpected happened */
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
