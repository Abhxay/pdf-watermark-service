/**
 * Lambda 1 — Form Handler
 * GET /
 * Serves the upload form where the user enters their name and picks a PDF.
 */
const formLambdaHandler = (req, res) => {
  const html = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
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
        .subtitle { color: #6b7280; font-size: 14px; margin-bottom: 28px; line-height: 1.5; }
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
        label { display: block; font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 6px; }
        input[type="text"], input[type="file"] {
          width: 100%;
          padding: 10px 14px;
          border: 1.5px solid #e5e7eb;
          border-radius: 8px;
          font-size: 14px;
          color: #111827;
          transition: border-color 0.2s;
        }
        input[type="text"]:focus {
          outline: none;
          border-color: #3b82f6;
          box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
        }
        input[type="file"] { padding: 8px; cursor: pointer; }
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
          transition: opacity 0.2s;
        }
        button:hover { opacity: 0.9; }
        button:disabled { opacity: 0.6; cursor: not-allowed; }
        #status { margin-top: 16px; font-size: 13px; text-align: center; color: #6b7280; }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>🔒 PDF Watermark Service</h1>
        <p class="subtitle">Enter your name and upload a PDF. Your name will be stamped diagonally across every page as a watermark.</p>

        <div class="info-box">
          📁 <strong>Files under 10 MB</strong> — processed instantly, returned directly.<br/>
          ☁️ <strong>Files over 10 MB</strong> — stored in S3, processed async, download link returned.
        </div>

        <form id="watermarkForm" action="/watermark" method="POST" enctype="multipart/form-data">
          <div class="form-group">
            <label for="name">Your Name <span style="color:#ef4444">*</span></label>
            <input
              type="text"
              id="name"
              name="name"
              required
              placeholder="e.g. Abhay Thakur"
              maxlength="50"
            />
            <p class="hint">This text will appear as a watermark on every page.</p>
          </div>

          <div class="form-group">
            <label for="pdf">PDF File <span style="color:#ef4444">*</span></label>
            <input
              type="file"
              id="pdf"
              name="pdf"
              accept=".pdf,application/pdf"
              required
            />
            <p class="hint">Max 100 MB. Only PDF files accepted.</p>
          </div>

          <button type="submit" id="submitBtn">Add Watermark →</button>
        </form>

        <p id="status"></p>
      </div>

      <script>
        document.getElementById('watermarkForm').addEventListener('submit', function(e) {
          const btn = document.getElementById('submitBtn');
          const status = document.getElementById('status');
          const file = document.getElementById('pdf').files[0];
          const name = document.getElementById('name').value.trim();

          if (!name) {
            e.preventDefault();
            status.textContent = '⚠️ Please enter your name.';
            status.style.color = '#ef4444';
            return;
          }

          if (file && file.type !== 'application/pdf') {
            e.preventDefault();
            status.textContent = '⚠️ Please select a valid PDF file.';
            status.style.color = '#ef4444';
            return;
          }

          btn.disabled = true;
          btn.textContent = 'Processing...';
          status.textContent = file && file.size > 10 * 1024 * 1024
            ? '⏳ Large file detected — uploading to S3...'
            : '⏳ Adding watermark...';
          status.style.color = '#3b82f6';
        });
      </script>
    </body>
    </html>
  `;

  return res.status(200).send(html);
};

module.exports = { formLambdaHandler };
