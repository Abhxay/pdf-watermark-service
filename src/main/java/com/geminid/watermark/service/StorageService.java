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
                .pathStyleAccessEnabled(true)
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
        configureLifecyclePolicy();
    }

    public void upload(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.info("Uploaded to s3://{}/{}", bucketName, key);
    }

    public String getSignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(r -> r.bucket(bucketName).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' already exists.", bucketName);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' created.", bucketName);
        } catch (Exception e) {
            log.warn("Could not check bucket at startup: {}", e.getMessage());
        }
    }

    private void configureLifecyclePolicy() {
        LifecycleRule uploadsCleanup = LifecycleRule.builder()
                .id("auto-delete-uploads-1d")
                .filter(LifecycleRuleFilter.builder().prefix("uploads/").build())
                .expiration(LifecycleExpiration.builder().days(1).build())
                .status(ExpirationStatus.ENABLED)
                .build();

        LifecycleRule processedCleanup = LifecycleRule.builder()
                .id("auto-delete-processed-7d")
                .filter(LifecycleRuleFilter.builder().prefix("processed/").build())
                .expiration(LifecycleExpiration.builder().days(7).build())
                .status(ExpirationStatus.ENABLED)
                .build();

        try {
            s3Client.putBucketLifecycleConfiguration(
                    PutBucketLifecycleConfigurationRequest.builder()
                            .bucket(bucketName)
                            .lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                                    .rules(uploadsCleanup, processedCleanup)
                                    .build())
                            .build());
            log.info("Lifecycle policy configured: uploads=1d, processed=7d");
        } catch (Exception e) {
            log.warn("Could not configure lifecycle policy: {}", e.getMessage());
        }
    }
}