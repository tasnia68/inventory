package com.inventory.system.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "storefront_pages")
@Getter
@Setter
public class StorefrontPage extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean published = false;
}
