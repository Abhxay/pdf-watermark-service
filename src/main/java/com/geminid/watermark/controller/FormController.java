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
                    :root {
                        --bg: #f4f3ef;
                        --card: #ffffff;
                        --ink: #1a1a2e;
                        --ink-mid: #4a4a6a;
                        --ink-soft: #9090aa;
                        --ink-ghost: #c8c8d8;
                        --accent: #4f46e5;
                        --accent-bg: #eef0ff;
                        --success: #059669;
                        --success-bg: #ecfdf5;
                        --error: #dc2626;
                        --error-bg: #fef2f2;
                        --border: #e8e7f0;
                        --radius: 12px;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        background: var(--bg);
                        min-height: 100vh;
                        display: flex; align-items: center; justify-content: center;
                        padding: 24px;
                    }
                    .card {
                        background: var(--card); border-radius: 18px; padding: 36px;
                        width: 100%; max-width: 480px;
                        box-shadow: 0 2px 8px rgba(79,70,229,0.07), 0 0 0 1px rgba(79,70,229,0.08);
                    }
                    .icon-wrap {
                        width: 42px; height: 42px; border-radius: 11px;
                        background: var(--accent-bg);
                        display: flex; align-items: center; justify-content: center;
                        margin-bottom: 16px;
                    }
                    h1 { font-size: 20px; font-weight: 700; color: var(--ink); letter-spacing: -0.4px; margin-bottom: 6px; }
                    .subtitle { font-size: 14px; color: var(--ink-mid); line-height: 1.55; }
                    .tags { display: flex; gap: 8px; margin: 18px 0 26px; flex-wrap: wrap; }
                    .tag { font-size: 11px; padding: 4px 10px; border-radius: 20px; border: 1px solid var(--border); color: var(--ink-soft); background: #fafafa; }
                    .form-group { margin-bottom: 20px; }
                    label { display: block; font-size: 11px; font-weight: 700; color: var(--ink-soft); text-transform: uppercase; letter-spacing: 0.07em; margin-bottom: 8px; }
                    input[type="text"] {
                        width: 100%; padding: 11px 14px;
                        border: 1.5px solid var(--border); border-radius: var(--radius);
                        font-size: 14px; color: var(--ink); background: #fff;
                        transition: border-color 0.15s; outline: none;
                    }
                    input[type="text"]:focus { border-color: var(--accent); }
                    input[type="text"]::placeholder { color: var(--ink-ghost); }
                    .hint { font-size: 12px; color: var(--ink-ghost); margin-top: 6px; }
                    .file-zone {
                        border: 1.5px dashed var(--border); border-radius: var(--radius);
                        padding: 28px 16px; text-align: center; cursor: pointer;
                        transition: border-color 0.15s, background 0.15s;
                        background: #fafafa; position: relative;
                    }
                    .file-zone:hover { border-color: var(--accent); background: var(--accent-bg); }
                    .file-zone.selected { border-style: solid; border-color: var(--accent); background: var(--accent-bg); }
                    .file-zone input[type="file"] { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
                    .file-icon-wrap {
                        width: 36px; height: 36px; margin: 0 auto 10px;
                        background: var(--border); border-radius: 9px;
                        display: flex; align-items: center; justify-content: center;
                        transition: background 0.15s;
                    }
                    .file-zone.selected .file-icon-wrap { background: var(--accent); }
                    .file-zone.selected .file-icon-wrap svg { stroke: #fff; }
                    .file-name { font-size: 13px; font-weight: 600; color: var(--ink); margin-bottom: 3px; }
                    .file-meta { font-size: 12px; color: var(--ink-soft); }
                    .btn-primary {
                        width: 100%; padding: 12px; background: var(--accent); color: #fff;
                        border: none; border-radius: var(--radius);
                        font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 4px;
                        display: flex; align-items: center; justify-content: center; gap: 8px;
                        transition: opacity 0.15s, transform 0.1s;
                    }
                    .btn-primary:hover { opacity: 0.88; }
                    .btn-primary:active { transform: scale(0.99); }
                    .btn-primary:disabled { opacity: 0.3; cursor: not-allowed; transform: none; }
                    .spinner {
                        width: 15px; height: 15px;
                        border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff;
                        border-radius: 50%; animation: spin 0.7s linear infinite; display: none;
                    }
                    @keyframes spin { to { transform: rotate(360deg); } }
                    .progress-wrap { margin-top: 16px; display: none; }
                    .progress-track { height: 3px; background: var(--border); border-radius: 3px; overflow: hidden; }
                    .progress-bar { height: 100%; width: 0%; background: var(--accent); border-radius: 3px; transition: width 0.35s ease; }
                    .progress-label { font-size: 12px; color: var(--ink-soft); margin-top: 8px; text-align: center; }
                    .success-panel {
                        display: none; border: 1.5px solid #a7f3d0; background: var(--success-bg);
                        border-radius: var(--radius); padding: 24px; text-align: center; margin-top: 16px;
                    }
                    .success-icon {
                        width: 44px; height: 44px; background: var(--success); border-radius: 50%;
                        display: flex; align-items: center; justify-content: center; margin: 0 auto 14px;
                    }
                    .success-title { font-size: 15px; font-weight: 700; color: #065f46; margin-bottom: 4px; }
                    .success-sub { font-size: 13px; color: #047857; margin-bottom: 18px; }
                    .btn-reset {
                        background: var(--success); color: #fff; border: none; border-radius: 8px;
                        padding: 9px 20px; font-size: 13px; font-weight: 600; cursor: pointer;
                        transition: opacity 0.15s;
                    }
                    .btn-reset:hover { opacity: 0.85; }
                    .error-panel {
                        display: none; border: 1.5px solid #fca5a5; background: var(--error-bg);
                        border-radius: var(--radius); padding: 16px; margin-top: 16px;
                        font-size: 13px; color: var(--error); text-align: center;
                    }
                    .divider { height: 1px; background: var(--border); margin: 24px 0 16px; }
                    .footer-note { font-size: 11px; color: var(--ink-ghost); text-align: center; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon-wrap">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#4f46e5" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                            <line x1="16" y1="13" x2="8" y2="13"/>
                            <line x1="16" y1="17" x2="8" y2="17"/>
                        </svg>
                    </div>
                    <h1>PDF Watermark Service</h1>
                    <p class="subtitle">Enter your name and upload a PDF. Your name will be stamped diagonally across every page.</p>

                    <div class="tags">
                        <span class="tag">Under 10 MB — opens in new tab</span>
                        <span class="tag">Over 10 MB — S3 link returned</span>
                    </div>

                    <form id="wForm">
                        <div class="form-group">
                            <label>Your name</label>
                            <input type="text" name="name" id="nameInput"
                                   placeholder="Watermark name"
                                   maxlength="50" autocomplete="off" oninput="checkReady()"/>
                            <p class="hint">This text appears as a watermark on every page.</p>
                        </div>
                        <div class="form-group">
                            <label>PDF file</label>
                            <div class="file-zone" id="fileZone">
                                <input type="file" name="pdf" id="pdfInput"
                                       accept=".pdf,application/pdf" onchange="fileSelected(this)"/>
                                <div class="file-icon-wrap">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                        <polyline points="16 16 12 12 8 16"/>
                                        <line x1="12" y1="12" x2="12" y2="21"/>
                                        <path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"/>
                                    </svg>
                                </div>
                                <p class="file-name" id="fileName">Click to select a PDF</p>
                                <p class="file-meta" id="fileMeta">Max 100 MB · PDF only</p>
                            </div>
                        </div>

                        <button type="submit" class="btn-primary" id="submitBtn" disabled>
                            <div class="spinner" id="spinner"></div>
                            <span id="btnText">Add Watermark</span>
                        </button>

                        <div class="progress-wrap" id="progressWrap">
                            <div class="progress-track"><div class="progress-bar" id="progressBar"></div></div>
                            <p class="progress-label" id="progressLabel">Starting...</p>
                        </div>
                    </form>

                    <div class="success-panel" id="successPanel">
                        <div class="success-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="20 6 9 17 4 12"/>
                            </svg>
                        </div>
                        <p class="success-title">Watermark added successfully</p>
                        <p class="success-sub" id="successSub">Your PDF is ready.</p>
                        <button class="btn-reset" onclick="resetForm()">Watermark another PDF</button>
                    </div>

                    <div class="error-panel" id="errorPanel"></div>

                    <div class="divider"></div>
                    <p class="footer-note">Java 21 · Spring Boot · Apache PDFBox · MiniStack · Docker</p>
                </div>

                <script>
                    var progressTimer = null;
                    var progressVal = 0;

                    function checkReady() {
                        var name = document.getElementById('nameInput').value.trim();
                        var file = document.getElementById('pdfInput').files[0];
                        document.getElementById('submitBtn').disabled = !(name && file);
                    }

                    function fileSelected(input) {
                        var file = input.files[0];
                        if (!file) return;
                        document.getElementById('fileZone').classList.add('selected');
                        document.getElementById('fileName').textContent = file.name;
                        var mb = (file.size / 1024 / 1024).toFixed(1);
                        document.getElementById('fileMeta').textContent =
                            mb + ' MB · ' + (file.size > 10485760 ? 'S3 link will be returned' : 'Will open in new tab');
                        checkReady();
                    }

                    document.getElementById('wForm').addEventListener('submit', function(e) {
                        e.preventDefault();

                        var name = document.getElementById('nameInput').value.trim();
                        var fileInput = document.getElementById('pdfInput');
                        var file = fileInput.files[0];
                        if (!name || !file) return;

                        // UI: start processing
                        clearInterval(progressTimer);
                        progressVal = 0;
                        var btn = document.getElementById('submitBtn');
                        btn.disabled = true;
                        document.getElementById('spinner').style.display = 'block';
                        document.getElementById('btnText').textContent = 'Processing...';
                        document.getElementById('progressWrap').style.display = 'block';
                        document.getElementById('progressBar').style.width = '0%';
                        document.getElementById('errorPanel').style.display = 'none';
                        document.getElementById('successPanel').style.display = 'none';

                        var msgs = ['Validating PDF...', 'Adding watermark to pages...', 'Almost done...'];
                        var msgIdx = 0;
                        progressTimer = setInterval(function() {
                            progressVal += Math.random() * 12 + 4;
                            if (progressVal > 88) progressVal = 88;
                            document.getElementById('progressBar').style.width = progressVal + '%';
                            if (msgIdx < msgs.length) {
                                document.getElementById('progressLabel').textContent = msgs[msgIdx++];
                            }
                        }, 700);

                        // Submit via fetch
                        var formData = new FormData();
                        formData.append('name', name);
                        formData.append('pdf', file);

                        fetch('/watermark', { method: 'POST', body: formData })
                            .then(function(response) {
                                clearInterval(progressTimer);
                                document.getElementById('progressBar').style.width = '100%';
                                document.getElementById('progressLabel').textContent = 'Done!';

                                var contentType = response.headers.get('content-type') || '';

                                if (contentType.includes('application/pdf')) {
                                    // Small file — open as blob in new tab
                                    return response.blob().then(function(blob) {
                                        var url = URL.createObjectURL(blob);
                                        window.open(url, '_blank');
                                        showSuccess('Your watermarked PDF opened in a new tab.');
                                    });
                                } else {
                                    // Large file — parse JSON and open download URL
                                    return response.json().then(function(data) {
                                        if (data.downloadUrl) {
                                            window.open(data.downloadUrl, '_blank');
                                            showSuccess('Large file processed. Download link opened in a new tab.');
                                        } else if (data.error) {
                                            showError(data.error);
                                        } else {
                                            showSuccess('Done! Check the new tab.');
                                        }
                                    });
                                }
                            })
                            .catch(function() {
                                clearInterval(progressTimer);
                                showError('Something went wrong. Is Docker running?');
                            });
                    });

                    function showSuccess(msg) {
                        document.getElementById('submitBtn').style.display = 'none';
                        document.getElementById('progressWrap').style.display = 'none';
                        document.getElementById('successSub').textContent = msg;
                        document.getElementById('successPanel').style.display = 'block';
                    }

                    function showError(msg) {
                        document.getElementById('progressWrap').style.display = 'none';
                        document.getElementById('submitBtn').disabled = false;
                        document.getElementById('spinner').style.display = 'none';
                        document.getElementById('btnText').textContent = 'Add Watermark';
                        var panel = document.getElementById('errorPanel');
                        panel.textContent = msg;
                        panel.style.display = 'block';
                    }

                    function resetForm() {
                        document.getElementById('wForm').reset();
                        document.getElementById('fileZone').classList.remove('selected');
                        document.getElementById('fileName').textContent = 'Click to select a PDF';
                        document.getElementById('fileMeta').textContent = 'Max 100 MB · PDF only';
                        var btn = document.getElementById('submitBtn');
                        btn.style.display = 'flex'; btn.disabled = true;
                        document.getElementById('spinner').style.display = 'none';
                        document.getElementById('btnText').textContent = 'Add Watermark';
                        document.getElementById('progressWrap').style.display = 'none';
                        document.getElementById('progressBar').style.width = '0%';
                        document.getElementById('progressLabel').textContent = 'Starting...';
                        document.getElementById('successPanel').style.display = 'none';
                        document.getElementById('errorPanel').style.display = 'none';
                        clearInterval(progressTimer);
                        progressVal = 0;
                    }
                </script>
            </body>
            </html>
            """;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public String health() {
        return "{\"status\":\"ok\",\"service\":\"pdf-watermark-service\",\"stack\":\"Java 21 + Spring Boot + Apache PDFBox + MiniStack\"}";
    }
}
