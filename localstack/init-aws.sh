#!/bin/bash
# Runs automatically when LocalStack is ready.
# Creates the S3 bucket so the app doesn't need to create it on first request.
echo "Creating S3 bucket: pdf-watermark-bucket"
awslocal s3 mb s3://pdf-watermark-bucket
echo "Bucket ready."
