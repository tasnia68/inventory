package com.inventory.system.service;

import com.inventory.system.common.entity.ProductImage;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ProductImageDto;
import com.inventory.system.repository.ProductImageRepository;
import com.inventory.system.repository.ProductTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductImageDto uploadImage(UUID templateId, MultipartFile file, Boolean isMain) {
        ProductTemplate template = productTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Template", "id", templateId));

        String folder = "product-images/" + templateId;
        String filename = fileStorageService.uploadFile(file, folder); // Returns full object path/key

        // If isMain is true, unset other main images for this template
        if (Boolean.TRUE.equals(isMain)) {
            unsetMainImages(templateId);
        }

        ProductImage image = new ProductImage();
        image.setProductTemplate(template);
        image.setFilename(file.getOriginalFilename());
        image.setUrl(filename); // Storing the MinIO object key as URL for now.
                                // Alternatively, store full signed URL if needed, but key is better for
                                // permanent storage.
        image.setIsMain(isMain != null ? isMain : false);

        image = productImageRepository.save(image);
        return mapToDto(image);
    }

    @Override
    @Transactional
    public void deleteImage(UUID imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Image", "id", imageId));

        fileStorageService.deleteFile(image.getUrl()); // Allow this to fail silently? Or throw?
                                                       // Assuming deleteFile logs error and shouldn't block DB delete
                                                       // unless critical.
        productImageRepository.delete(image);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageDto> getImages(UUID templateId) {
        if (!productTemplateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Product Template", "id", templateId);
        }
        return productImageRepository.findByProductTemplateId(templateId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductImageDto setMainImage(UUID imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Image", "id", imageId));

        unsetMainImages(image.getProductTemplate().getId());
        image.setIsMain(true);
        return mapToDto(productImageRepository.save(image));
    }

    private void unsetMainImages(UUID templateId) {
        List<ProductImage> images = productImageRepository.findByProductTemplateId(templateId);
        for (ProductImage img : images) {
            if (img.getIsMain()) {
                img.setIsMain(false);
                productImageRepository.save(img);
            }
        }
    }

    private ProductImageDto mapToDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setId(image.getId());
        dto.setUrl(image.getUrl());
        dto.setFilename(image.getFilename());
        dto.setIsMain(image.getIsMain());
        dto.setProductTemplateId(image.getProductTemplate().getId());
        return dto;
    }
}
