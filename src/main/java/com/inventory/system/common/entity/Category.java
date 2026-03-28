package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "published_to_storefront", nullable = false)
    private Boolean publishedToStorefront = false;

    @Column(name = "storefront_slug")
    private String storefrontSlug;

    @Column(name = "storefront_title")
    private String storefrontTitle;

    @Column(name = "storefront_description", columnDefinition = "TEXT")
    private String storefrontDescription;

    @Column(name = "storefront_sort_order")
    private Integer storefrontSortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "category_attributes",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    private Set<ProductAttribute> attributes = new HashSet<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<ProductTemplate> products = new ArrayList<>();
}
