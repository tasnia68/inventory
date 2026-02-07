package com.inventory.system.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String folder);

    void deleteFile(String filename);

    InputStream getFile(String filename);
}
