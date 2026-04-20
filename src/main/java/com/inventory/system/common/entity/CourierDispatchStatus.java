package com.inventory.system.common.entity;

public enum CourierDispatchStatus {
    UNASSIGNED,
    BOOKED,
    PICKUP_PENDING,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNED,
    CANCELLED
}
