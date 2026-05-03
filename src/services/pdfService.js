/**
 * PDF Service
 * All pdf-lib operations live here. The lambdas call this service
 * but never touch pdf-lib directly — clean separation of concerns.
 */

const { PDFDocument, rgb, StandardFonts, degrees } = require('pdf-lib');

// PDF magic bytes — first 4 bytes of any valid PDF are "%PDF"
const PDF_MAGIC_BYTES = Buffer.from([0x25, 0x50, 0x44, 0x46]); // %PDF

/**
 * Checks if a buffer is actually a PDF by reading its magic bytes.
 * Trusting the MIME type alone is not enough — clients can lie.
 */
const isValidPdf = (buffer) => {
  if (!buffer || buffer.length < 4) return false;
  return buffer.slice(0, 4).equals(PDF_MAGIC_BYTES);
};

/**
 * Adds a diagonal watermark to every page of a PDF.
 *
 * @param {Buffer}  pdfBuffer      - Raw bytes of the original PDF
 * @param {string}  watermarkText  - Text to stamp (the user's name)
 * @returns {Uint8Array}           - Raw bytes of the watermarked PDF
 */
const addWatermark = async (pdfBuffer, watermarkText) => {
  // Load the PDF document
  const pdfDoc = await PDFDocument.load(pdfBuffer, {
    ignoreEncryption: true, // Gracefully handle password-protected PDFs
  });

  // Embed a standard font (no external file needed)
  const font = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

  const pages = pdfDoc.getPages();

  if (pages.length === 0) {
    throw new Error('PDF has no pages.');
  }

  pages.forEach((page) => {
    const { width, height } = page.getSize();

    // Scale font size relative to page width so it looks good on any size PDF
    const fontSize = Math.min(Math.max(width * 0.07, 28), 72);
    const textWidth = font.widthOfTextAtSize(watermarkText, fontSize);

    // Centre the watermark on the page
    const x = (width - textWidth) / 2;
    const y = height / 2;

    // Draw the watermark — grey, semi-transparent, 45° rotation
    page.drawText(watermarkText, {
      x,
      y,
      size: fontSize,
      font,
      color: rgb(0.6, 0.6, 0.6),   // grey
      opacity: 0.25,                 // semi-transparent
      rotate: degrees(45),           // diagonal
    });
  });

  // Return modified PDF as bytes
  return pdfDoc.save();
};

module.exports = { addWatermark, isValidPdf };
