package com.inventory.system.repository;

import com.inventory.system.common.entity.PosSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PosSaleRepository extends JpaRepository<PosSale, UUID> {
    Optional<PosSale> findByClientSaleId(String clientSaleId);

    Page<PosSale> findByCashierIdOrderBySaleTimeDesc(UUID cashierId, Pageable pageable);

    Page<PosSale> findByTerminalIdOrderBySaleTimeDesc(UUID terminalId, Pageable pageable);

    @Query("""
            select coalesce(sum(s.totalAmount), 0)
            from PosSale s
            where (:cashierId is null or s.cashier.id = :cashierId)
              and (:terminalId is null or s.terminal.id = :terminalId)
              and s.saleTime between :from and :to
              and s.saleStatus = com.inventory.system.common.entity.PosSaleStatus.COMPLETED
            """)
    BigDecimal sumTotalAmount(@Param("cashierId") UUID cashierId,
                              @Param("terminalId") UUID terminalId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(i.quantity), 0)
            from PosSale s
            join s.items i
            where (:cashierId is null or s.cashier.id = :cashierId)
              and (:terminalId is null or s.terminal.id = :terminalId)
              and s.saleTime between :from and :to
              and s.saleStatus = com.inventory.system.common.entity.PosSaleStatus.COMPLETED
            """)
    BigDecimal sumUnits(@Param("cashierId") UUID cashierId,
                        @Param("terminalId") UUID terminalId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

    @Query("""
            select count(s)
            from PosSale s
            where (:cashierId is null or s.cashier.id = :cashierId)
              and (:terminalId is null or s.terminal.id = :terminalId)
              and s.saleTime between :from and :to
              and s.saleStatus = com.inventory.system.common.entity.PosSaleStatus.COMPLETED
            """)
    long countCompletedSales(@Param("cashierId") UUID cashierId,
                             @Param("terminalId") UUID terminalId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);
}