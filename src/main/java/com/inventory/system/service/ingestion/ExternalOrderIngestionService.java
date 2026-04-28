package com.inventory.system.service.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookEvent;
import com.inventory.system.common.entity.InboundWebhookStatus;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.MaterializeWebhookRequest;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.repository.InboundWebhookEventRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalOrderIngestionService {

    private final InboundWebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final ProductVariantRepository productVariantRepository;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRepository salesOrderRepository;

    @Transactional
    public IngestionResult ingest(ExternalOrderSource source, String topic, String rawPayload, String signature) {
        String externalOrderId = extractExternalOrderId(source, rawPayload);

        if (externalOrderId != null) {
            var existing = webhookEventRepository.findFirstBySourceAndExternalEventId(source, externalOrderId);
            if (existing.isPresent()) {
                return new IngestionResult(InboundWebhookStatus.DUPLICATE, existing.get().getId(),
                        existing.get().getSalesOrderId(), null);
            }
        }

        InboundWebhookEvent event = new InboundWebhookEvent();
        event.setSource(source);
        event.setExternalEventId(externalOrderId);
        event.setTopic(topic);
        event.setPayload(rawPayload != null ? rawPayload : "");
        event.setSignature(signature);
        event.setStatus(InboundWebhookStatus.VERIFIED);
        event.setReceivedAt(LocalDateTime.now());

        try {
            event = webhookEventRepository.save(event);
            return new IngestionResult(InboundWebhookStatus.VERIFIED, event.getId(), null, null);
        } catch (DataIntegrityViolationException dup) {
            return new IngestionResult(InboundWebhookStatus.DUPLICATE, null, null, null);
        } catch (Exception e) {
            log.error("Failed to persist {} webhook event: {}", source, e.getMessage(), e);
            return new IngestionResult(InboundWebhookStatus.FAILED, null, null, e.getMessage());
        }
    }

    private String extractExternalOrderId(ExternalOrderSource source, String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode idNode = root.get("id");
            if (idNode == null || idNode.isNull()) return null;
            String value = idNode.asText();
            return value == null || value.isBlank() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public SalesOrderDto materialize(UUID eventId, MaterializeWebhookRequest request) {
        InboundWebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("InboundWebhookEvent", "id", eventId));

        if (event.getStatus() == InboundWebhookStatus.PROCESSED && event.getSalesOrderId() != null) {
            throw new BadRequestException("Webhook event already materialized as sales order " + event.getSalesOrderId());
        }
        if (event.getStatus() != InboundWebhookStatus.VERIFIED && event.getStatus() != InboundWebhookStatus.RECEIVED) {
            throw new BadRequestException("Webhook event is in " + event.getStatus() + " status; cannot materialize");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(event.getPayload());
        } catch (Exception e) {
            throw new BadRequestException("Cannot parse stored payload: " + e.getMessage());
        }

        List<SalesOrderItemRequest> items = new ArrayList<>(request.getItems());
        if (items.isEmpty() && request.isAutoMapItemsBySku()) {
            items = autoMapItems(event.getSource(), root);
        }
        if (items.isEmpty()) {
            throw new BadRequestException("No items to materialize. Provide items[] or enable autoMapItemsBySku.");
        }

        SalesOrderRequest soRequest = new SalesOrderRequest();
        soRequest.setCustomerId(request.getCustomerId());
        soRequest.setWarehouseId(request.getWarehouseId());
        soRequest.setItems(items);
        soRequest.setCurrency(request.getCurrency() != null ? request.getCurrency() : textOrNull(root.get("currency")));
        soRequest.setNotes(request.getNotes() != null ? request.getNotes() : textOrNull(root.get("note")));

        SalesOrderDto created = salesOrderService.createSalesOrder(soRequest);

        SalesOrder salesOrder = salesOrderRepository.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", created.getId()));
        salesOrder.setExternalSource(event.getSource());
        salesOrder.setExternalOrderId(event.getExternalEventId());
        String externalRef = textOrNull(root.get("order_number"));
        if (externalRef == null) externalRef = textOrNull(root.get("name"));
        salesOrder.setExternalOrderRef(externalRef);
        salesOrderRepository.save(salesOrder);

        event.setSalesOrderId(salesOrder.getId());
        event.setStatus(InboundWebhookStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(event);

        return created;
    }

    private List<SalesOrderItemRequest> autoMapItems(ExternalOrderSource source, JsonNode root) {
        JsonNode lineItems = source == ExternalOrderSource.WOOCOMMERCE
                ? firstArray(root, "line_items")
                : firstArray(root, "line_items");
        if (lineItems == null || !lineItems.isArray()) {
            throw new BadRequestException("Payload contains no line_items[] to auto-map");
        }

        List<SalesOrderItemRequest> result = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (JsonNode line : lineItems) {
            String sku = textOrNull(line.get("sku"));
            if (sku == null) {
                missing.add("(line without sku id=" + textOrNull(line.get("id")) + ")");
                continue;
            }
            ProductVariant variant = productVariantRepository.findBySku(sku).orElse(null);
            if (variant == null) {
                missing.add(sku);
                continue;
            }
            SalesOrderItemRequest item = new SalesOrderItemRequest();
            item.setProductVariantId(variant.getId());
            BigDecimal qty = parseDecimal(line.get("quantity"));
            item.setQuantity(qty.compareTo(BigDecimal.ZERO) > 0 ? qty : BigDecimal.ONE);
            BigDecimal unitPrice = parseDecimal(line.get("price"));
            item.setUnitPrice(unitPrice);
            result.add(item);
        }
        if (!missing.isEmpty()) {
            throw new BadRequestException(
                    "Cannot map line items by SKU; missing variants for: " + String.join(", ", missing));
        }
        return result;
    }

    private static JsonNode firstArray(JsonNode root, String key) {
        if (root == null) return null;
        return root.get(key);
    }

    private static BigDecimal parseDecimal(JsonNode node) {
        String text = textOrNull(node);
        if (text == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    public record IngestionResult(InboundWebhookStatus status, UUID eventId, UUID salesOrderId, String error) {
    }
}
