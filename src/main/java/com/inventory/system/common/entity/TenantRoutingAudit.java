package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit of a datasource routing decision. Control-plane (no
 * tenant filter). Never updated or deleted by the application.
 */
@Entity
@Table(name = "tenant_routing_audit")
@Getter
@Setter
public class TenantRoutingAudit {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    /** SHARED | DEDICATED | DENIED (stored as text; see RoutingDecision). */
    @Column(name = "decision", nullable = false, length = 16)
    private String decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "jdbc_url_host", length = 255)
    private String jdbcUrlHost;

    @Column(name = "principal", length = 255)
    private String principal;

    @Column(name = "request_path", length = 512)
    private String requestPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
