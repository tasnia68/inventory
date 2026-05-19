package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Control-plane catalog row describing how a tenant's persistence is routed.
 *
 * <p>Deliberately extends {@link AuditableEntity} (audit columns only) and
 * <strong>not</strong> {@link BaseEntity}: this is global control-plane data,
 * read before the tenant's database is known, so it must never carry the
 * {@code tenantFilter} discriminator.
 *
 * <p>Credential columns hold AES-GCM ciphertext produced by
 * {@code CredentialCipher}; the KEK is supplied via the environment and is
 * never persisted here. Credentials are write-only at the API boundary and
 * must never be serialized into any response.
 */
@Entity
@Table(name = "tenant_datasource")
@Getter
@Setter
public class TenantDatasource extends AuditableEntity {

    /** The tenant routing identifier (same string used by {@code TenantContext}). */
    @Id
    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TenantDatasourceMode mode = TenantDatasourceMode.SHARED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TenantDatasourceStatus status = TenantDatasourceStatus.ACTIVE;

    @Column(name = "jdbc_url_enc", columnDefinition = "TEXT")
    private String jdbcUrlEnc;

    @Column(name = "jdbc_username_enc", columnDefinition = "TEXT")
    private String jdbcUsernameEnc;

    @Column(name = "jdbc_password_enc", columnDefinition = "TEXT")
    private String jdbcPasswordEnc;

    @Column(name = "key_id", length = 64)
    private String keyId;

    @Column(name = "pool_max_size")
    private Integer poolMaxSize;

    @Column(name = "pool_min_idle")
    private Integer poolMinIdle;

    @Column(name = "idle_timeout_ms")
    private Long idleTimeoutMs;

    @Column(name = "flyway_version", length = 32)
    private String flywayVersion;

    @Column(name = "provisioned_at")
    private LocalDateTime provisionedAt;

    @Column(name = "last_migrated_at")
    private LocalDateTime lastMigratedAt;

    @Column(name = "last_routed_at")
    private LocalDateTime lastRoutedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
