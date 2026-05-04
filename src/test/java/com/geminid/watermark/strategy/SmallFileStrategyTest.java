package com.geminid.watermark.strategy;

import com.geminid.watermark.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmallFileStrategyTest {

    @Mock        private PdfService pdfService;
    @InjectMocks private SmallFileStrategy strategy;

    private byte[] fakePdfBytes() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Returns Content-Type application/pdf")
    void process_returnsApplicationPdfContentType() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        MockMultipartFile file = new MockMultipartFile("pdf", "test.pdf", "application/pdf", pdf);

        ResponseEntity<?> response = strategy.process(file, "Alice");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    }

    @Test
    @DisplayName("Content-Disposition is inline with correct filename")
    void process_returnsInlineDisposition() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        MockMultipartFile file = new MockMultipartFile("pdf", "report.pdf", "application/pdf", pdf);

        ResponseEntity<?> response = strategy.process(file, "Bob");

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).startsWith("inline");
        assertThat(disposition).contains("watermarked_report.pdf");
    }

    @Test
    @DisplayName("Response body is the watermarked bytes from PdfService")
    void process_bodyIsWatermarkedBytes() throws Exception {
        byte[] watermarked = "WATERMARKED".getBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(watermarked);
        MockMultipartFile file = new MockMultipartFile("pdf", "test.pdf", "application/pdf", fakePdfBytes());

        ResponseEntity<?> response = strategy.process(file, "Test");

        assertThat(response.getBody()).isEqualTo(watermarked);
    }

    @Test
    @DisplayName("HTTP status is 200 OK")
    void process_returns200() throws Exception {
        byte[] pdf = fakePdfBytes();
        when(pdfService.addWatermark(any(), any())).thenReturn(pdf);
        MockMultipartFile file = new MockMultipartFile("pdf", "x.pdf", "application/pdf", pdf);

        assertThat(strategy.process(file, "X").getStatusCode().value()).isEqualTo(200);
    }
}