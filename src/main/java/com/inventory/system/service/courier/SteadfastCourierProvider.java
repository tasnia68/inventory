package com.inventory.system.service.courier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.CourierDispatchStatus;
import com.inventory.system.common.entity.CourierProfile;
import com.inventory.system.common.entity.DeliveryReviewStatus;
import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.entity.Shipment;
import com.inventory.system.common.entity.ShippingRateCard;
import com.inventory.system.repository.ShippingRateCardRepository;
import com.inventory.system.service.TenantSettingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteadfastCourierProvider implements CourierProvider {

    public static final String PROVIDER_CODE = "STEADFAST";
    private static final String DEFAULT_BASE_URL = "https://portal.packzy.com/api/v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ShippingRateCardRepository rateCardRepository;
    private final TenantSettingService tenantSettingService;

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public CourierBookingResult bookShipment(Shipment shipment, CourierProfile profile) {
        if (shipment.getCourierReference() != null && !shipment.getCourierReference().isBlank()) {
            throw new CourierProviderException("Shipment already booked with reference " + shipment.getCourierReference());
        }

        var salesOrder = shipment.getSalesOrder();
        var customer = salesOrder != null ? salesOrder.getCustomer() : null;
        BigDecimal codAmount = shipment.getCashOnDeliveryAmount() != null
                ? shipment.getCashOnDeliveryAmount()
                : BigDecimal.ZERO;

        Map<String, Object> body = Map.of(
                "invoice", shipment.getShipmentNumber(),
                "recipient_name", customer != null && customer.getName() != null ? customer.getName() : "N/A",
                "recipient_phone", customer != null && customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "",
                "recipient_address", customer != null && customer.getAddress() != null ? customer.getAddress() : "N/A",
                "cod_amount", codAmount,
                "note", shipment.getNotes() != null ? shipment.getNotes() : ""
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders(profile));

        try {
            ResponseEntity<SteadfastCreateResponse> response = restTemplate.postForEntity(
                    resolveBaseUrl(profile) + "/create_order", request, SteadfastCreateResponse.class);

            SteadfastCreateResponse result = response.getBody();
            if (result == null || result.getConsignment() == null) {
                throw new CourierProviderException("Steadfast API returned an empty response");
            }
            SteadfastConsignment consignment = result.getConsignment();
            return new CourierBookingResult(
                    PROVIDER_CODE,
                    String.valueOf(consignment.getConsignmentId()),
                    consignment.getTrackingCode(),
                    consignment.getTrackingCode() != null ? "https://steadfast.com.bd/t/" + consignment.getTrackingCode() : null,
                    CourierDispatchStatus.BOOKED,
                    "Consignment created: " + consignment.getStatus(),
                    LocalDateTime.now()
            );
        } catch (RestClientException e) {
            log.error("Steadfast API call failed for shipment {}: {}", shipment.getId(), e.getMessage());
            throw new CourierProviderException("Failed to book with Steadfast: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelShipment(Shipment shipment, CourierProfile profile) {
        throw new CourierProviderException(
                "Steadfast does not expose a consignment-cancel API. Cancel manually via the Steadfast portal.");
    }

    @Override
    public CourierStatusResult syncStatus(Shipment shipment, CourierProfile profile) {
        String consignmentId = shipment.getCourierReference();
        if (consignmentId == null || consignmentId.isBlank()) {
            throw new CourierProviderException("No Steadfast consignment ID on this shipment. Book it first.");
        }

        HttpEntity<?> request = new HttpEntity<>(buildHeaders(profile));

        try {
            ResponseEntity<SteadfastStatusResponse> response = restTemplate.exchange(
                    resolveBaseUrl(profile) + "/status_by_cid/" + consignmentId,
                    HttpMethod.GET, request, SteadfastStatusResponse.class);

            SteadfastStatusResponse result = response.getBody();
            if (result == null) {
                throw new CourierProviderException("Steadfast API returned an empty status response");
            }
            CourierDispatchStatus mapped = mapDispatchStatus(result.getDeliveryStatus());
            DeliveryReviewStatus reviewStatus = mapReviewStatus(result.getDeliveryStatus());
            String reviewReason = reviewStatus == DeliveryReviewStatus.PENDING
                    ? "Steadfast requires internal review for status: " + result.getDeliveryStatus()
                    : null;
            return new CourierStatusResult(
                    mapped,
                    reviewStatus,
                    reviewReason,
                    "steadfast:" + result.getDeliveryStatus(),
                    LocalDateTime.now()
            );
        } catch (RestClientException e) {
            log.error("Steadfast status check failed for shipment {}: {}", shipment.getId(), e.getMessage());
            throw new CourierProviderException("Failed to sync status from Steadfast: " + e.getMessage(), e);
        }
    }

    @Override
    public CourierFeeQuote calculateFee(DeliveryZone zone, BigDecimal weightKg, BigDecimal codAmount, CourierProfile profile) {
        ShippingRateCard card = rateCardRepository.findFirstByCourierProfileIdAndZone(profile.getId(), zone)
                .orElseThrow(() -> new CourierProviderException(
                        "No rate card configured for profile " + profile.getId() + " zone " + zone));
        return quoteFromRateCard(card, weightKg, codAmount);
    }

    @Override
    public BigDecimal getBalance(CourierProfile profile) {
        HttpEntity<?> request = new HttpEntity<>(buildHeaders(profile));
        try {
            ResponseEntity<SteadfastBalanceResponse> response = restTemplate.exchange(
                    resolveBaseUrl(profile) + "/get_balance", HttpMethod.GET, request, SteadfastBalanceResponse.class);
            SteadfastBalanceResponse body = response.getBody();
            return body != null && body.getCurrentBalance() != null ? body.getCurrentBalance() : BigDecimal.ZERO;
        } catch (RestClientException e) {
            log.error("Steadfast balance check failed: {}", e.getMessage());
            throw new CourierProviderException("Failed to check Steadfast balance: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsUpdate() {
        return false;
    }

    @Override
    public CourierReturnResult requestReturn(Shipment shipment, CourierProfile profile, String reason) {
        String consignmentId = shipment.getCourierReference();
        if (consignmentId == null || consignmentId.isBlank()) {
            throw new CourierProviderException("No Steadfast consignment ID on this shipment. Book it first.");
        }
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("consignment_id", consignmentId);
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, buildHeaders(profile));
        try {
            ResponseEntity<SteadfastReturnRequestResponse> response = restTemplate.postForEntity(
                    resolveBaseUrl(profile) + "/create_return_request", request, SteadfastReturnRequestResponse.class);
            SteadfastReturnRequestResponse result = response.getBody();
            if (result == null) {
                throw new CourierProviderException("Steadfast API returned an empty return-request response");
            }
            return new CourierReturnResult(
                    PROVIDER_CODE,
                    result.getId() != null ? result.getId().toString() : null,
                    consignmentId,
                    result.getStatus(),
                    result.getReason(),
                    LocalDateTime.now()
            );
        } catch (RestClientException e) {
            log.error("Steadfast return-request failed for shipment {}: {}", shipment.getId(), e.getMessage());
            throw new CourierProviderException("Failed to request Steadfast return: " + e.getMessage(), e);
        }
    }

    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    static class SteadfastReturnRequestResponse {
        private Long id;
        @com.fasterxml.jackson.annotation.JsonProperty("user_id")
        private Long userId;
        @com.fasterxml.jackson.annotation.JsonProperty("consignment_id")
        private Long consignmentId;
        private String reason;
        private String status;
    }

    static CourierFeeQuote quoteFromRateCard(ShippingRateCard card, BigDecimal weightKg, BigDecimal codAmount) {
        BigDecimal customerCharge = card.getCustomerCharge() != null ? card.getCustomerCharge() : BigDecimal.ZERO;
        if (weightKg != null && card.getWeightKgIncluded() != null && card.getPerKgOverage() != null) {
            BigDecimal overage = weightKg.subtract(card.getWeightKgIncluded()).max(BigDecimal.ZERO);
            customerCharge = customerCharge.add(overage.multiply(card.getPerKgOverage()));
        }
        BigDecimal codFeePercent = card.getCodFeePercent() != null ? card.getCodFeePercent() : BigDecimal.ZERO;
        BigDecimal codFee = codAmount != null
                ? codAmount.multiply(codFeePercent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal totalCustomerPayable = customerCharge.add(codFee);
        return new CourierFeeQuote(customerCharge, card.getCourierCost(), codFeePercent, codFee, totalCustomerPayable);
    }

    private String resolveBaseUrl(CourierProfile profile) {
        Map<String, Object> config = readJsonMap(profile.getConfigJson());
        Object override = config.get("base_url");
        return override != null && !override.toString().isBlank() ? override.toString() : DEFAULT_BASE_URL;
    }

    private HttpHeaders buildHeaders(CourierProfile profile) {
        Map<String, Object> credentials = readJsonMap(profile.getCredentialsJson());
        String apiKey = stringOrNull(credentials.get("api_key"));
        String secretKey = stringOrNull(credentials.get("secret_key"));

        if (apiKey == null || secretKey == null) {
            apiKey = apiKey != null ? apiKey : legacyTenantSetting("steadfast.api_key");
            secretKey = secretKey != null ? secretKey : legacyTenantSetting("steadfast.secret_key");
        }

        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new CourierProviderException(
                    "Steadfast API keys not configured on courier profile " + profile.getId()
                            + ". Set credentials_json {api_key, secret_key}.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);
        return headers;
    }

    private String legacyTenantSetting(String key) {
        return tenantSettingService.findSetting(key).map(s -> s.getValue()).orElse(null);
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new CourierProviderException("Failed to parse JSON config: " + e.getMessage(), e);
        }
    }

    private static String stringOrNull(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s.isBlank() ? null : s;
    }

    static CourierDispatchStatus mapDispatchStatus(String steadfastStatus) {
        if (steadfastStatus == null) return CourierDispatchStatus.BOOKED;
        return switch (steadfastStatus.toLowerCase()) {
            case "in_review", "pending" -> CourierDispatchStatus.BOOKED;
            case "delivered", "delivered_approval_pending",
                 "partial_delivered", "partial_delivered_approval_pending" -> CourierDispatchStatus.DELIVERED;
            case "cancelled", "cancelled_approval_pending" -> CourierDispatchStatus.CANCELLED;
            case "hold" -> CourierDispatchStatus.PICKUP_PENDING;
            case "unknown", "unknown_approval_pending" -> CourierDispatchStatus.BOOKED;
            default -> CourierDispatchStatus.IN_TRANSIT;
        };
    }

    static DeliveryReviewStatus mapReviewStatus(String steadfastStatus) {
        if (steadfastStatus == null) return DeliveryReviewStatus.NOT_REQUIRED;
        return steadfastStatus.toLowerCase().endsWith("_approval_pending")
                ? DeliveryReviewStatus.PENDING
                : DeliveryReviewStatus.NOT_REQUIRED;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SteadfastCreateResponse {
        private int status;
        private String message;
        private SteadfastConsignment consignment;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SteadfastConsignment {
        @JsonProperty("consignment_id")
        private long consignmentId;
        private String invoice;
        @JsonProperty("tracking_code")
        private String trackingCode;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SteadfastStatusResponse {
        private int status;
        @JsonProperty("delivery_status")
        private String deliveryStatus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SteadfastBalanceResponse {
        private int status;
        @JsonProperty("current_balance")
        private BigDecimal currentBalance;
    }

    public java.util.List<SteadfastPayment> getPayments(CourierProfile profile) {
        HttpEntity<?> request = new HttpEntity<>(buildHeaders(profile));
        try {
            ResponseEntity<SteadfastPaymentListResponse> response = restTemplate.exchange(
                    resolveBaseUrl(profile) + "/payments", HttpMethod.GET, request, SteadfastPaymentListResponse.class);
            SteadfastPaymentListResponse body = response.getBody();
            if (body == null || body.getData() == null) return java.util.List.of();
            return body.getData();
        } catch (RestClientException e) {
            log.error("Steadfast /payments call failed: {}", e.getMessage());
            throw new CourierProviderException("Failed to list Steadfast payments: " + e.getMessage(), e);
        }
    }

    public SteadfastPaymentDetail getPayment(CourierProfile profile, String paymentId) {
        HttpEntity<?> request = new HttpEntity<>(buildHeaders(profile));
        try {
            ResponseEntity<SteadfastPaymentDetailResponse> response = restTemplate.exchange(
                    resolveBaseUrl(profile) + "/payments/" + paymentId,
                    HttpMethod.GET, request, SteadfastPaymentDetailResponse.class);
            SteadfastPaymentDetailResponse body = response.getBody();
            return body != null ? body.getData() : null;
        } catch (RestClientException e) {
            log.error("Steadfast /payments/{} call failed: {}", paymentId, e.getMessage());
            throw new CourierProviderException("Failed to fetch Steadfast payment " + paymentId + ": " + e.getMessage(), e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastPaymentListResponse {
        private int status;
        private java.util.List<SteadfastPayment> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastPaymentDetailResponse {
        private int status;
        private SteadfastPaymentDetail data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastPayment {
        private Long id;
        private BigDecimal amount;
        @JsonProperty("service_charge")
        private BigDecimal serviceCharge;
        private BigDecimal fee;
        @JsonProperty("net_amount")
        private BigDecimal netAmount;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("paid_at")
        private String paidAt;

        public BigDecimal resolvedFee() {
            if (fee != null) return fee;
            return serviceCharge != null ? serviceCharge : BigDecimal.ZERO;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastPaymentDetail {
        private Long id;
        private BigDecimal amount;
        @JsonProperty("service_charge")
        private BigDecimal serviceCharge;
        private BigDecimal fee;
        @JsonProperty("net_amount")
        private BigDecimal netAmount;
        private java.util.List<SteadfastPaymentConsignment> consignments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteadfastPaymentConsignment {
        @JsonProperty("consignment_id")
        private Long consignmentId;
        private String invoice;
        @JsonProperty("cod_amount")
        private BigDecimal codAmount;
    }
}
