package com.geminid.watermark.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: #f5f5f5;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 24px;
                    }

                    .card {
                        background: #ffffff;
                        border-radius: 16px;
                        padding: 36px;
                        width: 100%;
                        max-width: 480px;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 4px 16px rgba(0,0,0,0.06);
                    }

                    .header { margin-bottom: 28px; }

                    .icon-wrap {
                        width: 40px;
                        height: 40px;
                        border-radius: 10px;
                        background: #f0f0f0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin-bottom: 14px;
                    }

                    h1 {
                        font-size: 20px;
                        font-weight: 600;
                        color: #111;
                        margin-bottom: 6px;
                        letter-spacing: -0.3px;
                    }

                    .subtitle {
                        font-size: 14px;
                        color: #777;
                        line-height: 1.55;
                    }

                    .tags {
                        display: flex;
                        gap: 8px;
                        margin: 20px 0 28px;
                        flex-wrap: wrap;
                    }

                    .tag {
                        font-size: 11px;
                        padding: 4px 10px;
                        border-radius: 20px;
                        border: 1px solid #e8e8e8;
                        color: #666;
                        background: #fafafa;
                    }

                    .form-group { margin-bottom: 20px; }

                    label {
                        display: block;
                        font-size: 11px;
                        font-weight: 600;
                        color: #999;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                        margin-bottom: 8px;
                    }

                    input[type="text"] {
                        width: 100%;
                        padding: 11px 14px;
                        border: 1.5px solid #e8e8e8;
                        border-radius: 10px;
                        font-size: 14px;
                        color: #111;
                        background: #fff;
                        transition: border-color 0.15s;
                        outline: none;
                    }

                    input[type="text"]:focus { border-color: #111; }
                    input[type="text"]::placeholder { color: #bbb; }

                    .hint {
                        font-size: 12px;
                        color: #bbb;
                        margin-top: 6px;
                    }

                    /* File zone */
                    .file-zone {
                        border: 1.5px dashed #e0e0e0;
                        border-radius: 10px;
                        padding: 24px 16px;
                        text-align: center;
                        cursor: pointer;
                        transition: border-color 0.15s, background 0.15s;
                        background: #fafafa;
                        position: relative;
                    }

                    .file-zone:hover { border-color: #bbb; background: #f5f5f5; }

                    .file-zone.selected {
                        border-style: solid;
                        border-color: #111;
                        background: #f8f8f8;
                    }

                    .file-zone input[type="file"] {
                        position: absolute;
                        inset: 0;
                        width: 100%;
                        height: 100%;
                        opacity: 0;
                        cursor: pointer;
                    }

                    .file-icon {
                        width: 32px;
                        height: 32px;
                        margin: 0 auto 10px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #efefef;
                        border-radius: 8px;
                    }

                    .file-name {
                        font-size: 13px;
                        font-weight: 500;
                        color: #333;
                        margin-bottom: 2px;
                    }

                    .file-meta {
                        font-size: 12px;
                        color: #aaa;
                    }

                    /* Button */
                    button[type="submit"] {
                        width: 100%;
                        padding: 12px;
                        background: #111;
                        color: #fff;
                        border: none;
                        border-radius: 10px;
                        font-size: 14px;
                        font-weight: 500;
                        cursor: pointer;
                        margin-top: 4px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                        transition: opacity 0.15s, transform 0.1s;
                        position: relative;
                        overflow: hidden;
                    }

                    button[type="submit"]:hover { opacity: 0.85; }
                    button[type="submit"]:active { transform: scale(0.99); }

                    button[type="submit"]:disabled {
                        opacity: 0.35;
                        cursor: not-allowed;
                        transform: none;
                    }

                    /* Spinner */
                    .spinner {
                        width: 15px;
                        height: 15px;
                        border: 2px solid rgba(255,255,255,0.3);
                        border-top-color: #fff;
                        border-radius: 50%;
                        animation: spin 0.7s linear infinite;
                        display: none;
                    }

                    @keyframes spin { to { transform: rotate(360deg); } }

                    /* Progress */
                    .progress-wrap {
                        margin-top: 16px;
                        display: none;
                    }

                    .progress-track {
                        height: 2px;
                        background: #eee;
                        border-radius: 2px;
                        overflow: hidden;
                    }

                    .progress-bar {
                        height: 100%;
                        width: 0%;
                        background: #111;
                        border-radius: 2px;
                        transition: width 0.3s ease;
                    }

                    .progress-label {
                        font-size: 12px;
                        color: #aaa;
                        margin-top: 8px;
                        text-align: center;
                    }

                    .divider {
                        height: 1px;
                        background: #f0f0f0;
                        margin: 24px 0 18px;
                    }

                    .footer-note {
                        font-size: 11px;
                        color: #ccc;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <div class="icon-wrap">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                                 stroke="#555" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                                <polyline points="14 2 14 8 20 8"/>
                                <line x1="16" y1="13" x2="8" y2="13"/>
                                <line x1="16" y1="17" x2="8" y2="17"/>
                            </svg>
                        </div>
                        <h1>PDF Watermark Service</h1>
                        <p class="subtitle">Enter your name and upload a PDF. Your name will be stamped diagonally across every page.</p>
                    </div>

                    <div class="tags">
                        <span class="tag">Under 10 MB — instant download</span>
                        <span class="tag">Over 10 MB — S3 link returned</span>
                    </div>

                    <form id="watermarkForm" action="/watermark" method="POST" enctype="multipart/form-data">

                        <div class="form-group">
                            <label>Your name</label>
                            <input type="text" name="name" id="nameInput"
                                   placeholder="e.g. Abhay Thakur"
                                   maxlength="50" autocomplete="off"
                                   oninput="checkReady()"/>
                            <p class="hint">This text appears as a watermark on every page.</p>
                        </div>

                        <div class="form-group">
                            <label>PDF file</label>
                            <div class="file-zone" id="fileZone">
                                <input type="file" name="pdf" id="pdfInput"
                                       accept=".pdf,application/pdf"
                                       onchange="fileSelected(this)"/>
                                <div class="file-icon">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                         stroke="#888" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                        <polyline points="16 16 12 12 8 16"/>
                                        <line x1="12" y1="12" x2="12" y2="21"/>
                                        <path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"/>
                                    </svg>
                                </div>
                                <p class="file-name" id="fileName">Click to select a PDF</p>
                                <p class="file-meta" id="fileMeta">Max 100 MB · PDF only</p>
                            </div>
                        </div>

                        <button type="submit" id="submitBtn" disabled>
                            <div class="spinner" id="spinner"></div>
                            <span id="btnText">Add Watermark</span>
                        </button>

                        <div class="progress-wrap" id="progressWrap">
                            <div class="progress-track">
                                <div class="progress-bar" id="progressBar"></div>
                            </div>
                            <p class="progress-label" id="progressLabel">Processing...</p>
                        </div>

                    </form>

                    <div class="divider"></div>
                    <p class="footer-note">Java 21 · Spring Boot · Apache PDFBox · MiniStack · Docker</p>
                </div>

                <script>
                    function checkReady() {
                        var name = document.getElementById('nameInput').value.trim();
                        var file = document.getElementById('pdfInput').files[0];
                        document.getElementById('submitBtn').disabled = !(name && file);
                    }

                    function fileSelected(input) {
                        var file = input.files[0];
                        if (!file) return;
                        var zone = document.getElementById('fileZone');
                        zone.classList.add('selected');
                        document.getElementById('fileName').textContent = file.name;
                        var mb = (file.size / 1024 / 1024).toFixed(1);
                        document.getElementById('fileMeta').textContent = mb + ' MB · ' +
                            (file.size > 10 * 1024 * 1024 ? 'S3 link will be returned' : 'Will download directly');
                        checkReady();
                    }

                    document.getElementById('watermarkForm').addEventListener('submit', function() {
                        var btn = document.getElementById('submitBtn');
                        var spinner = document.getElementById('spinner');
                        var btnText = document.getElementById('btnText');
                        var wrap = document.getElementById('progressWrap');
                        var bar = document.getElementById('progressBar');
                        var label = document.getElementById('progressLabel');

                        btn.disabled = true;
                        spinner.style.display = 'block';
                        btnText.textContent = 'Processing...';
                        wrap.style.display = 'block';

                        var w = 0;
                        var msgs = ['Validating PDF...', 'Adding watermark...', 'Almost done...'];
                        var i = 0;
                        setInterval(function() {
                            w += Math.random() * 15 + 5;
                            if (w > 85) w = 85;
                            bar.style.width = w + '%';
                            if (i < msgs.length) label.textContent = msgs[i++];
                        }, 700);
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