package com.inventory.system.service;

import com.inventory.system.payload.ProductImageDto;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface ProductImageService {
    ProductImageDto uploadImage(UUID templateId, MultipartFile file, Boolean isMain);

    void deleteImage(UUID imageId);

    List<ProductImageDto> getImages(UUID templateId);

    ProductImageDto getImage(UUID imageId);

    ProductImageDto setMainImage(UUID imageId);
}
