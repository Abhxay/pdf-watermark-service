package com.geminid.watermark.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class PdfServiceTest {

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService();
    }

    private byte[] buildPdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildEncryptedPdf() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy("ownerpass", "userpass", ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Single-page PDF: returns non-empty bytes")
    void addWatermark_singlePage_returnsBytes() throws IOException {
        byte[] result = pdfService.addWatermark(buildPdf(1), "Abhijay");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Result bytes are a valid PDF")
    void addWatermark_result_isValidPdf() throws IOException {
        byte[] result = pdfService.addWatermark(buildPdf(1), "Watermark");
        assertThat(result[0]).isEqualTo((byte) 0x25);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x44);
        assertThat(result[3]).isEqualTo((byte) 0x46);
    }

    @Test
    @DisplayName("Zero-page PDF: throws IOException with correct message")
    void addWatermark_zeroPagesDoc_throwsIOException() throws IOException {
        try (PDDocument emptyDoc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            emptyDoc.save(out);
            assertThatThrownBy(() -> pdfService.addWatermark(out.toByteArray(), "Name"))
                    .isInstanceOf(IOException.class)
                    .hasMessage("PDF has no pages.");
        }
    }

    @Test
    @DisplayName("Garbage bytes: throws IOException")
    void addWatermark_garbageBytes_throwsIOException() {
        assertThatThrownBy(() -> pdfService.addWatermark("not a pdf".getBytes(), "Name"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("50-char name: does not throw")
    void addWatermark_maxLengthName_doesNotThrow() throws IOException {
        assertThat(pdfService.addWatermark(buildPdf(1), "A".repeat(50))).isNotEmpty();
    }

    @Test
    @DisplayName("Name with special characters: does not throw")
    void addWatermark_specialCharsName_doesNotThrow() throws IOException {
        assertThat(pdfService.addWatermark(buildPdf(1), "John O'Brien Jr.")).isNotEmpty();
    }

    @Test
    @DisplayName("Password-protected PDF: throws IOException with password message")
    void addWatermark_encryptedPdf_throwsWithPasswordMessage() throws IOException {
        assertThatThrownBy(() -> pdfService.addWatermark(buildEncryptedPdf(), "Name"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("password-protected");
    }

    @Test
    @DisplayName("50-page PDF: processes successfully")
    void addWatermark_fiftyPages_completes() throws IOException {
        assertThat(pdfService.addWatermark(buildPdf(50), "BulkTest")).isNotEmpty();
    }
}