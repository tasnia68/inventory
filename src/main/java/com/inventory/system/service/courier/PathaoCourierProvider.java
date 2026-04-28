package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.Shipment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pathao courier integration stub. Registered with the CourierProviderRegistry so profiles
 * referencing PATHAO validate at creation time; concrete API calls throw until the Pathao
 * Merchant API contract (OAuth issue-token, /aladdin/api/v1/orders, status webhook) is wired.
 */
@Component
public class PathaoCourierProvider implements CourierProvider {

    public static final String PROVIDER_CODE = "PATHAO";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public CourierBookingResult bookShipment(Shipment shipment, CourierProfile profile) {
        throw new CourierProviderException("Pathao API contract not yet wired; booking unavailable");
    }

    @Override
    public void cancelShipment(Shipment shipment, CourierProfile profile) {
        throw new CourierProviderException("Pathao API contract not yet wired; cancel unavailable");
    }

    @Override
    public CourierStatusResult syncStatus(Shipment shipment, CourierProfile profile) {
        throw new CourierProviderException("Pathao API contract not yet wired; status sync unavailable");
    }

    @Override
    public CourierFeeQuote calculateFee(DeliveryZone zone, BigDecimal weightKg, BigDecimal codAmount, CourierProfile profile) {
        throw new CourierProviderException("Pathao API contract not yet wired; fee quote unavailable");
    }

    @Override
    public BigDecimal getBalance(CourierProfile profile) {
        throw new CourierProviderException("Pathao API contract not yet wired; balance unavailable");
    }

    @Override
    public boolean supportsUpdate() {
        return false;
    }
}
