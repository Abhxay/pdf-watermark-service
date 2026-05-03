package com.geminid.watermark.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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

    /**
     * Adds a diagonal, semi-transparent watermark to every page of a PDF.
     *
     * @param pdfBytes      Raw bytes of the original PDF
     * @param watermarkText Text to stamp (the user's name)
     * @return              Raw bytes of the watermarked PDF
     */
    public byte[] addWatermark(byte[] pdfBytes, String watermarkText) throws IOException {
      
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages.");
            }


            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (PDPage page : document.getPages()) {
                addWatermarkToPage(document, page, font, watermarkText);
            }

           
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Watermark '{}' added to {} page(s).", watermarkText, document.getNumberOfPages());
            return out.toByteArray();
        }
    }

    private void addWatermarkToPage(PDDocument document, PDPage page,
                                    PDFont font, String watermarkText) throws IOException {

        PDRectangle pageSize = page.getMediaBox();
        float pageWidth  = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();

       
        float fontSize = Math.min(Math.max(pageWidth * 0.08f, 28f), 72f);

       
        float textWidth = (font.getStringWidth(watermarkText) / 1000f) * fontSize;

        float x = (pageWidth  - (float)(textWidth * Math.cos(Math.PI / 4))) / 2f;
        float y = (pageHeight - (float)(textWidth * Math.sin(Math.PI / 4))) / 2f;

        
        try (PDPageContentStream cs = new PDPageContentStream(
                document, page, AppendMode.APPEND, true, true)) {

            
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.25f); // 25% opacity
            cs.setGraphicsStateParameters(gs);

            // Grey colour
            cs.setNonStrokingColor(0.59f, 0.59f, 0.59f);

            // Draw text at 45-degree angle
            cs.setFont(font, fontSize);
            cs.beginText();
            cs.setTextMatrix(Matrix.getRotateInstance(Math.PI / 4, x, y));
            cs.showText(watermarkText);
            cs.endText();
        }
    }
}
