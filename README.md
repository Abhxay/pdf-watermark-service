# 📄 PDF Watermark Service

A PDF watermarking microservice built with **Java 21**, **Spring Boot**, **Apache PDFBox**, **MiniStack**, and **Docker**.

Upload any PDF, enter your name → get it back with your name stamped diagonally across every page.

> 🔗 **Repo:** https://github.com/Abhxay/pdf-watermark-service

---

## 🛠️ Why These Tools?

| Requirement | Tool | Why |
|---|---|---|
| PDF manipulation | **Apache PDFBox 3.x** | Java-native. Open PDF, draw text on every page, save. Apache Foundation maintained. |
| Local S3 | **MiniStack** | Free, MIT-licensed, same API as AWS S3. Runs on port 4566. No account needed. |
| Language | **Java 21 + Spring Boot** | Primary language. Code you own is code you can defend. |
| Build | **Maven** | Standard Java build tool. |

---

## ⚙️ How It Works

```
POST /watermark
       |
       |-- Validate: name + file + magic bytes (%PDF)
       |-- Pick strategy by file size
       |
       |-- Small  (<10MB)  --> PdfService --> PDF bytes (opens in browser)
       +-- Large  (>=10MB) --> StorageService --> MiniStack S3 --> signed URL
```

**Engineering Patterns:**
- **Strategy** — `SmallFileStrategy` / `LargeFileStrategy`. Add new strategies without touching the controller.
- **Repository** — `StorageService` wraps all S3 ops. Swap MiniStack for real AWS by changing config only.
- **Middleware** — `GlobalExceptionHandler` catches every exception → structured JSON.

---

## ✨ Features

- Smart file routing — under 10 MB opens in browser, over 10 MB returns a signed S3 link (1 hour expiry)
- Magic byte validation — checks `%PDF` bytes, not just MIME type
- Password-protected PDF detection — rejected with `400 PASSWORD_PROTECTED_PDF`
- PDF/A support — transparency disabled, lighter grey used to stay archival-compliant
- S3 lifecycle management — originals deleted after 1 day, watermarked results after 7 days
- Storage verification endpoint — `GET /admin/storage/check?key=...`
- Structured error responses — every error returns `{ success, error, code }`

---

## 🚀 Quick Start

**Prerequisites:** Docker Desktop only. No Java or Maven needed.

```bash
git clone https://github.com/Abhxay/pdf-watermark-service.git
cd pdf-watermark-service
docker compose up --build
```

Wait for:
```
pdf-watermark-app | Started PdfWatermarkApplication in X.XXX seconds
```

Open **http://localhost:8080**, enter your name, pick a PDF, click **Add Watermark**.

---

## 📡 API Reference

### `POST /watermark`
`Content-Type: multipart/form-data`

| Field | Type | Notes |
|---|---|---|
| `name` | string | Watermark text, max 50 chars |
| `pdf` | file | PDF only, max 100 MB |

**Small file response `200`** — PDF bytes, opens inline in browser.

**Large file response `202`:**
```json
{
  "success": true,
  "jobId": "uuid",
  "strategy": "async-s3",
  "fileSize": "12.34 MB",
  "downloadUrl": "http://...",
  "expiresIn": "1 hour"
}
```

**All errors:**
```json
{ "success": false, "error": "Human-readable message", "code": "MACHINE_CODE" }
```

### ❌ Error Codes

| Code | HTTP | Cause |
|---|---|---|
| `MISSING_NAME` | 400 | Name field empty |
| `NAME_TOO_LONG` | 400 | Name over 50 chars |
| `INVALID_FILE_TYPE` | 400 | Not a PDF |
| `INVALID_PDF_CONTENT` | 400 | Fails `%PDF` magic byte check |
| `CORRUPT_PDF` | 400 | PDFBox cannot parse file |
| `EMPTY_PDF` | 400 | PDF has zero pages |
| `PASSWORD_PROTECTED_PDF` | 400 | PDF is encrypted |
| `FILE_TOO_LARGE` | 413 | Over 100 MB |
| `STORAGE_UNAVAILABLE` | 503 | MiniStack unreachable |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected error |

### `GET /admin/storage/check?key=...`
Checks if a file exists in S3.
```json
{ "key": "...", "exists": true,  "status": "PRESENT" }
{ "key": "...", "exists": false, "status": "DELETED_OR_NOT_FOUND" }
```

---

## 🗑️ S3 Lifecycle Management

Files auto-delete via `putBucketLifecycleConfiguration`, configured at startup.

| Prefix | Expires | Reason |
|---|---|---|
| `uploads/` | 1 day | Original has no value after watermarking |
| `processed/` | 7 days | Buffer for re-downloads (signed URL expires in 1hr anyway) |

**Tuning for production:**
- Drop `processed/` to 1 day if analytics show no re-downloads after hour 2
- Drop both to 1 day if handling PII or legally sensitive documents
- Add a CloudWatch alarm if bucket size exceeds a cost threshold

> ⚠️ **MiniStack vs real AWS:** MiniStack doesn't run lifecycle expiry on a timer. Real AWS fires rules at midnight UTC. Use the manual steps below to verify locally.

### Verifying Deletion

```bash
# 1. Get the jobId from logs
docker logs pdf-watermark-app --follow

# 2. Confirm file exists
curl "http://localhost:8080/admin/storage/check?key=processed/JOB-ID/watermarked_file.pdf"

# 3. List everything in the bucket
curl "http://localhost:4566/pdf-watermark-bucket?list-type=2"

# 4. Simulate lifecycle deletion
docker exec pdf-watermark-ministack awslocal s3 rm "s3://pdf-watermark-bucket/processed/JOB-ID/watermarked_file.pdf"

# 5. Confirm gone
curl "http://localhost:8080/admin/storage/check?key=processed/JOB-ID/watermarked_file.pdf"
# → { "exists": false, "status": "DELETED_OR_NOT_FOUND" }
```

> 🪟 **Windows PowerShell:** Use `(curl "http://...").Content` instead of bare `curl`.

---

## 🧪 Tests

Tests run without Docker — Mockito mocks and in-memory PDFs only.

```bash
mvn test                                      # all tests
mvn test -Dtest=ConcurrentWatermarkTest       # specific class
mvn test -Dtest=ConcurrentWatermarkTest -Dsurefire.useFile=false  # with console output
```

| Test Class | Tests | What It Covers |
|---|---|---|
| `PdfServiceTest` | 9 | Real in-memory PDFs — single page, 50 pages, garbage bytes, password-protected, special chars |
| `StorageServiceTest` | 5 | S3 ops via Mockito — putObject key/content-type, objectExists true/false, bucket creation |
| `WatermarkControllerTest` | 6 | MockMvc — all validation paths, error response structure |
| `SmallFileStrategyTest` | 4 | Response shape — Content-Type, Content-Disposition inline, body bytes, status 200 |
| `LargeFileStrategyTest` | 5 | S3 key prefixes, downloadUrl in response, status 202, unique jobIds |
| `GlobalExceptionHandlerTest` | 5 | All exception → HTTP status mappings |
| `ConcurrentWatermarkTest` | 5 | 20 threads simultaneously, 100 req/sec benchmark, OOM check, result isolation |

The scalability test prints timing to console:
```
100 sequential: 1243 ms (80.4 req/sec)
10-page watermark: 87 ms
```

---

## ☁️ Deploying to Real AWS

Two changes only:

**1.** Remove `endpointOverride` from `StorageService.java`:
```java
// Remove this line:
.endpointOverride(URI.create(endpoint))
```

**2.** Remove `credentialsProvider` and let the SDK use the IAM role attached to your EC2/ECS task.

Everything else — lifecycle rules, presigned URLs, bucket config — works identically on real AWS. Same SDK calls, same API.

---

## 📁 Project Structure

```
src/main/java/com/geminid/watermark/
├── controller/
│   ├── FormController.java          GET /  +  GET /health
│   ├── WatermarkController.java     POST /watermark
│   └── AdminController.java         GET /admin/storage/check
├── strategy/
│   ├── FileProcessingStrategy.java  Interface
│   ├── SmallFileStrategy.java       <10MB → in-memory → blob
│   └── LargeFileStrategy.java       >=10MB → S3 → signed URL
├── service/
│   ├── PdfService.java              PDFBox watermark logic
│   └── StorageService.java          All S3 operations + lifecycle
└── exception/
    ├── GlobalExceptionHandler.java  All errors → structured JSON
    └── InvalidPdfException.java

src/test/java/com/geminid/watermark/
├── service/         PdfServiceTest, StorageServiceTest
├── controller/      WatermarkControllerTest
├── strategy/        SmallFileStrategyTest, LargeFileStrategyTest
├── exception/       GlobalExceptionHandlerTest
└── scalability/     ConcurrentWatermarkTest
```

---

*Java 21 · Spring Boot 3.2.5 · Apache PDFBox 3.0.2 · MiniStack · Docker*
