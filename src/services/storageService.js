/**
 * Storage Service
 * Repository pattern — all S3 (LocalStack) interactions go through here.
 * If you swap S3 for GCS or Azure Blob tomorrow, only this file changes.
 */

const { S3Client, PutObjectCommand, GetObjectCommand, CreateBucketCommand, HeadBucketCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');

const BUCKET_NAME = process.env.S3_BUCKET || 'pdf-watermark-bucket';

// ── S3 client — points at LocalStack when running locally ────────────────────
const s3Client = new S3Client({
  region: process.env.AWS_REGION || 'us-east-1',
  endpoint: process.env.AWS_ENDPOINT || 'http://localstack:4566',
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID || 'test',
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || 'test',
  },
  forcePathStyle: true, // Required for LocalStack — uses path-style URLs
});

/**
 * Ensures the S3 bucket exists. Creates it if it doesn't.
 * Called once on startup.
 */
const ensureBucketExists = async () => {
  try {
    await s3Client.send(new HeadBucketCommand({ Bucket: BUCKET_NAME }));
    console.log(`[storageService] Bucket "${BUCKET_NAME}" already exists.`);
  } catch (err) {
    if (err.name === 'NotFound' || err.$metadata?.httpStatusCode === 404) {
      await s3Client.send(new CreateBucketCommand({ Bucket: BUCKET_NAME }));
      console.log(`[storageService] Bucket "${BUCKET_NAME}" created.`);
    } else {
      console.warn(`[storageService] Could not check bucket (LocalStack may still be starting): ${err.message}`);
    }
  }
};

/**
 * Uploads a file buffer to S3.
 *
 * @param {string} key         - S3 object key (path inside the bucket)
 * @param {Buffer} body        - File content
 * @param {string} contentType - MIME type
 */
const upload = async (key, body, contentType) => {
  const command = new PutObjectCommand({
    Bucket: BUCKET_NAME,
    Key: key,
    Body: body,
    ContentType: contentType,
    Metadata: {
      uploadedAt: new Date().toISOString(),
    },
  });

  const result = await s3Client.send(command);
  console.log(`[storageService] Uploaded → s3://${BUCKET_NAME}/${key}`);
  return result;
};

/**
 * Generates a pre-signed URL so clients can download directly from S3.
 *
 * @param {string} key       - S3 object key
 * @param {number} expiresIn - Seconds until URL expires (default 1 hour)
 */
const getSignedDownloadUrl = async (key, expiresIn = 3600) => {
  const command = new GetObjectCommand({
    Bucket: BUCKET_NAME,
    Key: key,
  });

  return getSignedUrl(s3Client, command, { expiresIn });
};

// Ensure the bucket exists when the service module loads
ensureBucketExists().catch(() => {
  // Non-fatal at startup — LocalStack might not be ready yet
  console.warn('[storageService] Bucket init deferred — will retry on first upload.');
});

module.exports = {
  upload,
  getSignedUrl: getSignedDownloadUrl,
  ensureBucketExists,
};
