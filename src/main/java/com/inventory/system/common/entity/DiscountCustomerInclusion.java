package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "discount_customer_inclusions")
@Getter
@Setter
public class DiscountCustomerInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiscountCustomerInclusionScope scope;

    /** CUSTOMER -> customer UUID as string; CUSTOMER_CATEGORY -> CustomerCategory enum name. */
    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountInclusionMode mode = DiscountInclusionMode.INCLUDE;
}
