package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_variant_versions")
@Getter
@Setter
public class ProductVariantVersion extends BaseEntity {

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "snapshot", columnDefinition = "TEXT", nullable = false)
    private String snapshot;
}