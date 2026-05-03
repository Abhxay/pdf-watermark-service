require('dotenv').config();
const express = require('express');
const { formLambdaHandler } = require('./lambdas/formLambda');
const { watermarkLambdaHandler } = require('./lambdas/watermarkLambda');
const errorHandler = require('./middleware/errorHandler');
const requestLogger = require('./middleware/requestLogger');

const app = express();
const PORT = process.env.PORT || 3000;

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(express.urlencoded({ extended: true }));
app.use(requestLogger);

// ── Lambda 1 — GET: Serve the upload form ─────────────────────────────────────
app.get('/', formLambdaHandler);

// ── Lambda 2 — POST: Receive PDF + name, add watermark, return result ─────────
app.post('/watermark', watermarkLambdaHandler);

// ── Health check ──────────────────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'pdf-watermark-service',
    timestamp: new Date().toISOString(),
  });
});

// ── 404 handler ───────────────────────────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: `Route ${req.method} ${req.path} not found`,
    code: 'NOT_FOUND',
  });
});

// ── Global error handler (must be last) ───────────────────────────────────────
app.use(errorHandler);

app.listen(PORT, () => {
  console.log(`\n🚀 PDF Watermark Service running on http://localhost:${PORT}`);
  console.log(`📋 Upload form   → GET  http://localhost:${PORT}/`);
  console.log(`🔒 Watermark API → POST http://localhost:${PORT}/watermark`);
  console.log(`❤️  Health check  → GET  http://localhost:${PORT}/health\n`);
});

module.exports = app;
