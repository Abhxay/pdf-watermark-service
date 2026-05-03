#!/bin/bash
# This script runs automatically when LocalStack is ready.
# It creates the S3 bucket so the app doesn't have to worry about it.

echo "🪣 Creating S3 bucket: pdf-watermark-bucket"
awslocal s3 mb s3://pdf-watermark-bucket
echo "✅ Bucket created successfully"
