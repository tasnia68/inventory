package com.inventory.system.config.tenant.routing;

/**
 * Outcome of resolving how a tenant's persistence is routed.
 *
 * <p>{@code shared == true}: use the shared pool ({@code routingKey} is the
 * shared sentinel). Otherwise the tenant has a dedicated database and the
 * connection fields are populated (decrypted in-memory, never logged).
 */
public record ResolvedRouting(
        boolean shared,
        String routingKey,
        String jdbcUrl,
        String username,
        String password,
        String host) {

    public static ResolvedRouting shared(String sharedKey) {
        return new ResolvedRouting(true, sharedKey, null, null, null, null);
    }

    public static ResolvedRouting dedicated(String tenantId, String jdbcUrl,
                                            String username, String password, String host) {
        return new ResolvedRouting(false, tenantId, jdbcUrl, username, password, host);
    }
}
