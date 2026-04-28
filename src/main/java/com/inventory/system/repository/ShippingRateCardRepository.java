package com.inventory.system.repository;

import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.ShippingRateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingRateCardRepository extends JpaRepository<ShippingRateCard, UUID> {

    List<ShippingRateCard> findByCourierProfileId(UUID courierProfileId);

    Optional<ShippingRateCard> findFirstByCourierProfileIdAndZone(UUID courierProfileId, DeliveryZone zone);
}
