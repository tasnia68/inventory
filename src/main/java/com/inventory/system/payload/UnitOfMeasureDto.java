package com.inventory.system.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.system.common.entity.UnitOfMeasure.UomCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitOfMeasureDto {
    private UUID id;
    private String name;
    private String code;
    private UomCategory category;
    private Boolean isBase;
    private BigDecimal conversionFactor;
}
