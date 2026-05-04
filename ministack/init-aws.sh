#!/bin/bash
# Runs automatically when MiniStack is ready.
# Creates the S3 bucket so the app doesn't need to create it on first request.
echo "Creating S3 bucket: pdf-watermark-bucket"
aws --endpoint-url=http://localhost:4566 \
    --region us-east-1 \
    --no-sign-request \
    s3 mb s3://pdf-watermark-bucket
echo "Bucket ready."