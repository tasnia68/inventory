package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontDomainContextDto {
    private String platformFallbackHost;
    private String platformFallbackUrl;
    private String primaryHostname;
    private String primaryUrl;
    private String verificationTarget;
    private List<StorefrontDomainDto> domains = new ArrayList<>();
}
