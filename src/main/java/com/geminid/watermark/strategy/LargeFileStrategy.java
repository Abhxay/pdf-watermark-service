package com.geminid.watermark.strategy;

import com.geminid.watermark.service.PdfService;
import com.geminid.watermark.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy for large files (10 MB and above).
 *
 * Flow:
 *  1. Upload original PDF to S3 (LocalStack) for safe-keeping
 *  2. Add watermark using PDFBox
 *  3. Upload watermarked PDF to S3
 *  4. Generate a pre-signed URL (valid 1 hour) so the client can download it
 *  5. Return JSON with the download URL — client doesn't get the bytes directly
 *
 * Why S3 for large files?
 *  Keeping large files in server memory is risky under high load.
 *  S3 gives durable, scalable storage and offloads the transfer to the client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LargeFileStrategy implements FileProcessingStrategy {

    private final PdfService pdfService;
    private final StorageService storageService;

    @Override
    public ResponseEntity<?> process(MultipartFile file, String watermarkText) throws Exception {
        String jobId = UUID.randomUUID().toString();
        String originalKey  = "uploads/"   + jobId + "/original.pdf";
        String processedKey = "processed/" + jobId + "/watermarked_" + file.getOriginalFilename();

        // 1. Store original
        storageService.upload(originalKey, file.getBytes(), "application/pdf");

        // 2. Watermark
        byte[] watermarked = pdfService.addWatermark(file.getBytes(), watermarkText);

        // 3. Store result
        storageService.upload(processedKey, watermarked, "application/pdf");

        // 4. Generate signed URL
        String downloadUrl = storageService.getSignedUrl(processedKey)
        .replace("http://ministack:4566", "http://localhost:4566");

        log.info("Large file job {} complete. Key: {}", jobId, processedKey);

        // 5. Return JSON response
        return ResponseEntity.accepted().body(Map.of(
                "success",       true,
                "message",       "Large file processed and stored in S3.",
                "jobId",         jobId,
                "strategy",      "async-s3",
                "watermarkText", watermarkText,
                "fileSize",      String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)),
                "downloadUrl",   downloadUrl,
                "expiresIn",     "1 hour"
        ));
    }
}
