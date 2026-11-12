package com.fashion.platform.infra.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService implements ObjectStorageService {

    private final MinioClient minioClient;

    public MinioStorageService(
            @Value("${fashion.storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${fashion.storage.minio.access-key:minioadmin}") String accessKey,
            @Value("${fashion.storage.minio.secret-key:minioadmin_password}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to verify/create MinIO bucket {}", bucketName, e);
            throw new RuntimeException("MinIO bucket operation failed", e);
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        ensureBucketExists(bucketName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded object to MinIO: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to upload object {}/{} to MinIO", bucketName, objectName, e);
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to retrieve object {}/{} from MinIO", bucketName, objectName, e);
            throw new RuntimeException("Retrieve failed", e);
        }
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String bucketName, String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}

