package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Read view of a tenant's datasource configuration. Deliberately omits all
 * credentials — only the host of the JDBC URL is exposed (for display), never
 * the URL with userinfo, username, or password.
 */
@Data
@AllArgsConstructor
public class TenantDatasourceResponse {
    private String tenantId;
    private String mode;
    private String status;
    private String flywayVersion;
    private String host;
    private boolean credentialsConfigured;
    private String lastError;
}
