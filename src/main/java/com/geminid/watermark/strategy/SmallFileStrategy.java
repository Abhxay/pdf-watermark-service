package com.geminid.watermark.strategy;

import com.geminid.watermark.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy for small files (under 10 MB).
 *
 * Flow:
 *  1. Load PDF bytes from the uploaded file
 *  2. Add watermark using PDFBox
 *  3. Return the watermarked PDF bytes directly in the HTTP response
 *     (browser triggers a download automatically)
 *
 * No S3 involved — everything happens in memory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmallFileStrategy implements FileProcessingStrategy {

    private final PdfService pdfService;

    @Override
    public ResponseEntity<?> process(MultipartFile file, String watermarkText) throws Exception {
        byte[] watermarkedBytes = pdfService.addWatermark(file.getBytes(), watermarkText);

        String outputFilename = "watermarked_" + file.getOriginalFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                .header("X-Processing-Strategy", "in-memory")
                .header("X-Watermark-Text", watermarkText)
                .body(watermarkedBytes);
    }
}
