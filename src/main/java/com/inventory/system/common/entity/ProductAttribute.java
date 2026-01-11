package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
public class ProductAttribute extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType type;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(name = "validation_regex")
    private String validationRegex;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON or comma-separated values for DROPDOWN/MULTI_SELECT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ProductTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private AttributeGroup group;

    public enum AttributeType {
        TEXT,
        NUMBER,
        DATE,
        DROPDOWN,
        MULTI_SELECT
    }
}
