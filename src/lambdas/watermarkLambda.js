/**
 * Lambda 2 — Watermark Handler
 * POST /watermark
 *
 * Engineering patterns used:
 * 1. Strategy Pattern  — different processing for small vs large files
 * 2. Single Responsibility — this lambda only orchestrates; PDF logic lives in pdfService
 * 3. Fail Fast — validate inputs before doing any expensive work
 */

const multer = require('multer');
const { v4: uuidv4 } = require('uuid');
const pdfService = require('../services/pdfService');
const storageService = require('../services/storageService');

// ── Constants ─────────────────────────────────────────────────────────────────
const LARGE_FILE_THRESHOLD_BYTES = 10 * 1024 * 1024; // 10 MB
const MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024;        // 100 MB hard cap
const MAX_NAME_LENGTH = 50;

// ── Multer config — memory storage so we work with raw bytes ─────────────────
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_FILE_SIZE_BYTES },
  fileFilter: (req, file, cb) => {
    if (file.mimetype !== 'application/pdf') {
      return cb(new Error('INVALID_FILE_TYPE'));
    }
    cb(null, true);
  },
}).single('pdf');

// ── Strategy: small file — process in memory, return bytes directly ───────────
const handleSmallFile = async (fileBuffer, originalName, watermarkText, res) => {
  const watermarkedBytes = await pdfService.addWatermark(fileBuffer, watermarkText);

  res.set({
    'Content-Type': 'application/pdf',
    'Content-Disposition': `attachment; filename="watermarked_${originalName}"`,
    'Content-Length': watermarkedBytes.length,
    'X-Processing-Strategy': 'in-memory',
    'X-Watermark-Text': watermarkText,
  });

  return res.status(200).send(Buffer.from(watermarkedBytes));
};

// ── Strategy: large file — store in S3, process, return signed URL ────────────
const handleLargeFile = async (fileBuffer, originalName, watermarkText, res) => {
  const jobId = uuidv4();
  const inputKey  = `uploads/${jobId}/original.pdf`;
  const outputKey = `processed/${jobId}/watermarked_${originalName}`;

  // 1. Save original to S3
  await storageService.upload(inputKey, fileBuffer, 'application/pdf');

  // 2. Add watermark
  const watermarkedBytes = await pdfService.addWatermark(fileBuffer, watermarkText);

  // 3. Save result to S3
  await storageService.upload(outputKey, Buffer.from(watermarkedBytes), 'application/pdf');

  // 4. Generate a signed download URL (valid 1 hour)
  const downloadUrl = await storageService.getSignedUrl(outputKey, 3600);

  return res.status(202).json({
    success: true,
    message: 'Large file processed and stored in S3.',
    jobId,
    watermarkText,
    strategy: 'async-s3',
    fileSize: `${(fileBuffer.length / (1024 * 1024)).toFixed(2)} MB`,
    downloadUrl,
    expiresIn: '1 hour',
  });
};

// ── Main handler ──────────────────────────────────────────────────────────────
const watermarkLambdaHandler = (req, res, next) => {
  upload(req, res, async (uploadError) => {
    if (uploadError) return next(uploadError);

    try {
      // ── Step 1: Validate inputs (fail fast) ───────────────────────────────
      const name = (req.body.name || '').trim();

      if (!name) {
        return res.status(400).json({
          success: false,
          error: 'Name is required to create a watermark.',
          code: 'MISSING_NAME',
          field: 'name',
        });
      }

      if (name.length > MAX_NAME_LENGTH) {
        return res.status(400).json({
          success: false,
          error: `Name must be ${MAX_NAME_LENGTH} characters or fewer.`,
          code: 'NAME_TOO_LONG',
          field: 'name',
        });
      }

      if (!req.file) {
        return res.status(400).json({
          success: false,
          error: 'A PDF file is required.',
          code: 'MISSING_FILE',
          field: 'pdf',
        });
      }

      // ── Step 2: Verify it's actually a PDF (check magic bytes, not just MIME) ─
      const isValidPdf = pdfService.isValidPdf(req.file.buffer);
      if (!isValidPdf) {
        return res.status(400).json({
          success: false,
          error: 'The uploaded file does not appear to be a valid PDF.',
          code: 'INVALID_PDF_CONTENT',
        });
      }

      // ── Step 3: Strategy — pick processing path based on file size ────────
      const isLargeFile = req.file.size > LARGE_FILE_THRESHOLD_BYTES;

      console.log(`[watermarkLambda] Processing "${req.file.originalname}" (${(req.file.size / 1024).toFixed(1)} KB) | strategy=${isLargeFile ? 'async-s3' : 'in-memory'} | watermark="${name}"`);

      if (isLargeFile) {
        return await handleLargeFile(req.file.buffer, req.file.originalname, name, res);
      } else {
        return await handleSmallFile(req.file.buffer, req.file.originalname, name, res);
      }

    } catch (error) {
      return next(error);
    }
  });
};

module.exports = { watermarkLambdaHandler };
