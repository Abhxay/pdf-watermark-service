package com.geminid.watermark.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lambda 1 — Form Handler
 * GET /
 * Serves the HTML upload form. No template engine needed — just returns plain HTML.
 */
@RestController
public class FormController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String showForm() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>PDF Watermark Service</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: #f0f2f5;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .card {
                        background: white;
                        border-radius: 12px;
                        padding: 40px;
                        max-width: 520px;
                        width: 100%;
                        box-shadow: 0 4px 24px rgba(0,0,0,0.08);
                    }
                    h1 { font-size: 24px; color: #1a1a2e; margin-bottom: 8px; }
                    .subtitle { color: #6b7280; font-size: 14px; margin-bottom: 24px; }
                    .info-box {
                        background: #eff6ff;
                        border: 1px solid #bfdbfe;
                        border-radius: 8px;
                        padding: 12px 16px;
                        margin-bottom: 24px;
                        font-size: 13px;
                        color: #1e40af;
                        line-height: 1.6;
                    }
                    .form-group { margin-bottom: 20px; }
                    label {
                        display: block;
                        font-size: 13px;
                        font-weight: 600;
                        color: #374151;
                        margin-bottom: 6px;
                    }
                    input[type="text"], input[type="file"] {
                        width: 100%;
                        padding: 10px 14px;
                        border: 1.5px solid #e5e7eb;
                        border-radius: 8px;
                        font-size: 14px;
                        color: #111827;
                    }
                    input[type="text"]:focus {
                        outline: none;
                        border-color: #3b82f6;
                    }
                    .hint { font-size: 12px; color: #9ca3af; margin-top: 4px; }
                    button {
                        width: 100%;
                        padding: 12px;
                        background: linear-gradient(135deg, #3b82f6, #1d4ed8);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 15px;
                        font-weight: 600;
                        cursor: pointer;
                        margin-top: 8px;
                    }
                    button:disabled { opacity: 0.6; cursor: not-allowed; }
                    #status { margin-top: 14px; font-size: 13px; text-align: center; color: #6b7280; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>🔒 PDF Watermark Service</h1>
                    <p class="subtitle">Enter your name and upload a PDF. Your name will be stamped diagonally across every page.</p>

                    <div class="info-box">
                        📄 <strong>Files under 10 MB</strong> — processed instantly, returned directly.<br/>
                        ☁️ <strong>Files over 10 MB</strong> — stored in S3 (LocalStack), download link returned.
                    </div>

                    <form id="watermarkForm" action="/watermark" method="POST" enctype="multipart/form-data">
                        <div class="form-group">
                            <label for="name">Your Name <span style="color:#ef4444">*</span></label>
                            <input type="text" id="name" name="name" required
                                   placeholder="e.g. Abhay Thakur" maxlength="50"/>
                            <p class="hint">This text appears as a watermark on every page.</p>
                        </div>
                        <div class="form-group">
                            <label for="pdf">PDF File <span style="color:#ef4444">*</span></label>
                            <input type="file" id="pdf" name="pdf" accept=".pdf,application/pdf" required/>
                            <p class="hint">Max 100 MB. Only PDF files accepted.</p>
                        </div>
                        <button type="submit" id="submitBtn">Add Watermark →</button>
                    </form>
                    <p id="status"></p>
                </div>

                <script>
                    document.getElementById('watermarkForm').addEventListener('submit', function(e) {
                        var btn = document.getElementById('submitBtn');
                        var status = document.getElementById('status');
                        var file = document.getElementById('pdf').files[0];
                        btn.disabled = true;
                        btn.textContent = 'Processing...';
                        status.textContent = file && file.size > 10 * 1024 * 1024
                            ? '⏳ Large file — uploading to S3...'
                            : '⏳ Adding watermark...';
                    });
                </script>
            </body>
            </html>
            """;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public String health() {
        return "{\"status\":\"ok\",\"service\":\"pdf-watermark-service\",\"stack\":\"Java + Spring Boot + Apache PDFBox\"}";
    }
}
