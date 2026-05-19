package com.inventory.system.config.tenant.routing;

/**
 * Thrown when a tenant configured for a DEDICATED database cannot be served
 * (DB unreachable, credentials invalid, status not ACTIVE, or schema behind
 * the application baseline).
 *
 * <p><strong>Fail-closed contract:</strong> when this is thrown the request
 * MUST error. The routing layer must never substitute the shared database for
 * a DEDICATED tenant — that would leak or mis-write data. Mapped to HTTP 503.
 */
public class TenantDatasourceUnavailableException extends RuntimeException {

    public TenantDatasourceUnavailableException(String message) {
        super(message);
    }

    public TenantDatasourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
