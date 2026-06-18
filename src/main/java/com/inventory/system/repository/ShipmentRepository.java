package com.inventory.system.repository;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {
    boolean existsByShipmentNumber(String shipmentNumber);
    List<Shipment> findBySalesOrderId(UUID salesOrderId);
    java.util.Optional<Shipment> findFirstByCourierProviderIgnoreCaseAndCourierReference(String courierProvider, String courierReference);
    java.util.Optional<Shipment> findFirstByTrackingNumber(String trackingNumber);
    List<Shipment> findBySalesOrderIdIn(Collection<UUID> salesOrderIds);

    @Query("""
        select s
        from Shipment s
        where s.tenantId = :tenantId
          and s.status = :status
          and upper(coalesce(s.courierProvider, '')) = upper(:provider)
          and (s.courierReference is null or trim(s.courierReference) = '')
        order by s.createdAt asc
        """)
    List<Shipment> findPendingCourierBooking(
        @Param("tenantId") String tenantId,
        @Param("status") ShipmentStatus status,
        @Param("provider") String provider,
        Pageable pageable
    );

    @Query("""
        select s
        from Shipment s
        where s.tenantId = :tenantId
          and upper(coalesce(s.courierProvider, '')) = upper(:provider)
          and s.status not in :terminalShipmentStatuses
          and s.courierDispatchStatus not in :terminalDispatchStatuses
          and s.courierReference is not null
          and trim(s.courierReference) <> ''
          and (s.lastCourierSyncAt is null or s.lastCourierSyncAt <= :syncBefore)
        order by s.lastCourierSyncAt asc, s.createdAt asc
        """)
    List<Shipment> findCourierShipmentsToSync(
        @Param("tenantId") String tenantId,
        @Param("provider") String provider,
        @Param("terminalShipmentStatuses") List<ShipmentStatus> terminalShipmentStatuses,
        @Param("terminalDispatchStatuses") List<CourierDispatchStatus> terminalDispatchStatuses,
        @Param("syncBefore") LocalDateTime syncBefore,
        Pageable pageable
    );
}