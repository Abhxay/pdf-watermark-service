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
 *  3. Return the watermarked PDF with Content-Disposition: inline
 *     so the browser opens it in a new tab instead of downloading
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
                // "inline" tells the browser to display the PDF rather than download it
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + outputFilename + "\"")
                .header("X-Processing-Strategy", "in-memory")
                .header("X-Watermark-Text", watermarkText)
                .body(watermarkedBytes);
    }
}
