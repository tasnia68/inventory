package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.DataImportHistory;
import com.inventory.system.common.entity.DataImportStatus;
import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.ReportType;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateSupplierRequest;
import com.inventory.system.payload.DataExchangeDataset;
import com.inventory.system.payload.DataImportHistoryDto;
import com.inventory.system.payload.DataImportValidationResultDto;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.payload.PurchaseOrderItemRequest;
import com.inventory.system.payload.PurchaseOrderRequest;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.repository.DataImportHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataExchangeServiceImpl implements DataExchangeService {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};

    private final DataImportHistoryRepository dataImportHistoryRepository;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final StockService stockService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor reportingTaskExecutor;
    private final WebhookService webhookService;

    @Override
    @Transactional(readOnly = true)
    public DataImportValidationResultDto validateImport(DataExchangeDataset dataset, MultipartFile file) {
        byte[] content = readContent(file);
        List<CSVRecord> records = parseCsv(content);
        DataImportValidationResultDto result = new DataImportValidationResultDto();
        result.setTotalRecords(records.size());

        if (records.isEmpty()) {
            result.getErrors().add("Import file must contain at least one data row");
        }

        List<String> requiredHeaders = requiredHeaders(dataset);
        if (!records.isEmpty()) {
            for (String header : requiredHeaders) {
                if (!records.get(0).isMapped(header)) {
                    result.getErrors().add("Missing required column: " + header);
                }
            }
        }

        int rowNumber = 2;
        for (CSVRecord record : records) {
            validateRecord(dataset, record, rowNumber, result.getErrors());
            rowNumber++;
        }

        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    @Override
    @Transactional
    public DataImportHistoryDto startImport(DataExchangeDataset dataset, MultipartFile file) {
        byte[] content = readContent(file);
        DataImportValidationResultDto validation = validateImport(dataset, file);

        DataImportHistory history = new DataImportHistory();
        history.setDataset(dataset.name());
        history.setFileName(file.getOriginalFilename() == null ? dataset.name().toLowerCase() + ".csv" : file.getOriginalFilename());
        history.setRequestedAt(LocalDateTime.now());
        history.setTotalRecords(validation.getTotalRecords());
        history.setProcessedRecords(0);
        history.setSuccessfulRecords(0);
        history.setFailedRecords(0);
        history.setValidationErrors(writeErrors(validation.getErrors()));

        if (!validation.isValid()) {
            history.setStatus(DataImportStatus.FAILED);
            history.setCompletedAt(LocalDateTime.now());
            history.setSummaryMessage("Import validation failed");
            return mapHistory(dataImportHistoryRepository.save(history));
        }

        history.setStatus(DataImportStatus.VALIDATED);
        DataImportHistory saved = dataImportHistoryRepository.save(history);
        reportingTaskExecutor.execute(() -> processImport(saved.getId(), dataset, content));
        return mapHistory(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataImportHistoryDto> getImportHistory() {
        return dataImportHistoryRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::mapHistory)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DataImportHistoryDto getImportHistory(UUID id) {
        return mapHistory(getHistoryEntity(id));
    }

    @Transactional
    protected void processImport(UUID historyId, DataExchangeDataset dataset, byte[] content) {
        DataImportHistory history = getHistoryEntity(historyId);
        history.setStatus(DataImportStatus.PROCESSING);
        history.setStartedAt(LocalDateTime.now());
        dataImportHistoryRepository.save(history);

        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            List<CSVRecord> records = parseCsv(content);
            int processed = 0;
            for (CSVRecord record : records) {
                processed++;
                try {
                    processRecord(dataset, record);
                    successCount++;
                } catch (RuntimeException exception) {
                    failedCount++;
                    errors.add("Row " + (processed + 1) + ": " + exception.getMessage());
                }

                history.setProcessedRecords(processed);
                history.setSuccessfulRecords(successCount);
                history.setFailedRecords(failedCount);
            }

            history.setStatus(errors.isEmpty() ? DataImportStatus.COMPLETED : DataImportStatus.FAILED);
            history.setSummaryMessage(errors.isEmpty()
                    ? "Import completed successfully"
                    : "Import completed with row failures");
            history.setValidationErrors(writeErrors(errors));
            history.setCompletedAt(LocalDateTime.now());
            dataImportHistoryRepository.save(history);

            webhookService.publishEvent(errors.isEmpty() ? WebhookEventType.IMPORT_COMPLETED : WebhookEventType.IMPORT_FAILED,
                    Map.of(
                            "importHistoryId", history.getId(),
                            "dataset", history.getDataset(),
                            "status", history.getStatus().name(),
                            "successfulRecords", history.getSuccessfulRecords(),
                            "failedRecords", history.getFailedRecords()));
        } catch (RuntimeException exception) {
            history.setStatus(DataImportStatus.FAILED);
            history.setSummaryMessage("Import failed: " + exception.getMessage());
            history.setValidationErrors(writeErrors(List.of(exception.getMessage())));
            history.setCompletedAt(LocalDateTime.now());
            dataImportHistoryRepository.save(history);
            webhookService.publishEvent(WebhookEventType.IMPORT_FAILED,
                    Map.of(
                            "importHistoryId", history.getId(),
                            "dataset", history.getDataset(),
                            "status", history.getStatus().name(),
                            "error", exception.getMessage()));
        }
    }

    private void validateRecord(DataExchangeDataset dataset, CSVRecord record, int rowNumber, List<String> errors) {
        try {
            switch (dataset) {
                case PRODUCTS -> {
                    require(record, "sku");
                    require(record, "price");
                    parseDecimal(record, "price");
                    parseUuid(record, "templateId");
                }
                case SUPPLIERS -> require(record, "name");
                case STOCKS -> {
                    parseUuid(record, "productVariantId");
                    parseUuid(record, "warehouseId");
                    parseDecimal(record, "quantity");
                    require(record, "movementType");
                }
                case PURCHASE_ORDERS -> {
                    parseUuid(record, "supplierId");
                    require(record, "itemsJson");
                    parsePurchaseItems(record.get("itemsJson"));
                }
                case SALES_ORDERS -> {
                    parseUuid(record, "customerId");
                    parseUuid(record, "warehouseId");
                    require(record, "itemsJson");
                    parseSalesItems(record.get("itemsJson"));
                }
            }
        } catch (RuntimeException exception) {
            errors.add("Row " + rowNumber + ": " + exception.getMessage());
        }
    }

    private void processRecord(DataExchangeDataset dataset, CSVRecord record) {
        switch (dataset) {
            case PRODUCTS -> importProduct(record);
            case SUPPLIERS -> importSupplier(record);
            case STOCKS -> importStockAdjustment(record);
            case PURCHASE_ORDERS -> importPurchaseOrder(record);
            case SALES_ORDERS -> importSalesOrder(record);
        }
    }

    private void importProduct(CSVRecord record) {
        ProductVariantDto dto = new ProductVariantDto();
        dto.setSku(record.get("sku"));
        dto.setBarcode(blankToNull(record.get("barcode")));
        dto.setPrice(parseDecimal(record, "price"));
        dto.setTemplateId(parseUuid(record, "templateId"));
        dto.setAttributeValues(new ArrayList<>());
        productService.createProductVariant(dto);
    }

    private void importSupplier(CSVRecord record) {
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName(record.get("name"));
        request.setContactName(blankToNull(record.get("contactName")));
        request.setEmail(blankToNull(record.get("email")));
        request.setPhoneNumber(blankToNull(record.get("phoneNumber")));
        request.setAddress(blankToNull(record.get("address")));
        request.setPaymentTerms(blankToNull(record.get("paymentTerms")));
        supplierService.createSupplier(request);
    }

    private void importStockAdjustment(CSVRecord record) {
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(parseUuid(record, "productVariantId"));
        dto.setWarehouseId(parseUuid(record, "warehouseId"));
        dto.setStorageLocationId(optionalUuid(record, "storageLocationId"));
        dto.setBatchId(optionalUuid(record, "batchId"));
        dto.setQuantity(parseDecimal(record, "quantity"));
        dto.setUnitCost(optionalDecimal(record, "unitCost"));
        dto.setType(StockMovement.StockMovementType.valueOf(record.get("movementType").trim().toUpperCase()));
        dto.setReason(blankToNull(record.get("reason")));
        dto.setReferenceId(blankToNull(record.get("referenceId")));
        stockService.adjustStock(dto);
    }

    private void importPurchaseOrder(CSVRecord record) {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setSupplierId(parseUuid(record, "supplierId"));
        request.setExpectedDeliveryDate(optionalDate(record, "expectedDeliveryDate"));
        request.setCurrency(blankToNull(record.get("currency")));
        request.setNotes(blankToNull(record.get("notes")));
        request.setItems(parsePurchaseItems(record.get("itemsJson")));
        purchaseOrderService.createPurchaseOrder(request);
    }

    private void importSalesOrder(CSVRecord record) {
        SalesOrderRequest request = new SalesOrderRequest();
        request.setCustomerId(parseUuid(record, "customerId"));
        request.setWarehouseId(parseUuid(record, "warehouseId"));
        request.setExpectedDeliveryDate(optionalDate(record, "expectedDeliveryDate"));
        if (record.isMapped("priority") && !record.get("priority").isBlank()) {
            request.setPriority(OrderPriority.valueOf(record.get("priority").trim().toUpperCase()));
        }
        request.setCurrency(blankToNull(record.get("currency")));
        request.setNotes(blankToNull(record.get("notes")));
        request.setItems(parseSalesItems(record.get("itemsJson")));
        salesOrderService.createSalesOrder(request);
    }

    private List<PurchaseOrderItemRequest> parsePurchaseItems(String itemsJson) {
        try {
            List<Map<String, Object>> items = objectMapper.readValue(itemsJson, LIST_OF_MAPS);
            List<PurchaseOrderItemRequest> requests = new ArrayList<>();
            for (Map<String, Object> item : items) {
                PurchaseOrderItemRequest request = new PurchaseOrderItemRequest();
                request.setProductVariantId(UUID.fromString(String.valueOf(item.get("productVariantId"))));
                request.setQuantity(Integer.parseInt(String.valueOf(item.get("quantity"))));
                request.setUnitPrice(new BigDecimal(String.valueOf(item.get("unitPrice"))));
                requests.add(request);
            }
            if (requests.isEmpty()) {
                throw new BadRequestException("itemsJson cannot be empty");
            }
            return requests;
        } catch (IOException exception) {
            throw new BadRequestException("Invalid purchase order itemsJson payload", exception);
        }
    }

    private List<SalesOrderItemRequest> parseSalesItems(String itemsJson) {
        try {
            List<Map<String, Object>> items = objectMapper.readValue(itemsJson, LIST_OF_MAPS);
            List<SalesOrderItemRequest> requests = new ArrayList<>();
            for (Map<String, Object> item : items) {
                SalesOrderItemRequest request = new SalesOrderItemRequest();
                request.setProductVariantId(UUID.fromString(String.valueOf(item.get("productVariantId"))));
                request.setQuantity(new BigDecimal(String.valueOf(item.get("quantity"))));
                request.setUnitPrice(new BigDecimal(String.valueOf(item.get("unitPrice"))));
                requests.add(request);
            }
            if (requests.isEmpty()) {
                throw new BadRequestException("itemsJson cannot be empty");
            }
            return requests;
        } catch (IOException exception) {
            throw new BadRequestException("Invalid sales order itemsJson payload", exception);
        }
    }

    private List<String> requiredHeaders(DataExchangeDataset dataset) {
        return switch (dataset) {
            case PRODUCTS -> List.of("sku", "barcode", "price", "templateId");
            case SUPPLIERS -> List.of("name", "contactName", "email", "phoneNumber", "address", "paymentTerms");
            case STOCKS -> List.of("productVariantId", "warehouseId", "storageLocationId", "batchId", "quantity", "movementType", "unitCost", "reason", "referenceId");
            case PURCHASE_ORDERS -> List.of("supplierId", "expectedDeliveryDate", "currency", "notes", "itemsJson");
            case SALES_ORDERS -> List.of("customerId", "warehouseId", "expectedDeliveryDate", "priority", "currency", "notes", "itemsJson");
        };
    }

    private List<CSVRecord> parseCsv(byte[] content) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            return parser.getRecords();
        } catch (IOException exception) {
            throw new BadRequestException("Failed to parse CSV file", exception);
        }
    }

    private byte[] readContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Import file is required");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Failed to read import file", exception);
        }
    }

    private void require(CSVRecord record, String field) {
        if (!record.isMapped(field) || record.get(field) == null || record.get(field).isBlank()) {
            throw new BadRequestException("Missing required value for " + field);
        }
    }

    private UUID parseUuid(CSVRecord record, String field) {
        require(record, field);
        return UUID.fromString(record.get(field));
    }

    private UUID optionalUuid(CSVRecord record, String field) {
        if (!record.isMapped(field) || record.get(field).isBlank()) {
            return null;
        }
        return UUID.fromString(record.get(field));
    }

    private BigDecimal parseDecimal(CSVRecord record, String field) {
        require(record, field);
        return new BigDecimal(record.get(field));
    }

    private BigDecimal optionalDecimal(CSVRecord record, String field) {
        if (!record.isMapped(field) || record.get(field).isBlank()) {
            return null;
        }
        return new BigDecimal(record.get(field));
    }

    private LocalDate optionalDate(CSVRecord record, String field) {
        if (!record.isMapped(field) || record.get(field).isBlank()) {
            return null;
        }
        return LocalDate.parse(record.get(field));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String writeErrors(List<String> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private DataImportHistory getHistoryEntity(UUID id) {
        return dataImportHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DataImportHistory", "id", id));
    }

    private DataImportHistoryDto mapHistory(DataImportHistory history) {
        DataImportHistoryDto dto = new DataImportHistoryDto();
        dto.setId(history.getId());
        dto.setDataset(history.getDataset());
        dto.setFileName(history.getFileName());
        dto.setStatus(history.getStatus());
        dto.setRequestedAt(history.getRequestedAt());
        dto.setStartedAt(history.getStartedAt());
        dto.setCompletedAt(history.getCompletedAt());
        dto.setTotalRecords(history.getTotalRecords());
        dto.setProcessedRecords(history.getProcessedRecords());
        dto.setSuccessfulRecords(history.getSuccessfulRecords());
        dto.setFailedRecords(history.getFailedRecords());
        dto.setValidationErrors(history.getValidationErrors());
        dto.setSummaryMessage(history.getSummaryMessage());
        dto.setCreatedBy(history.getCreatedBy());
        return dto;
    }
}