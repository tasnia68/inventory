package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(unique = true)
    private String barcode;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "compare_at_price")
    private BigDecimal compareAtPrice;

    /**
     * Per-variant base cost (the unit cost recorded against stock movements
     * defaults to this when none is supplied). ProductCost.averageCost stays
     * as the running per-warehouse weighted average — `cost` here is the seed.
     */
    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "storefront_badge")
    private String storefrontBadge;

    @Column(name = "storefront_featured", nullable = false)
    private Boolean storefrontFeatured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProductTemplate template;

    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();
}
