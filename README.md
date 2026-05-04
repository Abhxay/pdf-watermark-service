# 📄 PDF Watermark Service

A PDF watermarking microservice built with **Java 21**, **Spring Boot**, **Apache PDFBox**, **MiniStack**, and **Docker**.

Upload any PDF, enter your name, and get it back with your name stamped diagonally across every page. Small files open directly in the browser. Large files are stored in MiniStack (local S3) and a signed download URL is returned.

---

## 🤔 Why These Tools? (Alternatives Explained)

| Requirement | Tool Used | Original Suggestion | Why This One? |
|---|---|---|---|
| PDF manipulation | **Apache PDFBox 3.x** | pdf-lib.js | pdf-lib.js is JavaScript only. Apache PDFBox is the Java equivalent. Same concept: open PDF, draw text on every page, save it. Apache Foundation maintained. |
| Local AWS simulation | **MiniStack** | Paid cloud emulators | MiniStack is free, MIT-licensed, runs locally on port 4566, and speaks the same S3 API. No account, no license, no cost. |
| Language | **Java 21 + Spring Boot** | Node.js | Java is the developer primary language. Code you own is code you can defend. |
| Build | **Maven** | npm | Standard Java build tool. Same role as npm for Node. |

---

## ✨ Features

| Feature | Detail |
|---|---|
| 🔀 Two lambda-style endpoints | GET / serves the upload form · POST /watermark processes the PDF |
| 🧠 Smart file strategy | Under 10 MB: processed in memory, opens in browser · 10 MB+: stored in MiniStack S3, signed URL returned |
| 🔄 Reset flow | After each watermark, a success screen appears with a button to watermark another PDF |
| 🛡️ Graceful error handling | Every error returns structured JSON: `{ success, error, code }` |
| ✅ PDF validation | Checks magic bytes (%PDF) not just MIME type |
| 🏗️ Engineering patterns | Strategy, Repository, Middleware |

---

## 🏛️ Architecture

```
Browser
  |
  |-- GET  /           --> FormController      --> HTML upload form
  +-- POST /watermark  --> WatermarkController
                              |
                              |-- Validate: name + file + magic bytes
                              |-- Pick strategy by file size
                              |
                              |-- Small (< 10MB) --> PdfService --> PDF bytes (opens inline in browser)
                              +-- Large (>=10MB) --> StorageService --> MiniStack S3 --> PdfService --> signed URL
```

### 🧩 Engineering Patterns

- **Strategy Pattern** — `FileProcessingStrategy` interface with `SmallFileStrategy` and `LargeFileStrategy`. Add a new strategy without touching the controller.
- **Repository Pattern** — `StorageService` abstracts all S3 operations. Swap MiniStack for real AWS by changing config only.
- **Middleware Pattern** — `GlobalExceptionHandler` with `@RestControllerAdvice` catches every unhandled exception and returns structured JSON.

---

## 📁 Project Structure

```
pdf-watermark-service/
├── src/main/java/com/geminid/watermark/
│   ├── PdfWatermarkApplication.java        ← 🚀 Spring Boot entry point
│   ├── controller/
│   │   ├── FormController.java             ← 🌐 GET / (HTML form) + GET /health
│   │   └── WatermarkController.java        ← 📮 POST /watermark
│   ├── strategy/
│   │   ├── FileProcessingStrategy.java     ← 🔌 Interface (Strategy pattern)
│   │   ├── SmallFileStrategy.java          ← 🐤 <10MB: in memory, return blob
│   │   └── LargeFileStrategy.java          ← 🐘 >=10MB: S3, return signed URL
│   ├── service/
│   │   ├── PdfService.java                 ← 🖊️ Apache PDFBox watermark logic
│   │   └── StorageService.java             ← 🪣 All S3/MiniStack operations
│   └── exception/
│       ├── GlobalExceptionHandler.java     ← 🚨 All errors → structured JSON
│       └── InvalidPdfException.java        ← ⚠️ Custom exception for bad PDFs
├── ministack/init-aws.sh                   ← 🪄 Creates S3 bucket on startup
├── Dockerfile                              ← 🐳 Multi-stage Maven build + JRE
├── docker-compose.yml                      ← 🎼 App + MiniStack containers
├── pom.xml
└── README.md
```

---

## 📡 API Reference

### GET /

Returns the HTML upload form.

### GET /health

Returns service status.

```json
{
  "status": "ok",
  "service": "pdf-watermark-service",
  "stack": "Java 21 + Spring Boot + Apache PDFBox + MiniStack"
}
```

### POST /watermark

**Content-Type:** `multipart/form-data`

| Field | Type | Notes |
|---|---|---|
| name | string | Watermark text, max 50 characters |
| pdf | file | PDF only, max 100 MB |

**✅ Small file response (200):** PDF bytes with `Content-Disposition: inline` — browser opens it in a new tab.

**✅ Large file response (202):**

```json
{
  "success": true,
  "message": "Large file processed and stored in S3.",
  "jobId": "uuid",
  "strategy": "async-s3",
  "watermarkText": "Your Name",
  "fileSize": "12.34 MB",
  "downloadUrl": "http://...",
  "expiresIn": "1 hour"
}
```

**❌ All error responses:**

```json
{ "success": false, "error": "Human-readable message", "code": "MACHINE_CODE" }
```

### 🚨 Error Codes

| Code | HTTP | When |
|---|---|---|
| MISSING_NAME | 400 | Name field is empty |
| NAME_TOO_LONG | 400 | Name exceeds 50 characters |
| MISSING_FILE | 400 | No file attached |
| INVALID_FILE_TYPE | 400 | File is not a PDF |
| INVALID_PDF_CONTENT | 400 | Fails magic-byte check (`%PDF`) |
| CORRUPT_PDF | 400 | PDFBox cannot parse the file |
| EMPTY_PDF | 400 | PDF has zero pages |
| FILE_TOO_LARGE | 413 | File exceeds 100 MB |
| STORAGE_UNAVAILABLE | 503 | MiniStack is not reachable |
| INTERNAL_SERVER_ERROR | 500 | Unexpected error |

---

## 🚀 How to Clone and Run Locally

### 📋 Prerequisites

Install **Docker Desktop** only. That is all you need.

https://www.docker.com/products/docker-desktop/

You do NOT need Java, Maven, or anything else installed locally.

---

### Step 1 — 📥 Clone

```bash
git clone https://github.com/Abhxay/pdf-watermark-service.git
cd pdf-watermark-service
```

### Step 2 — ▶️ Start

```bash
docker compose up --build
```

This starts two containers:

- 🟢 `pdf-watermark-app` — your Spring Boot app on port 8080
- 🟢 `pdf-watermark-ministack` — MiniStack (local S3 emulator) on port 4566

The first run takes about 2 minutes while Maven downloads dependencies. Wait for this line:

```
pdf-watermark-app  | Started PdfWatermarkApplication in X.XXX seconds
```

### Step 3 — 🌐 Open

```
http://localhost:8080
```

1. ✏️ Enter your name
2. 📎 Pick any PDF
3. 🖊️ Click **Add Watermark**

Small files (under 10 MB) open directly in a new browser tab. Large files return a signed S3 download link valid for 1 hour.

---

## 🔧 Running Without Docker

If you have Java 21 and Maven installed locally:

1. Start MiniStack separately on port 4566
2. In `application.properties`, set `aws.endpoint` to `http://localhost:4566`
3. Run `mvn spring-boot:run`

No other code changes needed — all config reads from environment variables with sensible defaults.

---

## ☁️ Deploying to Real AWS

In `StorageService.java`, remove the `.endpointOverride(...)` line from both the `S3Client` and `S3Presigner` builders. Supply real IAM credentials via environment variables. Everything else is identical — the SDK calls do not change.

---

## 🛠️ Tech Stack

| Tool | Version |
|---|---|
| ☕ Java | 21 |
| 🍃 Spring Boot | 3.2.5 |
| 📄 Apache PDFBox | 3.0.2 |
| ☁️ AWS SDK v2 | 2.25.40 |
| 🪣 MiniStack | latest |
| 🐳 Docker + Compose | — |
| 📦 Maven | 3.9.6 |
