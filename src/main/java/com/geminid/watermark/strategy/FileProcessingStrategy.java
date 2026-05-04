package com.geminid.watermark.strategy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy Pattern — defines the contract for processing a PDF.
 *
 * Two implementations:
 *  - SmallFileStrategy: process in memory, return PDF bytes directly
 *  - LargeFileStrategy: upload to S3 (ministack), return a signed download URL
 *
 * The controller picks the right strategy based on file size.
 * If requirements change (e.g. add async queue strategy), just add a new implementation.
 */
public interface FileProcessingStrategy {
    ResponseEntity<?> process(MultipartFile file, String watermarkText) throws Exception;
}
