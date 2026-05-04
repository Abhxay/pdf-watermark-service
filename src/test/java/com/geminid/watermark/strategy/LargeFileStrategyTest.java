package com.geminid.watermark.strategy;

import com.geminid.watermark.service.PdfService;
import com.geminid.watermark.service.StorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeFileStrategyTest {

    @Mock        private PdfService pdfService;
    @Mock        private StorageService storageService;
    @InjectMocks private LargeFileStrategy strategy;

    private byte[] fakePdfBytes() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Uploads original to 'uploads/' prefix")
    void process_uploadsOriginalWithCorrectPrefix() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        when(storageService.getSignedUrl(any())).thenReturn("http://localhost:4566/signed");

        strategy.process(new MockMultipartFile("pdf", "large.pdf", "application/pdf", pdf), "Alice");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(storageService, atLeastOnce()).upload(keys.capture(), any(), any());
        assertThat(keys.getAllValues()).anyMatch(k -> k.startsWith("uploads/"));
    }

    @Test
    @DisplayName("Uploads watermarked result to 'processed/' prefix")
    void process_uploadsWatermarkedWithCorrectPrefix() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        when(storageService.getSignedUrl(any())).thenReturn("http://localhost:4566/signed");

        strategy.process(new MockMultipartFile("pdf", "large.pdf", "application/pdf", pdf), "Alice");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(storageService, atLeastOnce()).upload(keys.capture(), any(), any());
        assertThat(keys.getAllValues()).anyMatch(k -> k.startsWith("processed/"));
    }

    @Test
    @DisplayName("Response body contains downloadUrl and success=true")
    void process_responseContainsDownloadUrl() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        when(storageService.getSignedUrl(any())).thenReturn("http://localhost:4566/signed");

        ResponseEntity<?> response = strategy.process(
                new MockMultipartFile("pdf", "large.pdf", "application/pdf", pdf), "Bob");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("downloadUrl");
        assertThat(body.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("HTTP status is 202 Accepted")
    void process_returns202() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        when(storageService.getSignedUrl(any())).thenReturn("http://signed");

        ResponseEntity<?> response = strategy.process(
                new MockMultipartFile("pdf", "large.pdf", "application/pdf", pdf), "Test");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
    }

    @Test
    @DisplayName("Two requests generate different S3 keys (unique jobId)")
    void process_twoRequests_generateDifferentKeys() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        when(storageService.getSignedUrl(any())).thenReturn("http://signed");

        strategy.process(new MockMultipartFile("pdf", "a.pdf", "application/pdf", pdf), "T");
        strategy.process(new MockMultipartFile("pdf", "b.pdf", "application/pdf", pdf), "T");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(4)).upload(keys.capture(), any(), any());
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
    }
}