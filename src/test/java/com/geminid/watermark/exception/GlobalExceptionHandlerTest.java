package com.geminid.watermark.exception;

import com.geminid.watermark.controller.WatermarkController;
import com.geminid.watermark.strategy.LargeFileStrategy;
import com.geminid.watermark.strategy.SmallFileStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatermarkController.class)
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SmallFileStrategy smallFileStrategy;
    @MockBean  private LargeFileStrategy largeFileStrategy;

    private MockMultipartFile validPdfFile() {
        return new MockMultipartFile("pdf", "x.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});
    }

    @Test
    @DisplayName("IOException 'PDF has no pages' → 400 EMPTY_PDF")
    void ioException_emptyPdf_returns400() throws Exception {
        when(smallFileStrategy.process(any(), any())).thenThrow(new IOException("PDF has no pages."));

        mockMvc.perform(multipart("/watermark").file(validPdfFile()).param("name", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PDF"));
    }

    @Test
    @DisplayName("IOException password-protected → 400 PASSWORD_PROTECTED_PDF")
    void ioException_passwordProtected_returns400() throws Exception {
        when(smallFileStrategy.process(any(), any()))
                .thenThrow(new IOException("PDF is password-protected and cannot be watermarked. Please remove the password first."));

        mockMvc.perform(multipart("/watermark").file(validPdfFile()).param("name", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_PROTECTED_PDF"));
    }

    @Test
    @DisplayName("Generic IOException → 400 CORRUPT_PDF")
    void ioException_generic_returns400() throws Exception {
        when(smallFileStrategy.process(any(), any())).thenThrow(new IOException("read error"));

        mockMvc.perform(multipart("/watermark").file(validPdfFile()).param("name", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CORRUPT_PDF"));
    }

    @Test
    @DisplayName("RuntimeException → 500 INTERNAL_SERVER_ERROR")
    void runtimeException_returns500() throws Exception {
        when(smallFileStrategy.process(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart("/watermark").file(validPdfFile()).param("name", "Test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SdkClientException → 503 STORAGE_UNAVAILABLE")
    void sdkClientException_returns503() throws Exception {
        when(smallFileStrategy.process(any(), any()))
                .thenThrow(software.amazon.awssdk.core.exception.SdkClientException
                        .builder().message("Connection refused").build());

        mockMvc.perform(multipart("/watermark").file(validPdfFile()).param("name", "Test"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
    }
}