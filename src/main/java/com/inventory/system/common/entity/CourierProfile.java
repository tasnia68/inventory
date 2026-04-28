package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "courier_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_courier_profiles_tenant_provider_name",
                columnNames = {"tenant_id", "provider_code", "display_name"}
        )
)
@Getter
@Setter
public class CourierProfile extends BaseEntity {

    @Column(name = "provider_code", nullable = false, length = 32)
    private String providerCode;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "credentials_json", columnDefinition = "TEXT")
    private String credentialsJson;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;
}
