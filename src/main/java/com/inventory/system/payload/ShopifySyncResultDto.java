package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopifySyncResultDto {
    private boolean success;
    private String message;
    private int productsSeen;
    private int productsCreated;
    private int productsUpdated;
    private int variantsCreated;
    private int variantsUpdated;
    private int categoriesCreated;
    private int imagesImported;
    private int ordersSeen;
    private int ordersImported;
    private int ordersDuplicate;
    private int locationsSeen;
    private int locationsCreated;
    private int locationsMatched;
    private int stockLevelsSeen;
    private int stockLevelsApplied;
    private int productsPushed;
    private int variantsPushed;
    private int inventoryAdjustmentsPushed;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
