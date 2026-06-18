package com.inventory.system.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String folder);

    /**
     * Store raw bytes (e.g. an image downloaded from an external CDN) and return the
     * object key, matching the key format produced by {@link #uploadFile}. Callers can
     * persist the returned key in the same field used for uploaded files so the rest of
     * the stack (storefront, admin, /file streaming) treats it identically.
     */
    String uploadBytes(byte[] content, String originalFilename, String contentType, String folder);

    void deleteFile(String filename);

    InputStream getFile(String filename);
}
