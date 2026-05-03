# 🔒 PDF Watermark Service

A serverless-style PDF watermarking microservice built with **Node.js**, **pdf-lib**, **LocalStack**, and **Docker**.

Upload any PDF, enter your name — get it back with your name stamped diagonally across every page as a watermark.

---

## ✨ Features

| Feature | Detail |
|---|---|
| **Two Lambda handlers** | GET serves the form · POST processes the PDF |
| **Smart file strategy** | Files < 10 MB processed in memory · Files ≥ 10 MB stored in S3 (LocalStack) and a signed download URL is returned |
| **Graceful error handling** | Every error returns a structured `{ success, error, code }` response — no vague 500s |
| **Magic-byte PDF validation** | Checks actual file content, not just the MIME type |
| **LocalStack** | Runs a fake AWS S3 locally — no real AWS account needed |
| **Docker Compose** | One command spins up everything |

---

## 🏗️ Architecture

```
Browser
  │
  ├── GET  /           → Lambda 1 (formLambda)    → Returns HTML upload form
  └── POST /watermark  → Lambda 2 (watermarkLambda)
                              │
                              ├── Validate: name + file present, file is real PDF
                              ├── Strategy: small (<10MB) or large (≥10MB)?
                              │
                              ├── [Small] → pdfService.addWatermark() → return PDF bytes directly
                              └── [Large] → storageService.upload() → S3 (LocalStack)
                                           → pdfService.addWatermark()
                                           → storageService.upload() output
                                           → return signed download URL
```

### Engineering Patterns Used

- **Strategy Pattern** — file size determines which processing path runs (in-memory vs S3)
- **Repository Pattern** — `storageService.js` abstracts all S3 operations; swap cloud providers by changing one file
- **Middleware Pattern** — error handling and request logging are separate, reusable middleware layers
- **Single Responsibility** — each file has one job: `pdfService` does PDF work, `storageService` does storage, lambdas only orchestrate

---

## 🚀 Getting Started

### Prerequisites

Make sure you have these installed:
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- [Node.js 18+](https://nodejs.org/) (only needed if running without Docker)

---

### Option 1 — Run with Docker (Recommended)

This starts both your app and LocalStack (fake AWS) with one command.

```bash
# 1. Clone the repo
git clone https://github.com/Abhxay/pdf-watermark-service.git
cd pdf-watermark-service

# 2. Start everything
docker compose up --build

# 3. Open in your browser
# http://localhost:3000
```

That's it. Docker handles everything else.

To stop:
```bash
docker compose down
```

---

### Option 2 — Run Locally (without Docker)

You'll need LocalStack running separately, or skip S3 (large files will still work but won't persist).

```bash
# 1. Clone the repo
git clone https://github.com/Abhxay/pdf-watermark-service.git
cd pdf-watermark-service

# 2. Install dependencies
npm install

# 3. Set up environment variables
cp .env.example .env
# Edit .env if needed (defaults work out of the box)

# 4. Start the app
npm start
# or for auto-reload during development:
npm run dev

# 5. Open in your browser
# http://localhost:3000
```

---

## 📡 API Reference

### `GET /`
Returns the HTML upload form.

**Response:** `200 OK` — HTML page with a form to enter name and upload PDF.

---

### `POST /watermark`
Accepts a PDF file and a name, returns the watermarked PDF.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✅ | Text to use as the watermark (max 50 chars) |
| `pdf` | file | ✅ | PDF file to watermark (max 100 MB) |

**Response — Small file (< 10 MB):**
```
200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="watermarked_yourfile.pdf"

[PDF binary data]
```

**Response — Large file (≥ 10 MB):**
```json
{
  "success": true,
  "message": "Large file processed and stored in S3.",
  "jobId": "uuid-here",
  "strategy": "async-s3",
  "fileSize": "25.4 MB",
  "downloadUrl": "http://localhost:4566/pdf-watermark-bucket/processed/...",
  "expiresIn": "1 hour"
}
```

**Error responses** always follow this shape:
```json
{
  "success": false,
  "error": "Human-readable message",
  "code": "MACHINE_READABLE_CODE"
}
```

| Code | HTTP Status | Meaning |
|---|---|---|
| `MISSING_NAME` | 400 | Name field was empty |
| `NAME_TOO_LONG` | 400 | Name exceeds 50 characters |
| `MISSING_FILE` | 400 | No file was uploaded |
| `INVALID_FILE_TYPE` | 400 | File is not a PDF |
| `INVALID_PDF_CONTENT` | 400 | File fails magic-byte PDF check |
| `CORRUPT_PDF` | 400 | pdf-lib couldn't parse the file |
| `EMPTY_PDF` | 400 | PDF has zero pages |
| `FILE_TOO_LARGE` | 413 | File exceeds 100 MB hard cap |
| `STORAGE_UNAVAILABLE` | 503 | S3 / LocalStack unreachable |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected error |

---

### `GET /health`
Health check endpoint.

```json
{
  "status": "ok",
  "service": "pdf-watermark-service",
  "timestamp": "2026-05-03T10:00:00.000Z"
}
```

---

## 📁 Project Structure

```
pdf-watermark-service/
├── src/
│   ├── index.js                    # App entry point, route definitions
│   ├── lambdas/
│   │   ├── formLambda.js           # GET / — serves the upload form
│   │   └── watermarkLambda.js      # POST /watermark — orchestrates processing
│   ├── services/
│   │   ├── pdfService.js           # All pdf-lib logic (watermarking, validation)
│   │   └── storageService.js       # All S3 / LocalStack operations
│   └── middleware/
│       ├── errorHandler.js         # Global error handler
│       └── requestLogger.js        # Request/response logging
├── localstack/
│   └── init-aws.sh                 # Auto-creates S3 bucket on LocalStack startup
├── Dockerfile                      # Multi-stage Docker build
├── docker-compose.yml              # Orchestrates app + LocalStack
├── .env.example                    # Environment variable template
├── .gitignore
└── README.md
```

---

## 🔧 How Small vs Large Files Work

| | Small (< 10 MB) | Large (≥ 10 MB) |
|---|---|---|
| **Where processed** | In server memory | Bytes loaded then uploaded to S3 |
| **Response** | PDF binary — browser downloads directly | JSON with a signed S3 URL |
| **Response code** | `200 OK` | `202 Accepted` |
| **Why** | Fast, no extra infra for small files | Avoids memory pressure for large files; S3 gives durable storage |

---

## 🛠️ Local Development Tips

**View LocalStack S3 bucket contents:**
```bash
aws --endpoint-url=http://localhost:4566 s3 ls s3://pdf-watermark-bucket --recursive
```

**Rebuild just the app (after code changes):**
```bash
docker compose up --build app
```

**View logs:**
```bash
docker compose logs -f app
docker compose logs -f localstack
```

---

## 📦 Tech Stack

| Tool | Purpose |
|---|---|
| **Node.js 18** | Runtime |
| **Express** | HTTP server / routing |
| **pdf-lib** | PDF watermarking |
| **Multer** | Multipart form / file upload handling |
| **AWS SDK v3** | S3 client |
| **LocalStack** | Local AWS S3 emulator |
| **Docker + Compose** | Containerisation and orchestration |

---

## 📄 License

MIT © 2026 Abhay Thakur
