package com.geminid.watermark.controller;

import com.geminid.watermark.strategy.LargeFileStrategy;
import com.geminid.watermark.strategy.SmallFileStrategy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doReturn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatermarkController.class)
class WatermarkControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SmallFileStrategy smallFileStrategy;
    @MockBean  private LargeFileStrategy largeFileStrategy;

    private byte[] realPdfBytes() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile pdfFile(String name, byte[] bytes) {
        return new MockMultipartFile("pdf", name, "application/pdf", bytes);
    }

    @Test
    @DisplayName("Missing name → 400 MISSING_NAME")
    void watermark_missingName_returns400() throws Exception {
        mockMvc.perform(multipart("/watermark")
                        .file(pdfFile("test.pdf", realPdfBytes()))
                        .param("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_NAME"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Whitespace-only name → 400 MISSING_NAME")
    void watermark_whitespaceOnlyName_returns400() throws Exception {
        mockMvc.perform(multipart("/watermark")
                        .file(pdfFile("test.pdf", realPdfBytes()))
                        .param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_NAME"));
    }

    @Test
    @DisplayName("Name over 50 chars → 400 NAME_TOO_LONG")
    void watermark_nameTooLong_returns400() throws Exception {
        mockMvc.perform(multipart("/watermark")
                        .file(pdfFile("test.pdf", realPdfBytes()))
                        .param("name", "A".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NAME_TOO_LONG"));
    }

    @Test
    @DisplayName("Non-PDF content type → 400 INVALID_FILE_TYPE")
    void watermark_jpegContentType_returns400() throws Exception {
        MockMultipartFile jpeg = new MockMultipartFile("pdf", "photo.jpg", "image/jpeg", "fake".getBytes());
        mockMvc.perform(multipart("/watermark")
                        .file(jpeg)
                        .param("name", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    @DisplayName("PDF extension but bad magic bytes → 400 INVALID_PDF_CONTENT")
    void watermark_badMagicBytes_returns400() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("pdf", "fake.pdf", "application/pdf", "NOT A PDF".getBytes());
        mockMvc.perform(multipart("/watermark")
                        .file(fake)
                        .param("name", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PDF_CONTENT"));
    }

    @Test
    @DisplayName("Valid small PDF → 200 with application/pdf content type")
    void watermark_validSmallPdf_returns200() throws Exception {
        byte[] pdf = realPdfBytes();
        ResponseEntity<byte[]> pdfResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        doReturn(pdfResponse).when(smallFileStrategy).process(any(), anyString());

        mockMvc.perform(multipart("/watermark")
                        .file(pdfFile("small.pdf", pdf))
                        .param("name", "Abhijay"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Every error response has success=false, error string, and code")
    void watermark_errors_haveCorrectStructure() throws Exception {
        mockMvc.perform(multipart("/watermark")
                        .file(pdfFile("test.pdf", realPdfBytes()))
                        .param("name", ""))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.code").isString());
    }
}