package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.Shipment;

import java.math.BigDecimal;

public interface CourierProvider {

    String providerCode();

    CourierBookingResult bookShipment(Shipment shipment, CourierProfile profile);

    void cancelShipment(Shipment shipment, CourierProfile profile);

    CourierStatusResult syncStatus(Shipment shipment, CourierProfile profile);

    CourierFeeQuote calculateFee(DeliveryZone zone, BigDecimal weightKg, BigDecimal codAmount, CourierProfile profile);

    BigDecimal getBalance(CourierProfile profile);

    boolean supportsUpdate();

    default CourierReturnResult requestReturn(Shipment shipment, CourierProfile profile, String reason) {
        throw new CourierProviderException(providerCode() + " does not expose a return-request API");
    }
}
