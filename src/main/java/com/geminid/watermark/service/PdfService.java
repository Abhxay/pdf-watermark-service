package com.geminid.watermark.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class PdfService {

    public byte[] addWatermark(byte[] pdfBytes, String watermarkText) throws IOException {
        PDDocument document;
        try {
            document = Loader.loadPDF(pdfBytes);
        } catch (InvalidPasswordException e) {
            throw new IOException("PDF is password-protected and cannot be watermarked. Please remove the password first.", e);
        }

        try (document) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages.");
            }

            boolean isPdfA = isPdfACompliant(document);
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (PDPage page : document.getPages()) {
                addWatermarkToPage(document, page, font, watermarkText, !isPdfA);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Watermark '{}' added to {} page(s). PDF/A={}", watermarkText, document.getNumberOfPages(), isPdfA);
            return out.toByteArray();
        }
    }

    private boolean isPdfACompliant(PDDocument document) {
        try {
            var meta = document.getDocumentCatalog().getMetadata();
            if (meta == null) return false;
            String xmp = new String(meta.toByteArray());
            return xmp.contains("pdfaid:conformance") || xmp.contains("http://www.aiim.org/pdfa");
        } catch (Exception e) {
            return false;
        }
    }

    private void addWatermarkToPage(PDDocument document, PDPage page,
                                    PDFont font, String watermarkText,
                                    boolean transparencyAllowed) throws IOException {
        PDRectangle pageSize = page.getMediaBox();
        float pageWidth  = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();

        float fontSize  = Math.min(Math.max(pageWidth * 0.08f, 28f), 72f);
        float textWidth = (font.getStringWidth(watermarkText) / 1000f) * fontSize;

        float x = (pageWidth  - (float)(textWidth * Math.cos(Math.PI / 4))) / 2f;
        float y = (pageHeight - (float)(textWidth * Math.sin(Math.PI / 4))) / 2f;

        try (PDPageContentStream cs = new PDPageContentStream(
                document, page, AppendMode.APPEND, true, true)) {

            if (transparencyAllowed) {
                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.25f);
                cs.setGraphicsStateParameters(gs);
            }

            float grey = transparencyAllowed ? 0.59f : 0.82f;
            cs.setNonStrokingColor(grey, grey, grey);
            cs.setFont(font, fontSize);
            cs.beginText();
            cs.setTextMatrix(Matrix.getRotateInstance(Math.PI / 4, x, y));
            cs.showText(watermarkText);
            cs.endText();
        }
    }
}