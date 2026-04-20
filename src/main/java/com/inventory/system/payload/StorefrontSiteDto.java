package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontSiteDto {
    private boolean enabled;
    private String templateKey;
    private String name;
    private String tagline;
    private String announcement;
    private String domain;
    private String modulePlan;
    private String logoUrl;
    private String iconUrl;
    private Integer logoWidth;
    private String productCardHoverMode;
    private Boolean productCardHoverZoom;
    private String drawerButtonLabel;
    private String drawerLabel;
    private String drawerTitle;
    private String drawerDescription;
    private String allCollectionsLabel;
    private String searchPlaceholder;
    private String cartLabel;
    private String warehouseId;
    private String footerCatalogLabel;
    private String footerCollectionsLabel;
    private String footerTrackingLabel;
    private String publishStatus;
    private String publishedVersion;
    private String lastPublishedAt;
}
