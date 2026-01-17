package com.inventory.system.repository;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.SerialNumberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, UUID> {
    Optional<SerialNumber> findBySerialNumberAndProductVariantId(String serialNumber, UUID productVariantId);
    List<SerialNumber> findByProductVariantAndStatus(ProductVariant productVariant, SerialNumberStatus status);
    List<SerialNumber> findAllBySerialNumber(String serialNumber);
}
