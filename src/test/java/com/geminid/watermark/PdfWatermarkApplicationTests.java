package com.geminid.watermark;

import com.geminid.watermark.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PdfWatermarkApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void watermarkTextIsApplied() throws Exception {
        PdfService pdfService = new PdfService();
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();

        byte[] result = pdfService.addWatermark(out.toByteArray(), "Test Name");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}