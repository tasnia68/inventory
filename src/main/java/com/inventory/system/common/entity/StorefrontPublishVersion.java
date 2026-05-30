package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "storefront_publish_versions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "version_number"})
})
@Getter
@Setter
public class StorefrontPublishVersion extends BaseEntity {

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "restored_from_version_number")
    private Integer restoredFromVersionNumber;

    @Column(name = "theme_key", length = 128)
    private String themeKey;

    @Column(name = "theme_version", length = 64)
    private String themeVersion;
}

