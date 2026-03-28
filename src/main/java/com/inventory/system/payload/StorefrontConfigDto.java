package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontConfigDto {
    private StorefrontSiteDto site;
    private StorefrontThemeDto theme;
    private List<StorefrontNavItemDto> navigationItems = new ArrayList<>();
    private List<StorefrontBannerDto> banners = new ArrayList<>();
    private StorefrontPageDto headerPage;
    private StorefrontPageDto homePage;
    private StorefrontPageDto footerPage;
}
