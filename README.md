# PDF Watermark Service

A PDF watermarking microservice built with **Java 21**, **Spring Boot**, **Apache PDFBox**, **MiniStack**, and **Docker**.

Upload any PDF, enter your name, and get it back with your name stamped diagonally across every page. Small files open directly in the browser. Large files are stored in MiniStack (local S3) and a signed download URL is returned.

---

## Why These Tools? (Alternatives Explained)

| Requirement | Tool Used | Original Suggestion | Why This One? |
|---|---|---|---|
| PDF manipulation | **Apache PDFBox 3.x** | pdf-lib.js | pdf-lib.js is JavaScript only. Apache PDFBox is the Java equivalent. Same concept: open PDF, draw text on every page, save it. Apache Foundation maintained. |
| Local AWS simulation | **MiniStack** | LocalStack | LocalStack moved core services behind a paid plan in early 2026. MiniStack is the free, MIT-licensed drop-in replacement. Same port (4566), same API, same SDK calls. Zero code changes needed. |
| Language | **Java 21 + Spring Boot** | Node.js | Java is the developer primary language. Code you own is code you can defend. |
| Build | **Maven** | npm | Standard Java build tool. Same role as npm for Node. |

---

## Features

| Feature | Detail |
|---|---|
| Two lambda-style endpoints | GET / serves the upload form · POST /watermark processes the PDF |
| Smart file strategy | Under 10 MB: processed in memory, opens in browser · 10 MB+: stored in MiniStack S3, signed URL returned |
| Reset flow | After each watermark, a success screen appears with a button to watermark another PDF |
| Graceful error handling | Every error returns structured JSON: { success, error, code } |
| PDF validation | Checks magic bytes (%PDF) not just MIME type |
| Engineering patterns | Strategy, Repository, Middleware |

---

## Architecture

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

### Engineering Patterns

- **Strategy Pattern** - FileProcessingStrategy interface with SmallFileStrategy and LargeFileStrategy
- - **Repository Pattern** - StorageService abstracts all S3 operations. Swap MiniStack for real AWS by changing config only
  - - **Middleware Pattern** - GlobalExceptionHandler returns structured JSON for every error
   
    - ---

    ## How to Clone and Run Locally

    ### Prerequisites

    Install **Docker Desktop** only. That is all you need.
    https://www.docker.com/products/docker-desktop/

    You do NOT need Java, Maven, or anything else installed.

    ---

    ### Step 1 - Clone

    ```bash
    git clone https://github.com/Abhxay/pdf-watermark-service.git
    cd pdf-watermark-service
    ```

    ### Step 2 - Start

    ```bash
    docker compose up --build
    ```

    This starts two containers:
    - Your Spring Boot app on port 8080
    - - MiniStack (local AWS S3 emulator) on port 4566
     
      - First run takes about 2 minutes for Maven to download dependencies.
     
      - Wait for this line:
      - ```
        pdf-watermark-app  | Started PdfWatermarkApplication in X.XXX seconds
        ```

        ### Step 3 - Open

        ```
        http://localhost:8080
        ```

        - Enter your name
        - - Pick any PDF
          - -
