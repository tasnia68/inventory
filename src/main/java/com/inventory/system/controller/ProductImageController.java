package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ProductImageDto;
import com.inventory.system.service.FileStorageService;
import com.inventory.system.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/product-templates/{templateId}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductImageDto>> uploadImage(
            @PathVariable UUID templateId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isMain", required = false) Boolean isMain) {

        ProductImageDto image = productImageService.uploadImage(templateId, file, isMain);
        return new ResponseEntity<>(new ApiResponse<>(true, "Image uploaded successfully", image), HttpStatus.CREATED);
    }

    @GetMapping("/product-templates/{templateId}/images")
    public ResponseEntity<ApiResponse<List<ProductImageDto>>> getImages(@PathVariable UUID templateId) {
        List<ProductImageDto> images = productImageService.getImages(templateId);
        return new ResponseEntity<>(new ApiResponse<>(true, "Images retrieved successfully", images), HttpStatus.OK);
    }

    @GetMapping("/product-images/{id}/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<InputStreamResource> getImageFile(@PathVariable UUID id) {
        ProductImageDto image = productImageService.getImage(id);
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(image.getUrl()));

        MediaType mediaType = MediaTypeFactory.getMediaType(image.getFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/product-images/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID id) {
        productImageService.deleteImage(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Image deleted successfully", null), HttpStatus.NO_CONTENT);
    }

    @PutMapping("/product-images/{id}/main")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ProductImageDto>> setMainImage(@PathVariable UUID id) {
        ProductImageDto image = productImageService.setMainImage(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Main image set successfully", image), HttpStatus.OK);
    }
}
