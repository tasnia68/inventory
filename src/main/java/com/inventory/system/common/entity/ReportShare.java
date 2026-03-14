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
@Table(name = "report_shares")
@Getter
@Setter
public class ReportShare extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_configuration_id", nullable = false)
    private ReportConfiguration reportConfiguration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWithUser;

    @Column(name = "access_level", nullable = false)
    private String accessLevel = "VIEW";
}