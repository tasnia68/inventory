package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "damage_record_documents")
@Getter
@Setter
public class DamageRecordDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_record_id", nullable = false)
    private DamageRecord damageRecord;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}