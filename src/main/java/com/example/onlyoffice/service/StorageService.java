package com.example.onlyoffice.service;

import com.example.onlyoffice.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.List;

/**
 * Stores document files in an S3-compatible bucket (MinIO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3;
    private final StorageProperties props;

    @PostConstruct
    void ensureBucket() {
        String bucket = props.getBucket();
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Storage bucket '{}' is available", bucket);
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' not found, creating it", bucket);
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    public void upload(String key, InputStream data, long contentLength, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(data, contentLength));
        log.debug("Uploaded '{}' ({} bytes)", key, contentLength);
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        return s3.getObject(GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    public List<String> list() {
        return s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(props.getBucket())
                        .build())
                .contents().stream()
                .map(S3Object::key)
                .toList();
    }

    public boolean exists(String key) {
        return list().contains(key);
    }
}
