PDF WATERMARK SERVICE
=====================
Java 21 · Spring Boot 3.2.5 · Apache PDFBox 3.0.2 · MiniStack · Docker

Upload any PDF, enter your name, and get it back with your name stamped
diagonally across every page. Small files open directly in the browser.
Large files are stored in MiniStack (local S3) and a signed download URL
is returned.

Repo: https://github.com/Abhxay/pdf-watermark-service

-------------------------------------------------------------------------------
WHY THESE TOOLS?
-------------------------------------------------------------------------------

Requirement          Tool Used                Why
-----------          ---------                ---
PDF manipulation     Apache PDFBox 3.x        pdf-lib.js is JavaScript only.
                                              PDFBox is the Java equivalent.
                                              Same concept: open PDF, draw
                                              text on every page, save it.
                                              Apache Foundation maintained.

Local AWS            MiniStack                Free, MIT-licensed, runs locally
simulation                                    on port 4566, speaks the same S3
                                              API. No account, no license,
                                              no cost.

Language             Java 21 + Spring Boot    Java is the primary language.
                                              Code you own is code you can
                                              defend.

Build                Maven                    Standard Java build tool.
                                              Same role as npm for Node.

-------------------------------------------------------------------------------
FEATURES
-------------------------------------------------------------------------------

- Two endpoints
  GET  /           serves the upload form
  POST /watermark  processes the PDF

- Smart file strategy
  Under 10 MB  : processed in memory, opens in browser
  10 MB and up : stored in MiniStack S3, signed URL returned

- Reset flow
  After each watermark a success screen appears with a button to
  watermark another PDF

- Graceful error handling
  Every error returns structured JSON: { success, error, code }

- PDF validation
  Checks magic bytes (%PDF) not just MIME type

- PDF type handling                                                     [NEW]
  Password-protected PDFs are detected and rejected with a clear error
  PDF/A archival format is detected via XMP metadata — transparency is
  skipped and a lighter grey is used instead to stay compliant

- S3 lifecycle management                                               [NEW]
  Uploaded originals auto-delete after 1 day
  Watermarked results auto-delete after 7 days

- Storage verification endpoint                                         [NEW]
  GET /admin/storage/check?key=... confirms whether a file exists in S3

- Engineering patterns
  Strategy, Repository, Middleware

-------------------------------------------------------------------------------
ARCHITECTURE
-------------------------------------------------------------------------------

Browser
  |
  |-- GET  /           --> FormController      --> HTML upload form
  +-- POST /watermark  --> WatermarkController
                               |
                               |-- Validate: name + file + magic bytes
                               |-- Pick strategy by file size
                               |
                               |-- Small (<10MB)  --> PdfService --> PDF bytes
                               +-- Large (>=10MB) --> StorageService
                                                          |
                                                          +--> MiniStack S3
                                                          +--> PdfService
                                                          +--> signed URL

Engineering Patterns
--------------------
Strategy   - FileProcessingStrategy interface with SmallFileStrategy and
             LargeFileStrategy. Add a new strategy without touching the
             controller.

Repository - StorageService abstracts all S3 operations. Swap MiniStack
             for real AWS by changing config only.

Middleware - GlobalExceptionHandler with @RestControllerAdvice catches
             every unhandled exception and returns structured JSON.

-------------------------------------------------------------------------------
PROJECT STRUCTURE
-------------------------------------------------------------------------------

pdf-watermark-service/
├── src/main/java/com/geminid/watermark/
│   ├── PdfWatermarkApplication.java
│   ├── controller/
│   │   ├── FormController.java             GET / and GET /health
│   │   ├── WatermarkController.java        POST /watermark
│   │   └── AdminController.java            GET /admin/storage/check  [NEW]
│   ├── strategy/
│   │   ├── FileProcessingStrategy.java     Interface (Strategy pattern)
│   │   ├── SmallFileStrategy.java          <10MB: in memory, return blob
│   │   └── LargeFileStrategy.java          >=10MB: S3, return signed URL
│   ├── service/
│   │   ├── PdfService.java                 PDFBox watermark logic        [UPDATED]
│   │   └── StorageService.java             All S3/MiniStack operations   [UPDATED]
│   └── exception/
│       ├── GlobalExceptionHandler.java     All errors -> structured JSON [UPDATED]
│       └── InvalidPdfException.java        Custom exception for bad PDFs
├── src/test/java/com/geminid/watermark/
│   ├── service/
│   │   ├── PdfServiceTest.java                                           [NEW]
│   │   └── StorageServiceTest.java                                       [NEW]
│   ├── controller/
│   │   └── WatermarkControllerTest.java                                  [NEW]
│   ├── strategy/
│   │   ├── SmallFileStrategyTest.java                                    [NEW]
│   │   └── LargeFileStrategyTest.java                                    [NEW]
│   ├── exception/
│   │   └── GlobalExceptionHandlerTest.java                               [NEW]
│   └── scalability/
│       └── ConcurrentWatermarkTest.java                                  [NEW]
├── ministack/init-aws.sh
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.txt

-------------------------------------------------------------------------------
API REFERENCE
-------------------------------------------------------------------------------

GET /
Returns the HTML upload form.

GET /health
Returns service status.
  { "status": "ok", "service": "pdf-watermark-service", "stack": "..." }

POST /watermark
Content-Type: multipart/form-data

  Field   Type    Notes
  -----   ----    -----
  name    string  Watermark text, max 50 characters
  pdf     file    PDF only, max 100 MB

Small file response (200):
  PDF bytes with Content-Disposition: inline
  Browser opens it directly in a new tab

Large file response (202):
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

All error responses:
  { "success": false, "error": "Human-readable message", "code": "CODE" }

Error Codes
-----------
MISSING_NAME            400   Name field is empty
NAME_TOO_LONG           400   Name exceeds 50 characters
MISSING_FILE            400   No file attached
INVALID_FILE_TYPE       400   File is not a PDF
INVALID_PDF_CONTENT     400   Fails magic-byte check (%PDF)
CORRUPT_PDF             400   PDFBox cannot parse the file
EMPTY_PDF               400   PDF has zero pages
PASSWORD_PROTECTED_PDF  400   PDF is encrypted with a password           [NEW]
FILE_TOO_LARGE          413   File exceeds 100 MB
STORAGE_UNAVAILABLE     503   MiniStack is not reachable
INTERNAL_SERVER_ERROR   500   Unexpected error

GET /admin/storage/check?key=...                                         [NEW]
Checks whether a specific file exists in the S3 bucket.
  { "key": "...", "exists": true,  "status": "PRESENT" }
  { "key": "...", "exists": false, "status": "DELETED_OR_NOT_FOUND" }

-------------------------------------------------------------------------------
HOW TO CLONE AND RUN LOCALLY
-------------------------------------------------------------------------------

Prerequisites
-------------
Install Docker Desktop only. That is all you need.
https://www.docker.com/products/docker-desktop/

You do NOT need Java, Maven, or anything else installed locally.

Step 1 - Clone
--------------
  git clone https://github.com/Abhxay/pdf-watermark-service.git
  cd pdf-watermark-service

Step 2 - Start
--------------
  docker compose up --build

This starts two containers:
  pdf-watermark-app        Spring Boot app on port 8080
  pdf-watermark-ministack  MiniStack local S3 emulator on port 4566

First run takes about 2 minutes for Maven to download dependencies.
Wait for this line:
  pdf-watermark-app | Started PdfWatermarkApplication in X.XXX seconds

Step 3 - Open
-------------
  http://localhost:8080

  1. Enter your name
  2. Pick any PDF
  3. Click Add Watermark

Small files open in a new browser tab.
Large files return a signed S3 download link valid for 1 hour.

-------------------------------------------------------------------------------
S3 LIFECYCLE MANAGEMENT                                                  [NEW]
-------------------------------------------------------------------------------

What it does
------------
Every file uploaded to MiniStack is automatically scheduled for deletion.
Rules are configured once at startup inside StorageService.init() using
the AWS SDK putBucketLifecycleConfiguration call.

Current rules
-------------
  uploads/    auto-deleted after 1 day
  processed/  auto-deleted after 7 days

Why these numbers
-----------------
uploads/ stores the original PDF before watermarking. Once the watermark
is applied that original has no value. 1 day is just a safety buffer in
case something needs to be debugged.

processed/ stores the watermarked result. The signed URL expires in 1 hour
so the file is already inaccessible after that. 7 days gives a window if
someone needs to regenerate the URL or retry a download.

How to tune for production
--------------------------
These numbers should be driven by real usage data.

Short-term tuning:
  If analytics show nobody re-downloads after hour 2, drop processed/ to
  1 day. If support tickets show users requesting files from 2 weeks ago,
  extend to 14 days.

Cost-based tuning:
  S3 storage costs money at scale. A 40 MB watermarked PDF stored for 7
  days costs more than one stored for 1 day. At high volume this adds up.
  Run a cost report after the first month and adjust accordingly.

Compliance-based tuning:
  If the service handles documents with PII or legal content, your data
  retention policy may require deletion within 24 hours. Drop both
  prefixes to 1 day and add a deletion audit log.

Production recommendation:
  uploads/    1 day  (no reason to keep longer)
  processed/  1 day  (signed URL already expired after 1 hour)
  Add a CloudWatch alarm to alert if bucket size exceeds a threshold.

To change the values, edit StorageService.java:
  .expiration(LifecycleExpiration.builder().days(1).build())

Note on MiniStack vs real AWS:
  MiniStack does not execute lifecycle expiry on a timer. In production
  on real AWS the rules fire automatically at midnight UTC. In local
  development use the manual delete steps below to simulate and verify.

-------------------------------------------------------------------------------
HANDLING DIFFERENT PDF TYPES                                             [NEW]
-------------------------------------------------------------------------------

Standard PDFs
  All versions 1.0 through 2.0 handled by PDFBox automatically.

Password-protected PDFs
  PDFBox throws InvalidPasswordException when loading an encrypted file.
  Caught and returned as HTTP 400 PASSWORD_PROTECTED_PDF with the message
  "PDF is password-protected. Please remove the password first."

PDF/A (archival format)
  PDF/A-1b forbids transparency operators. The service detects PDF/A via
  XMP metadata in the document catalog. If detected, alpha is skipped and
  a lighter grey (0.82) is used instead of 25% opacity. The watermark
  still appears — it uses a flat colour instead of transparency.

Zero-page PDFs
  Caught before watermarking. Returned as HTTP 400 EMPTY_PDF.

Corrupted PDFs
  PDFBox throws IOException during load. Caught by GlobalExceptionHandler.
  Returned as HTTP 400 CORRUPT_PDF.

-------------------------------------------------------------------------------
VERIFYING FILE DELETION                                                  [NEW]
-------------------------------------------------------------------------------

After uploading a large PDF, get the jobId from the Docker logs:
  docker logs pdf-watermark-app --follow

You will see:
  Large file job abc-123 complete. Key: processed/abc-123/watermarked_file.pdf

Step 1 - Confirm the file exists
  curl "http://localhost:8080/admin/storage/check?key=processed/JOB-ID/watermarked_file.pdf"
  Response: { "exists": true, "status": "PRESENT" }

Step 2 - View all files in the bucket
  curl "http://localhost:4566/pdf-watermark-bucket?list-type=2"
  Returns XML listing every key under uploads/ and processed/

Step 3 - Simulate lifecycle deletion manually
  docker exec pdf-watermark-ministack awslocal s3 rm "s3://pdf-watermark-bucket/processed/JOB-ID/watermarked_file.pdf"

Step 4 - Confirm the file is gone
  curl "http://localhost:8080/admin/storage/check?key=processed/JOB-ID/watermarked_file.pdf"
  Response: { "exists": false, "status": "DELETED_OR_NOT_FOUND" }

On Windows PowerShell use this syntax instead of bare curl:
  (curl "http://localhost:8080/admin/storage/check?key=...").Content

-------------------------------------------------------------------------------
RUNNING THE TESTS                                                        [NEW]
-------------------------------------------------------------------------------

Tests run without Docker. They use Mockito mocks and in-memory PDFs.

Run all tests
  mvn test

Run a specific test class
  mvn test -Dtest=PdfServiceTest
  mvn test -Dtest=StorageServiceTest
  mvn test -Dtest=WatermarkControllerTest
  mvn test -Dtest=SmallFileStrategyTest
  mvn test -Dtest=LargeFileStrategyTest
  mvn test -Dtest=GlobalExceptionHandlerTest
  mvn test -Dtest=ConcurrentWatermarkTest

See detailed console output
  mvn test -Dtest=ConcurrentWatermarkTest -Dsurefire.useFile=false

Expected output
  [INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS

The scalability test also prints timing to the console:
  100 sequential: 1243 ms (80.4 req/sec)
  10-page watermark: 87 ms

Test Coverage by Layer
----------------------

PdfServiceTest (9 tests)
  Tests core watermarking logic with real in-memory PDFs.
  Covers: single page, multi page, 50 pages, valid PDF magic bytes,
  zero-page PDF, null bytes, garbage bytes, 50-char name, special
  characters in name, password-protected PDF.

StorageServiceTest (5 tests)
  Tests S3 operations using Mockito mocks — no real S3 call is made.
  Covers: upload calls putObject with correct key and content type,
  upload called exactly once, objectExists returns true on 200,
  objectExists returns false on NoSuchKeyException, ensureBucketExists
  skips createBucket if bucket already exists and creates it if missing.

WatermarkControllerTest (6 tests)
  Tests HTTP request validation using MockMvc.
  Covers: empty name, whitespace-only name, name over 50 chars, wrong
  content type, bad magic bytes, valid request routes to small strategy.

SmallFileStrategyTest (4 tests)
  Verifies response shape for small files.
  Covers: Content-Type is application/pdf, Content-Disposition is inline
  with correct filename, body equals bytes from PdfService, status 200.

LargeFileStrategyTest (5 tests)
  Verifies S3 upload behaviour and response shape for large files.
  Covers: original uploaded to uploads/ prefix, watermarked result
  uploaded to processed/ prefix, response contains downloadUrl and
  success=true, status 202, two requests generate different S3 keys.

GlobalExceptionHandlerTest (5 tests)
  Verifies every exception maps to the correct HTTP status and code.
  Covers: EMPTY_PDF, PASSWORD_PROTECTED_PDF, CORRUPT_PDF,
  INTERNAL_SERVER_ERROR, STORAGE_UNAVAILABLE.

ConcurrentWatermarkTest (5 tests)
  Runs PdfService with real PDFs and real threads — no mocks.
  Covers: 20 threads firing simultaneously with zero failures,
  100 sequential requests under 10 seconds, 50-page PDF repeated
  10 times with no OutOfMemoryError, 10 concurrent results are
  all independent valid PDFs, single 10-page watermark under 500ms.

-------------------------------------------------------------------------------
COMMON BUGS AND FIXES
-------------------------------------------------------------------------------

Bug                          Cause                          Fix
---                          -----                          ---
Parameters must be           PDFBox 3.x color is float      Use 0.59f not 150
within 0..1                  not int

InvalidPdfException          Class referenced but           Add the file to
not found                    never created                  exception package

YAML indentation error       Browser typing corrupted       Rewrite
                             indentation                    docker-compose.yml manually

Git push rejected            Remote has commits not         git pull origin
                             in local                       main --rebase

Progress bar stuck           iframe approach unreliable     Switch to fetch()
                                                            and createObjectURL

Container is unhealthy       Healthcheck URL does not       Remove healthcheck,
                             exist on MiniStack             use restart: on-failure

Large file 500 error         Signed URL has internal        .replace(
                             Docker hostname                "http://ministack:4566",
                                                            "http://localhost:4566")

aws.endpoint space typo      http://ministack :4566         http://ministack:4566

-------------------------------------------------------------------------------
DEPLOYING TO REAL AWS
-------------------------------------------------------------------------------

Only two changes needed:

1. In StorageService.java remove the endpointOverride line:
     .endpointOverride(URI.create(endpoint))

2. Switch credentials from static test values to IAM roles.
   Remove the credentialsProvider line and let the SDK use the
   default credential chain (IAM role on the EC2 or ECS task).

application.properties already reads from environment variables so no
code changes are needed for region, bucket name, or other config.

Lifecycle rules, presigned URLs, and all S3 operations work identically
on real AWS — same SDK calls, same API.

-------------------------------------------------------------------------------
