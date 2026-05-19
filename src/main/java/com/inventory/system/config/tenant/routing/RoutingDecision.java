package com.inventory.system.config.tenant.routing;

/** Outcome recorded for every datasource routing decision (audit). */
public enum RoutingDecision {
    /** Routed to the shared schema pool. */
    SHARED,
    /** Routed to the tenant's dedicated database pool. */
    DEDICATED,
    /** Fail-closed: dedicated tenant could not be served; request errored. */
    DENIED
}
