package com.geminid.watermark.controller;

import com.geminid.watermark.exception.InvalidPdfException;
import com.geminid.watermark.strategy.FileProcessingStrategy;
import com.geminid.watermark.strategy.LargeFileStrategy;
import com.geminid.watermark.strategy.SmallFileStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lambda 2 — Watermark Handler
 * POST /watermark
 *
 * Engineering patterns used:
 * 1. Strategy Pattern  — picks SmallFileStrategy or LargeFileStrategy based on file size
 * 2. Fail Fast         — validates inputs before any expensive processing
 * 3. Single Responsibility — this class only orchestrates; PDF work is in PdfService
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WatermarkController {

    private final SmallFileStrategy smallFileStrategy;
    private final LargeFileStrategy largeFileStrategy;

    private static final long LARGE_FILE_THRESHOLD = 10L * 1024 * 1024; // 10 MB
    private static final int MAX_NAME_LENGTH = 50;
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

    @PostMapping("/watermark")
    public ResponseEntity<?> watermark(
            @RequestParam("name") String name,
            @RequestParam("pdf") MultipartFile file) throws Exception {

        // ── Step 1: Validate inputs (fail fast) ───────────────────────────────
        String trimmedName = name == null ? "" : name.trim();

        if (trimmedName.isEmpty()) {
            return ResponseEntity.badRequest().body(error("Name is required for the watermark.", "MISSING_NAME"));
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            return ResponseEntity.badRequest().body(error("Name must be " + MAX_NAME_LENGTH + " characters or fewer.", "NAME_TOO_LONG"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(error("A PDF file is required.", "MISSING_FILE"));
        }
        if (!isPdf(file)) {
            return ResponseEntity.badRequest().body(error("Only PDF files are accepted.", "INVALID_FILE_TYPE"));
        }
        if (!hasValidPdfBytes(file.getBytes())) {
            return ResponseEntity.badRequest().body(error("The file does not appear to be a valid PDF.", "INVALID_PDF_CONTENT"));
        }

        // ── Step 2: Pick strategy based on file size ──────────────────────────
        boolean isLarge = file.getSize() > LARGE_FILE_THRESHOLD;
        FileProcessingStrategy strategy = isLarge ? largeFileStrategy : smallFileStrategy;

        log.info("Processing '{}' ({} KB) | strategy={} | watermark='{}'",
                file.getOriginalFilename(),
                file.getSize() / 1024,
                isLarge ? "async-s3" : "in-memory",
                trimmedName);

        // ── Step 3: Delegate to strategy ──────────────────────────────────────
        return strategy.process(file, trimmedName);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return "application/pdf".equals(contentType)
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
    }

    /** Check magic bytes — first 4 bytes of any real PDF are %PDF (0x25 0x50 0x44 0x46) */
    private boolean hasValidPdfBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        return bytes[0] == PDF_MAGIC[0] && bytes[1] == PDF_MAGIC[1]
                && bytes[2] == PDF_MAGIC[2] && bytes[3] == PDF_MAGIC[3];
    }

    private java.util.Map<String, Object> error(String message, String code) {
        return java.util.Map.of("success", false, "error", message, "code", code);
    }
}
