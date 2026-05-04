package com.geminid.watermark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;

/**
 * Storage Service — Repository Pattern.
 *
 * All S3 / MiniStack  operations go through here.
 * If you swap MiniStack  for real AWS tomorrow, only this file changes.
 *
 * MiniStack  is used here as the local alternative to real AWS S3.
 * It runs inside Docker and listens on port 4566.
 * Your code talks to it exactly like real AWS — same SDK, same API.
 */
@Slf4j
@Service
public class StorageService {

    @Value("${aws.endpoint}")
    private String endpoint;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretKey));

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Required for MiniStack
                .build();

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        ensureBucketExists();
    }

    /**
     * Uploads a file to S3 (MiniStack ).
     *
     * @param key         S3 object key — the "path" inside the bucket
     * @param data        File bytes
     * @param contentType MIME type
     */
    public void upload(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.info("Uploaded to s3://{}/{}", bucketName, key);
    }

    /**
     * Generates a pre-signed URL for downloading a file from S3.
     * The URL is valid for 1 hour — after that it stops working.
     *
     * Pre-signed URL = a temporary access ticket.
     * The client can download directly from S3 without going through our server.
     *
     * @param key S3 object key
     * @return    Pre-signed HTTPS URL
     */
    public String getSignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(r -> r.bucket(bucketName).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Creates the S3 bucket if it doesn't already exist.
     * Called once when the service starts up.
     */
    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' already exists.", bucketName);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' created.", bucketName);
        } catch (Exception e) {
            log.warn("Could not check bucket at startup (MiniStack  may still be starting): {}", e.getMessage());
        }
    }
}
