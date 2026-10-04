package org.example.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.example.config.RustfsStorageProperties;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Service
public class RustfsObjectStorageService implements ObjectStorageService {
    private static final Logger log = LoggerFactory.getLogger(RustfsObjectStorageService.class);
    private final MinioClient client;
    private final RustfsStorageProperties properties;

    public RustfsObjectStorageService(MinioClient client, RustfsStorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            if (!isAlreadyExists(e)) {
                throw new IllegalStateException("无法初始化 RustFS 存储桶", e);
            }
        }
    }

    @Override
    public void putObject(String key, InputStream content, long size, String contentType) throws IOException {
        try {
            client.putObject(PutObjectArgs.builder().bucket(properties.getBucket()).object(key)
                    .stream(content, size, -1)
                    .contentType(contentType == null || contentType.trim().isEmpty()
                            ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            log.error("RustFS object write failed for key {}", key, e);
            throw new IOException("无法写入 RustFS 对象", e);
        }
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(properties.getBucket()).object(key).build());
        } catch (Exception e) {
            if (isNotFound(e)) {
                log.info("RustFS object was not found for key {}", key);
                throw new ObjectStorageNotFoundException("对象不存在", e);
            }
            log.error("RustFS object read failed for key {}", key, e);
            throw new IOException("无法读取 RustFS 对象", e);
        }
    }

    @Override
    public void deleteObject(String key) throws IOException {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(properties.getBucket()).object(key).build());
        } catch (Exception e) {
            log.error("RustFS object deletion failed for key {}", key, e);
            throw new IOException("无法删除 RustFS 对象", e);
        }
    }

    private boolean isAlreadyExists(Exception exception) {
        return exception instanceof ErrorResponseException
                && ("BucketAlreadyExists".equals(((ErrorResponseException) exception).errorResponse().code())
                || "BucketAlreadyOwnedByYou".equals(((ErrorResponseException) exception).errorResponse().code()));
    }

    private boolean isNotFound(Exception exception) {
        return exception instanceof ErrorResponseException
                && ("NoSuchKey".equals(((ErrorResponseException) exception).errorResponse().code())
                || "NoSuchObject".equals(((ErrorResponseException) exception).errorResponse().code())
                || "NoSuchBucket".equals(((ErrorResponseException) exception).errorResponse().code()));
    }
}
