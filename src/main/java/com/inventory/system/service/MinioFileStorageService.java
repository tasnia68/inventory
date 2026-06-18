package com.inventory.system.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String filename = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // Return relative path or full URL depending on need.
            // Usually internal storage tracks key, and we generate URL via API or
            // Presigned.
            // For now, returning filename (key).
            return filename;
        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Could not upload file " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public String uploadBytes(byte[] content, String originalFilename, String contentType, String folder) {
        String safeName = (originalFilename == null || originalFilename.isBlank()) ? "image" : originalFilename;
        String filename = folder + "/" + UUID.randomUUID() + "-" + safeName;
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            try (InputStream stream = new ByteArrayInputStream(content)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .stream(stream, content.length, -1)
                                .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                                .build());
            }
            return filename;
        } catch (Exception e) {
            log.error("Error uploading bytes to MinIO", e);
            throw new RuntimeException("Could not upload bytes " + safeName, e);
        }
    }

    @Override
    public void deleteFile(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());
        } catch (Exception e) {
            log.error("Error deleting file from MinIO", e);
            throw new RuntimeException("Could not delete file " + filename, e);
        }
    }

    @Override
    public InputStream getFile(String filename) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());
        } catch (Exception e) {
            log.error("Error getting file from MinIO", e);
            throw new RuntimeException("Could not get file " + filename, e);
        }
    }
}
