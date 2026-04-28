package com.inventory.system.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_templates")
@Getter
@Setter
public class ProductTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_batch_tracked")
    private Boolean isBatchTracked = false;

    @Column(name = "is_serial_tracked")
    private Boolean isSerialTracked = false;

    @Column(name = "published_to_storefront", nullable = false)
    private Boolean publishedToStorefront = true;

    @Column(name = "storefront_slug")
    private String storefrontSlug;

    @Column(name = "storefront_title")
    private String storefrontTitle;

    @Column(name = "storefront_description", columnDefinition = "TEXT")
    private String storefrontDescription;

    @Column(name = "storefront_sort_order")
    private Integer storefrontSortOrder;

    @Column(name = "storefront_seo_title")
    private String storefrontSeoTitle;

    @Column(name = "storefront_seo_description", columnDefinition = "TEXT")
    private String storefrontSeoDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UnitOfMeasure uom;

    @OneToMany(mappedBy = "productTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}
