package com.fashion.platform.infra.storage;

import java.io.InputStream;

public interface ObjectStorageService {
    void putObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);
    InputStream getObject(String bucketName, String objectName);
    boolean doesObjectExist(String bucketName, String objectName);
    String generatePresignedUrl(String bucketName, String objectName, int expiryMinutes);
}

