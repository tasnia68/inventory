package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "picking_lists")
@Getter
@Setter
public class PickingList extends BaseEntity {

    @Column(name = "picking_number", nullable = false, unique = true)
    private String pickingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickingStatus status = PickingStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickingType type = PickingType.SINGLE_ORDER;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "pickingList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PickingTask> tasks = new ArrayList<>();
}
