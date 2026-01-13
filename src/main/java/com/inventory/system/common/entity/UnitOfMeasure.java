package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "units_of_measure")
@Getter
@Setter
public class UnitOfMeasure extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UomCategory category;

    @Column(name = "is_base", nullable = false)
    private Boolean isBase = false;

    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 6)
    private BigDecimal conversionFactor;

    public enum UomCategory {
        WEIGHT,
        VOLUME,
        LENGTH,
        QUANTITY,
        TIME
    }
}
