package com.inventory.system.payload;

import lombok.Data;

/**
 * Write-only datasource configuration for a tenant. Credentials are accepted
 * here, encrypted before persistence, and never read back out (see
 * {@link TenantDatasourceResponse}).
 */
@Data
public class TenantDatasourceRequest {
    /** SHARED or DEDICATED. */
    private String mode;
    private String jdbcUrl;
    private String username;
    private String password;
    private Integer poolMaxSize;
}
