package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a permission that can be assigned to roles.
 *
 * <p><strong>Multi-tenancy note:</strong> Permission is intentionally system-wide and does NOT
 * extend {@link BaseEntity}. Permissions are defined at deploy time and shared across all tenants.
 * They are immutable in production — tenants cannot create, modify, or delete permissions.
 * The Hibernate tenant filter ({@code tenantFilter}) is therefore NOT applied to this entity.
 *
 * <p>If per-tenant custom permissions are required in the future, this class must be migrated
 * to extend {@link BaseEntity} and a corresponding Flyway migration must add a {@code tenant_id}
 * column to the {@code permissions} table.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;
}
