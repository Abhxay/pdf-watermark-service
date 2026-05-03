/**
 * Global Error Handler Middleware
 *
 * Express catches any error passed to next(err) and routes it here.
 * Every error gets a structured JSON response — no vague 500s.
 *
 * Pattern: always return { success, error, code } so clients can handle predictably.
 */

const errorHandler = (err, req, res, next) => {
  // Log the full error internally (never expose stack traces to clients)
  console.error(`[errorHandler] ${err.name || 'Error'}: ${err.message}`);

  // ── Multer errors (file upload issues) ────────────────────────────────────
  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({
      success: false,
      error: 'File is too large. Maximum allowed size is 100 MB.',
      code: 'FILE_TOO_LARGE',
    });
  }

  if (err.code === 'LIMIT_UNEXPECTED_FILE') {
    return res.status(400).json({
      success: false,
      error: 'Unexpected file field. Use the field name "pdf".',
      code: 'UNEXPECTED_FILE_FIELD',
    });
  }

  // ── Custom application errors ─────────────────────────────────────────────
  if (err.message === 'INVALID_FILE_TYPE') {
    return res.status(400).json({
      success: false,
      error: 'Invalid file type. Only PDF files (.pdf) are accepted.',
      code: 'INVALID_FILE_TYPE',
    });
  }

  if (err.message === 'PDF has no pages.') {
    return res.status(400).json({
      success: false,
      error: 'The uploaded PDF has no pages and cannot be watermarked.',
      code: 'EMPTY_PDF',
    });
  }

  // ── pdf-lib errors ────────────────────────────────────────────────────────
  if (err.name === 'InvalidPDFException' || err.message?.includes('Failed to parse PDF')) {
    return res.status(400).json({
      success: false,
      error: 'The uploaded file is corrupted or is not a valid PDF.',
      code: 'CORRUPT_PDF',
    });
  }

  // ── AWS / S3 errors ───────────────────────────────────────────────────────
  if (err.name === 'NoSuchBucket' || err.Code === 'NoSuchBucket') {
    return res.status(503).json({
      success: false,
      error: 'Storage service unavailable. Please try again shortly.',
      code: 'STORAGE_UNAVAILABLE',
    });
  }

  if (err.name === 'NetworkingError' || err.code === 'ECONNREFUSED') {
    return res.status(503).json({
      success: false,
      error: 'Could not reach storage service. Is LocalStack running?',
      code: 'STORAGE_CONNECTION_FAILED',
    });
  }

  // ── Generic fallback ──────────────────────────────────────────────────────
  const isDev = process.env.NODE_ENV !== 'production';
  return res.status(500).json({
    success: false,
    error: 'An unexpected error occurred. Please try again.',
    code: 'INTERNAL_SERVER_ERROR',
    // Only show details in development — never in production
    ...(isDev && { detail: err.message }),
  });
};

module.exports = errorHandler;
