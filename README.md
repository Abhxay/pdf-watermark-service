# 🔒 PDF Watermark Service

A PDF watermarking microservice built with **Java 21**, **Spring Boot**, **Apache PDFBox**, **LocalStack**, and **Docker**.

Upload any PDF, enter your name — get it back with your name stamped diagonally across every page as a watermark.

> Built by [Abhay Thakur](https://github.com/Abhxay)

---

## 🔄 Why These Tools? (Alternatives Explained)

| Requirement | Tool Used | Original Suggestion | Why This One? |
|---|---|---|---|
| PDF manipulation | **Apache PDFBox 3.x** | pdf-lib.js | pdf-lib.js is JavaScript only. Apache PDFBox is the Java equivalent — same idea: open a PDF, loop through pages, draw text, save it. Free, open source, Apache Foundation. |
| Local AWS simulation | **LocalStack** | LocalStack | Same tool. Runs in Docker, simulates AWS S3. No AWS account needed. Same SDK — switching to real AWS later requires zero code changes. |
| Language | **Java 21 + Spring Boot** | Node.js | Java is the developer's primary language. Code you own is code you can defend. |
| Build | **Maven** | npm | Standard Java build tool. Same role as npm for Node. |

---

## ✨ Features

| Feature | Detail |
|---|---|
| Two Lambda-style endpoints | GET / serves form · POST /watermark processes PDF |
| Smart file size strategy | Under 10 MB → in-memory, direct download · 10 MB+ → S3 (LocalStack), signed URL |
| Graceful error handling | Every error returns structured JSON with error code |
| PDF magic-byte validation | Checks actual file bytes not just MIME type |
| Engineering patterns | Strategy, Repository, Middleware |

---

## 🏗️ Architecture

```
Browser
  │
  ├── GET  /           → FormController      → HTML upload form
  └── POST /watermark  → WatermarkController
                              │
                              ├── Validate: name + file + magic bytes
                              ├── Pick strategy by file size
                              │
                              ├── Small (< 10MB) → PdfService → return PDF bytes
                              └── Large (≥ 10MB) → StorageService → S3 → PdfService → signed URL
```

### Engineering Patterns

- **Strategy Pattern** — `FileProcessingStrategy` interface with `SmallFileStrategy` and `LargeFileStrategy`. Controller picks based on file size. Adding a new strategy = zero changes to controller.
- **Repository Pattern** — `StorageService` abstracts all S3 calls. Swap LocalStack for real AWS by changing one file.
- **Middleware Pattern** — `GlobalExceptionHandler` intercepts all exceptions, returns structured JSON. Controllers never write error code.

---

## 🚀 How to Clone and Run Locally

### Prerequisites

Install **Docker Desktop** only. That is all you need.
👉 https://www.docker.com/products/docker-desktop/

You do NOT need Java, Maven, or anything else. Docker handles everything.

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/Abhxay/pdf-watermark-service.git
cd pdf-watermark-service
```

---

### Step 2 — Start Everything

```bash
docker compose up --build
```

This command:
1. Downloads LocalStack (fake AWS S3) — port 4566
2. Compiles the Spring Boot app with Maven inside Docker
3. Starts your app — port 8080
4. Creates the S3 bucket automatically

First run takes about 2 minutes (Maven downloads dependencies).

Wait until you see this line in the logs:
```
pdf-watermark-app  | Started PdfWatermarkApplication in X.XXX seconds
```

---

### Step 3 — Open in Browser

```
http://localhost:8080
```

Enter your name, pick a PDF, click submit.

- **PDF under 10 MB** → watermarked PDF downloads directly to your browser
- **PDF 10 MB or above** → JSON response with a signed S3 download URL

---

### Step 4 — Stop

```bash
docker compose down
```

---

### Running Again (After First Build)

```bash
docker compose up
```

Only use `--build` if you changed code.

---

## 📡 API Reference

### GET /
Returns the HTML form.

### POST /watermark

**Content-Type:** `multipart/form-data`

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | ✅ | Watermark text, max 50 chars |
| `pdf` | file | ✅ | PDF only, max 100 MB |

**Small file response (200):**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="watermarked_yourfile.pdf"
[PDF bytes]
```

**Large file response (202):**
```json
{
  "success": true,
  "message": "Large file processed and stored in S3.",
  "jobId": "uuid",
  "strategy": "async-s3",
  "fileSize": "25.40 MB",
  "downloadUrl": "http://localhost:4566/...",
  "expiresIn": "1 hour"
}
```

**Error response (all errors):**
```json
{ "success": false, "error": "What went wrong", "code": "ERROR_CODE" }
```

| Code | HTTP | Meaning |
|---|---|---|
| `MISSING_NAME` | 400 | Name field empty |
| `NAME_TOO_LONG` | 400 | Name over 50 chars |
| `MISSING_FILE` | 400 | No file uploaded |
| `INVALID_FILE_TYPE` | 400 | Not a PDF |
| `INVALID_PDF_CONTENT` | 400 | Fails magic-byte check |
| `CORRUPT_PDF` | 400 | PDFBox could not parse it |
| `EMPTY_PDF` | 400 | PDF has zero pages |
| `FILE_TOO_LARGE` | 413 | File over 100 MB |
| `STORAGE_UNAVAILABLE` | 503 | LocalStack not running |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected error |

### GET /health
```json
{ "status": "ok", "service": "pdf-watermark-service", "stack": "Java + Spring Boot + Apache PDFBox" }
```

---

## 📁 Project Structure

```
pdf-watermark-service/
├── src/main/java/com/geminid/watermark/
│   ├── PdfWatermarkApplication.java       ← Entry point
│   ├── controller/
│   │   ├── FormController.java            ← GET / and GET /health
│   │   └── WatermarkController.java       ← POST /watermark
│   ├── strategy/
│   │   ├── FileProcessingStrategy.java    ← Interface
│   │   ├── SmallFileStrategy.java         ← In-memory processing
│   │   └── LargeFileStrategy.java         ← S3 storage + signed URL
│   ├── service/
│   │   ├── PdfService.java                ← Apache PDFBox watermark logic
│   │   └── StorageService.java            ← S3 / LocalStack (Repository pattern)
│   └── exception/
│       └── GlobalExceptionHandler.java    ← Structured error responses
├── src/main/resources/
│   └── application.properties             ← Config
├── localstack/
│   └── init-aws.sh                        ← Auto-creates S3 bucket
├── Dockerfile                             ← Multi-stage: Maven build + JRE run
├── docker-compose.yml                     ← App + LocalStack orchestration
├── pom.xml                                ← Maven dependencies
└── README.md
```

---

## 📦 Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Web framework |
| Apache PDFBox | 3.0.2 | PDF watermarking (Java alternative to pdf-lib.js) |
| AWS SDK v2 | 2.25.40 | S3 client |
| LocalStack | latest | Local AWS S3 emulator |
| Docker + Compose | — | Containerisation |
| Maven | 3.9.6 | Build tool |

---

## 📄 License
MIT © 2026 Abhay Thakur
