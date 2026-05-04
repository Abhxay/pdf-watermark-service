package com.geminid.watermark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock private S3Client mockS3Client;
    @Mock private S3Presigner mockPresigner;

    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new StorageService();
        setField("s3Client", mockS3Client);
        setField("presigner", mockPresigner);
        setField("bucketName", "pdf-watermark-bucket");
        setField("endpoint", "http://localhost:4566");
        setField("region", "us-east-1");
        setField("accessKeyId", "test");
        setField("secretKey", "test");
    }

    private void setField(String name, Object value) throws Exception {
        var field = StorageService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(storageService, value);
    }

    @Test
    @DisplayName("upload: calls putObject with correct key and content type")
    void upload_validData_callsPutObject() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storageService.upload("processed/abc/test.pdf", "fake".getBytes(), "application/pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo("processed/abc/test.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().bucket()).isEqualTo("pdf-watermark-bucket");
    }

    @Test
    @DisplayName("upload: called exactly once")
    void upload_calledOnce() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storageService.upload("key/file.pdf", new byte[]{1, 2, 3}, "application/pdf");

        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("objectExists: returns true when headObject succeeds")
    void objectExists_keyPresent_returnsTrue() {
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertThat(storageService.objectExists("processed/abc/test.pdf")).isTrue();
    }

    @Test
    @DisplayName("objectExists: returns false when key is missing")
    void objectExists_keyMissing_returnsFalse() {
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        assertThat(storageService.objectExists("processed/missing.pdf")).isFalse();
    }

    @Test
    @DisplayName("ensureBucketExists: does not createBucket when bucket already exists")
    void ensureBucketExists_alreadyExists_noCreate() {
        when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());

        storageService.ensureBucketExists();

        verify(mockS3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("ensureBucketExists: calls createBucket when bucket missing")
    void ensureBucketExists_bucketMissing_createsBucket() {
        when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("no bucket").build());
        when(mockS3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        storageService.ensureBucketExists();

        verify(mockS3Client).createBucket(any(CreateBucketRequest.class));
    }
}