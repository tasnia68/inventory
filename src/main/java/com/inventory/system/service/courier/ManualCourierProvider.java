package com.inventory.system.service.courier;

import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryReviewStatus;
import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShippingRateCard;
import com.inventory.system.repository.ShippingRateCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ManualCourierProvider implements CourierProvider {

    public static final String PROVIDER_CODE = "MANUAL";

    private final ShippingRateCardRepository rateCardRepository;

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public CourierBookingResult bookShipment(Shipment shipment, CourierProfile profile) {
        String reference = "MANUAL-" + UUID.randomUUID();
        return new CourierBookingResult(
                PROVIDER_CODE,
                reference,
                reference,
                null,
                CourierDispatchStatus.UNASSIGNED,
                "Manual handoff: " + reference,
                LocalDateTime.now()
        );
    }

    @Override
    public void cancelShipment(Shipment shipment, CourierProfile profile) {
        // no-op — manual shipments are cancelled by updating the SalesOrder directly
    }

    @Override
    public CourierStatusResult syncStatus(Shipment shipment, CourierProfile profile) {
        CourierDispatchStatus current = shipment.getCourierDispatchStatus() != null
                ? shipment.getCourierDispatchStatus()
                : CourierDispatchStatus.UNASSIGNED;
        return new CourierStatusResult(
                current,
                DeliveryReviewStatus.NOT_REQUIRED,
                null,
                "manual:no-sync",
                LocalDateTime.now()
        );
    }

    @Override
    public CourierFeeQuote calculateFee(DeliveryZone zone, BigDecimal weightKg, BigDecimal codAmount, CourierProfile profile) {
        ShippingRateCard card = rateCardRepository.findFirstByCourierProfileIdAndZone(profile.getId(), zone).orElse(null);
        if (card == null) {
            return new CourierFeeQuote(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return SteadfastCourierProvider.quoteFromRateCard(card, weightKg, codAmount);
    }

    @Override
    public BigDecimal getBalance(CourierProfile profile) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }
}
