package com.inventory.system.repository;

import com.inventory.system.common.entity.PosPaymentMethod;
import com.inventory.system.common.entity.PosSalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PosSalePaymentRepository extends JpaRepository<PosSalePayment, UUID> {

    List<PosSalePayment> findBySaleId(UUID saleId);

    @Query("select p from PosSalePayment p where p.sale.shift.id = :shiftId")
    List<PosSalePayment> findByShiftId(@Param("shiftId") UUID shiftId);

    @Query("select coalesce(sum(p.amount), 0) from PosSalePayment p where p.sale.shift.id = :shiftId and p.paymentMethod = :paymentMethod")
    BigDecimal sumByShiftAndMethod(@Param("shiftId") UUID shiftId, @Param("paymentMethod") PosPaymentMethod paymentMethod);
}