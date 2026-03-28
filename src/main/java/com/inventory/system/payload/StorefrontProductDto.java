package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontProductDto {
    private String productTemplateId;
    private String productSlug;
    private String productVariantId;
    private String slug;
    private String sku;
    private String name;
    private String category;
    private String collectionSlug;
    private String collectionTitle;
    private String parentCollectionSlug;
    private String parentCollectionTitle;
    private String categoryPath;
    private List<String> collectionTrailSlugs = new ArrayList<>();
    private List<String> collectionTrailTitles = new ArrayList<>();
    private String variantLabel;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;
    private String summary;
    private String imageUrl;
    private List<String> imageUrls = new ArrayList<>();
    private String seoTitle;
    private String seoDescription;
    private String uomName;
    private String badge;
    private Boolean featured;
    private BigDecimal availableToPromise;
    private String availabilityLabel;
    private List<StorefrontVariantOptionDto> variants = new ArrayList<>();
}
