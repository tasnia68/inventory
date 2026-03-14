package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.DashboardWidgetType;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.PurchaseOrderItem;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.common.entity.ReportCategory;
import com.inventory.system.common.entity.ReportConfiguration;
import com.inventory.system.common.entity.ReportExecutionHistory;
import com.inventory.system.common.entity.ReportExecutionStatus;
import com.inventory.system.common.entity.ReportOutputFormat;
import com.inventory.system.common.entity.ReportType;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockAlertStatus;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockStatus;
import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.entity.WebhookEventType;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AgingAnalysisReportDto;
import com.inventory.system.payload.CurrentStockReportDto;
import com.inventory.system.payload.DashboardSummaryDto;
import com.inventory.system.payload.DashboardWidgetDto;
import com.inventory.system.payload.DataExchangeDataset;
import com.inventory.system.payload.DataExchangeTemplateDto;
import com.inventory.system.payload.GenerateReportRequest;
import com.inventory.system.payload.GeneratedReportDto;
import com.inventory.system.payload.InventoryValuationReportDto;
import com.inventory.system.payload.PurchaseOrderReportDto;
import com.inventory.system.payload.ReportFileDto;
import com.inventory.system.payload.ReportConfigurationDto;
import com.inventory.system.payload.ReportExecutionHistoryDto;
import com.inventory.system.payload.SalesOrderReportDto;
import com.inventory.system.payload.StockAlertDto;
import com.inventory.system.payload.StockMovementReportDto;
import com.inventory.system.payload.SupplierPerformanceReportDto;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.PurchaseOrderRepository;
import com.inventory.system.repository.ReportConfigurationRepository;
import com.inventory.system.repository.ReportExecutionHistoryRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.SupplierRepository;
import com.inventory.system.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ReportConfigurationRepository reportConfigurationRepository;
    private final ReportExecutionHistoryRepository reportExecutionHistoryRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryValuationService inventoryValuationService;
    private final ReplenishmentService replenishmentService;
    private final StockReservationService stockReservationService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final WebhookService webhookService;

    @Override
    @Transactional(readOnly = true)
    public List<ReportConfigurationDto> getConfigurations(ReportCategory category, Boolean active) {
        return reportConfigurationRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (category != null) {
                        predicates.add(cb.equal(root.get("category"), category));
                    }
                    if (active != null) {
                        predicates.add(cb.equal(root.get("active"), active));
                    }
                    return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
                }).stream()
                .sorted(Comparator.comparing(ReportConfiguration::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapConfiguration)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportConfigurationDto getConfiguration(UUID id) {
        return mapConfiguration(getConfigurationEntity(id));
    }

    @Override
    @Transactional
    public ReportConfigurationDto createConfiguration(ReportConfigurationDto request) {
        validateConfigurationRequest(request, null);
        ReportConfiguration configuration = new ReportConfiguration();
        applyConfiguration(configuration, request);
        return mapConfiguration(reportConfigurationRepository.save(configuration));
    }

    @Override
    @Transactional
    public ReportConfigurationDto updateConfiguration(UUID id, ReportConfigurationDto request) {
        ReportConfiguration configuration = getConfigurationEntity(id);
        validateConfigurationRequest(request, configuration);
        applyConfiguration(configuration, request);
        return mapConfiguration(reportConfigurationRepository.save(configuration));
    }

    @Override
    @Transactional
    public void deleteConfiguration(UUID id) {
        reportConfigurationRepository.delete(getConfigurationEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExecutionHistoryDto> getExecutionHistory() {
        return reportExecutionHistoryRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::mapExecution)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentStockReportDto> getCurrentStockReport(UUID warehouseId, UUID productVariantId) {
        Map<String, List<Stock>> groupedStocks = findStocks(warehouseId, productVariantId).stream()
            .collect(Collectors.groupingBy(stock -> stock.getWarehouse().getId() + ":" + stock.getProductVariant().getId() + ":" + stock.getStatus()));

        List<CurrentStockReportDto> results = new ArrayList<>();
        for (List<Stock> grouped : groupedStocks.values()) {
            if (grouped.isEmpty()) {
                continue;
            }
            Stock representative = grouped.get(0);
            BigDecimal onHand = grouped.stream()
                    .map(Stock::getQuantity)
                    .reduce(ZERO, BigDecimal::add);
            if (onHand.compareTo(ZERO) == 0) {
                continue;
            }

            CurrentStockReportDto dto = new CurrentStockReportDto();
            dto.setProductVariantId(representative.getProductVariant().getId());
            dto.setProductName(representative.getProductVariant().getTemplate().getName());
            dto.setSku(representative.getProductVariant().getSku());
            dto.setWarehouseId(representative.getWarehouse().getId());
            dto.setWarehouseName(representative.getWarehouse().getName());
                dto.setStockStatus(representative.getStatus());
            dto.setOnHandQuantity(onHand);
                dto.setAvailableQuantity(representative.getStatus() == StockStatus.AVAILABLE
                    ? stockReservationService.getAvailableToPromise(representative.getProductVariant().getId(), representative.getWarehouse().getId())
                    : ZERO);

            BigDecimal unitCost = inventoryValuationService.getCurrentValuation(
                    representative.getProductVariant().getId(),
                    representative.getWarehouse().getId());
            dto.setUnitCost(unitCost);
            dto.setTotalValue(onHand.multiply(unitCost));
            dto.setCurrency(resolveCurrency(representative.getWarehouse().getId()));
            results.add(dto);
        }

        results.sort(Comparator.comparing(CurrentStockReportDto::getWarehouseName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CurrentStockReportDto::getSku, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementReportDto> getStockMovementReport(UUID warehouseId, UUID productVariantId, LocalDate fromDate, LocalDate toDate) {
        return stockMovementRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (warehouseId != null) {
                        predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
                    }
                    if (productVariantId != null) {
                        predicates.add(cb.equal(root.get("productVariant").get("id"), productVariantId));
                    }
                    predicates.add(cb.between(root.get("createdAt"), resolveFromDateTime(fromDate), resolveToDateTime(toDate)));
                    query.orderBy(cb.desc(root.get("createdAt")));
                    return cb.and(predicates.toArray(new Predicate[0]));
                }).stream()
                .map(this::mapStockMovement)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingAnalysisReportDto> getAgingAnalysisReport(UUID warehouseId, Integer slowMovingThresholdDays) {
        int threshold = slowMovingThresholdDays == null || slowMovingThresholdDays <= 0 ? 30 : slowMovingThresholdDays;
        List<CurrentStockReportDto> currentStock = getCurrentStockReport(warehouseId, null);
        List<StockMovement> movements = stockMovementRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        });

        List<AgingAnalysisReportDto> results = new ArrayList<>();
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        for (CurrentStockReportDto stock : currentStock) {
            List<StockMovement> relevantMovements = movements.stream()
                    .filter(movement -> movement.getProductVariant().getId().equals(stock.getProductVariantId()))
                    .filter(movement -> movement.getWarehouse().getId().equals(stock.getWarehouseId()))
                    .toList();

            LocalDateTime lastMovementAt = relevantMovements.stream()
                    .map(StockMovement::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            BigDecimal outboundLast30Days = relevantMovements.stream()
                    .filter(movement -> movement.getCreatedAt() != null && !movement.getCreatedAt().isBefore(last30Days))
                    .filter(movement -> movement.getType() == StockMovement.StockMovementType.OUT
                            || movement.getType() == StockMovement.StockMovementType.TRANSFER_OUT)
                    .map(StockMovement::getQuantity)
                    .reduce(ZERO, BigDecimal::add);

            long daysSinceLastMovement = lastMovementAt == null
                    ? threshold + 1L
                    : Duration.between(lastMovementAt, LocalDateTime.now()).toDays();

            AgingAnalysisReportDto dto = new AgingAnalysisReportDto();
            dto.setProductVariantId(stock.getProductVariantId());
            dto.setProductName(stock.getProductName());
            dto.setSku(stock.getSku());
            dto.setWarehouseId(stock.getWarehouseId());
            dto.setWarehouseName(stock.getWarehouseName());
            dto.setOnHandQuantity(stock.getOnHandQuantity());
            dto.setOutboundQuantityLast30Days(outboundLast30Days);
            dto.setLastMovementAt(lastMovementAt);
            dto.setDaysSinceLastMovement(daysSinceLastMovement);
            dto.setMovementClass(daysSinceLastMovement > threshold ? "SLOW_MOVING" : "FAST_MOVING");
            dto.setTotalValue(stock.getTotalValue());
            results.add(dto);
        }

        results.sort(Comparator.comparingLong(AgingAnalysisReportDto::getDaysSinceLastMovement).reversed());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderReportDto> getPurchaseOrderReport(UUID supplierId, PurchaseOrderStatus status, LocalDate fromDate, LocalDate toDate) {
        return purchaseOrderRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (supplierId != null) {
                        predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
                    }
                    if (status != null) {
                        predicates.add(cb.equal(root.get("status"), status));
                    }
                    predicates.add(cb.between(root.get("orderDate"), resolveFromDateTime(fromDate), resolveToDateTime(toDate)));
                    query.orderBy(cb.desc(root.get("orderDate")));
                    return cb.and(predicates.toArray(new Predicate[0]));
                }).stream()
                .map(this::mapPurchaseOrder)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderReportDto> getSalesOrderReport(UUID customerId, UUID warehouseId, SalesOrderStatus status, LocalDate fromDate, LocalDate toDate) {
        return salesOrderRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (customerId != null) {
                        predicates.add(cb.equal(root.get("customer").get("id"), customerId));
                    }
                    if (warehouseId != null) {
                        predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
                    }
                    if (status != null) {
                        predicates.add(cb.equal(root.get("status"), status));
                    }
                    predicates.add(cb.between(root.get("orderDate"), resolveFromDateTime(fromDate), resolveToDateTime(toDate)));
                    query.orderBy(cb.desc(root.get("orderDate")));
                    return cb.and(predicates.toArray(new Predicate[0]));
                }).stream()
                .map(this::mapSalesOrder)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierPerformanceReportDto> getSupplierPerformanceReport(LocalDate fromDate, LocalDate toDate) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll((root, query, cb) -> cb.between(
                root.get("orderDate"), resolveFromDateTime(fromDate), resolveToDateTime(toDate)));
        List<GoodsReceiptNote> receipts = goodsReceiptNoteRepository.findAll((root, query, cb) -> cb.between(
                root.get("receivedDate"), resolveFromDateTime(fromDate), resolveToDateTime(toDate)));

        Map<UUID, SupplierPerformanceReportDto> report = new LinkedHashMap<>();

        for (PurchaseOrder order : orders) {
            Supplier supplier = order.getSupplier();
            SupplierPerformanceReportDto dto = report.computeIfAbsent(supplier.getId(), ignored -> {
                SupplierPerformanceReportDto created = new SupplierPerformanceReportDto();
                created.setSupplierId(supplier.getId());
                created.setSupplierName(supplier.getName());
                created.setSupplierRating(supplier.getRating());
                created.setTotalSpend(ZERO);
                created.setFulfillmentRate(ZERO);
                created.setOnTimeDeliveryRate(ZERO);
                created.setAverageLeadTimeDays(ZERO);
                return created;
            });

            dto.setPurchaseOrderCount(dto.getPurchaseOrderCount() + 1);
            if (order.getStatus() == PurchaseOrderStatus.COMPLETED || order.getStatus() == PurchaseOrderStatus.CLOSED) {
                dto.setCompletedOrderCount(dto.getCompletedOrderCount() + 1);
            }
            dto.setTotalSpend(dto.getTotalSpend().add(nullSafe(order.getTotalAmount())));
        }

        for (Map.Entry<UUID, SupplierPerformanceReportDto> entry : report.entrySet()) {
            UUID supplierId = entry.getKey();
            SupplierPerformanceReportDto dto = entry.getValue();

            List<PurchaseOrder> supplierOrders = orders.stream()
                    .filter(order -> order.getSupplier().getId().equals(supplierId))
                    .toList();
            List<GoodsReceiptNote> supplierReceipts = receipts.stream()
                    .filter(receipt -> receipt.getSupplier().getId().equals(supplierId))
                    .toList();

            BigDecimal orderedQty = supplierOrders.stream()
                    .flatMap(order -> order.getItems().stream())
                    .map(PurchaseOrderItem::getQuantity)
                    .map(BigDecimal::valueOf)
                    .reduce(ZERO, BigDecimal::add);
            BigDecimal receivedQty = supplierOrders.stream()
                    .flatMap(order -> order.getItems().stream())
                    .map(PurchaseOrderItem::getReceivedQuantity)
                    .map(BigDecimal::valueOf)
                    .reduce(ZERO, BigDecimal::add);

            long onTimeDeliveries = supplierReceipts.stream()
                    .filter(receipt -> receipt.getPurchaseOrder().getExpectedDeliveryDate() != null)
                    .filter(receipt -> !receipt.getReceivedDate().toLocalDate().isAfter(receipt.getPurchaseOrder().getExpectedDeliveryDate()))
                    .count();

            BigDecimal averageLeadTime = average(supplierReceipts.stream()
                    .map(receipt -> BigDecimal.valueOf(Duration.between(
                            receipt.getPurchaseOrder().getOrderDate(),
                            receipt.getReceivedDate()).toDays()))
                    .toList());

            dto.setFulfillmentRate(ratePercentage(receivedQty, orderedQty));
            dto.setOnTimeDeliveryRate(ratePercentage(BigDecimal.valueOf(onTimeDeliveries), BigDecimal.valueOf(Math.max(supplierReceipts.size(), 1))));
            dto.setAverageLeadTimeDays(scale(averageLeadTime, 2));
        }

        for (Supplier supplier : supplierRepository.findAll()) {
            report.computeIfAbsent(supplier.getId(), ignored -> {
                SupplierPerformanceReportDto dto = new SupplierPerformanceReportDto();
                dto.setSupplierId(supplier.getId());
                dto.setSupplierName(supplier.getName());
                dto.setSupplierRating(supplier.getRating());
                dto.setTotalSpend(ZERO);
                dto.setFulfillmentRate(ZERO);
                dto.setOnTimeDeliveryRate(ZERO);
                dto.setAverageLeadTimeDays(ZERO);
                return dto;
            });
        }

        return report.values().stream()
                .sorted(Comparator.comparing(SupplierPerformanceReportDto::getTotalSpend).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        List<CurrentStockReportDto> currentStock = getCurrentStockReport(warehouseId, null);
        List<InventoryValuationReportDto> valuation = inventoryValuationService.getValuationReport(warehouseId);
        List<PurchaseOrderReportDto> purchaseOrders = getPurchaseOrderReport(null, null, fromDate, toDate);
        List<SalesOrderReportDto> salesOrders = getSalesOrderReport(null, warehouseId, null, fromDate, toDate);
        List<StockAlertDto> alerts = getStockAlerts(warehouseId);

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setWarehouseId(warehouseId);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setTotalOnHandQuantity(currentStock.stream().map(CurrentStockReportDto::getOnHandQuantity).reduce(ZERO, BigDecimal::add));
        summary.setTotalAvailableQuantity(currentStock.stream().map(CurrentStockReportDto::getAvailableQuantity).reduce(ZERO, BigDecimal::add));
        summary.setTotalInventoryValue(valuation.stream().map(InventoryValuationReportDto::getTotalValue).reduce(ZERO, BigDecimal::add));
        summary.setInventoryTurnover(calculateInventoryTurnover(warehouseId, fromDate, toDate, summary.getTotalOnHandQuantity()));
        summary.setOpenPurchaseOrders(purchaseOrders.stream().filter(dto -> isOpenPurchaseOrder(dto.getStatus())).count());
        summary.setOpenSalesOrders(salesOrders.stream().filter(dto -> isOpenSalesOrder(dto.getStatus())).count());

        BigDecimal orderedQty = salesOrders.stream().map(SalesOrderReportDto::getOrderedQuantity).reduce(ZERO, BigDecimal::add);
        BigDecimal shippedQty = salesOrders.stream().map(SalesOrderReportDto::getShippedQuantity).reduce(ZERO, BigDecimal::add);
        summary.setOrderFulfillmentRate(ratePercentage(shippedQty, orderedQty));
        summary.setStockOutIncidents(alerts.stream().filter(alert -> alert.getStatus() == StockAlertStatus.BELOW_MIN).count());
        summary.setActiveStockAlerts((long) alerts.size());
        summary.setAverageWarehouseUtilization(calculateWarehouseUtilization(warehouseId));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardWidgetDto> getDashboardWidgets(UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        List<ReportConfiguration> configs = reportConfigurationRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("category"), ReportCategory.DASHBOARD),
                cb.equal(root.get("active"), true)));

        List<DashboardWidgetDto> widgets = new ArrayList<>();
        int index = 1;

        if (configs.isEmpty()) {
            widgets.add(buildDefaultWidget("Inventory Overview", ReportType.DASHBOARD_SUMMARY, DashboardWidgetType.KPI_CARD,
                    toMap(getDashboardSummary(warehouseId, fromDate, toDate)), index++));
            widgets.add(buildDefaultWidget("Critical Stock Alerts", ReportType.STOCK_ALERTS, DashboardWidgetType.ALERT_FEED,
                    Map.of("rows", getStockAlerts(warehouseId).stream().limit(10).map(this::toMap).toList()), index++));
            widgets.add(buildDefaultWidget("Top Suppliers", ReportType.SUPPLIER_PERFORMANCE, DashboardWidgetType.TABLE,
                    Map.of("rows", getSupplierPerformanceReport(fromDate, toDate).stream().limit(5).map(this::toMap).toList()), index++));
            widgets.add(buildDefaultWidget("Recent Sales Orders", ReportType.SALES_ORDER, DashboardWidgetType.TABLE,
                    Map.of("rows", getSalesOrderReport(null, warehouseId, null, fromDate, toDate).stream().limit(5).map(this::toMap).toList()), index));
            return widgets;
        }

        for (ReportConfiguration config : configs) {
            DashboardWidgetDto widget = new DashboardWidgetDto();
            widget.setConfigurationId(config.getId());
            widget.setTitle(config.getName());
            widget.setWidgetType(config.getWidgetType() != null ? config.getWidgetType() : DashboardWidgetType.KPI_CARD);
            widget.setReportType(config.getReportType());
            widget.setDisplayOrder(index++);
            widget.setData(buildWidgetData(config.getReportType(), warehouseId, fromDate, toDate));
            widgets.add(widget);
        }
        return widgets;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAlertDto> getStockAlerts(UUID warehouseId) {
        if (warehouseId != null) {
            return replenishmentService.getStockAlerts(warehouseId);
        }

        List<StockAlertDto> alerts = new ArrayList<>();
        for (Warehouse warehouse : warehouseRepository.findAll()) {
            alerts.addAll(replenishmentService.getStockAlerts(warehouse.getId()));
        }
        alerts.sort(Comparator.comparing(StockAlertDto::getWarehouseName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StockAlertDto::getSku, String.CASE_INSENSITIVE_ORDER));
        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedReportDto generateReport(GenerateReportRequest request) {
        GenerateReportRequest preparedRequest = prepareRequest(request);
        ReportConfiguration configuration = resolveConfiguration(preparedRequest);
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            GeneratedReportDto report = buildReport(preparedRequest);
            recordExecution(configuration, report.getReportName(), preparedRequest, report.getRows().size(), null, startedAt, ReportExecutionStatus.SUCCESS);
            publishReportEvent(WebhookEventType.REPORT_GENERATED, report, preparedRequest);
            return report;
        } catch (RuntimeException exception) {
            recordExecution(configuration, reportName(preparedRequest.getReportType()), preparedRequest, 0, exception.getMessage(), startedAt, ReportExecutionStatus.FAILED);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String exportReport(GenerateReportRequest request) {
        ReportFileDto file = exportReportFile(request);
        if (request != null && (request.getFormat() == ReportOutputFormat.PDF || request.getFormat() == ReportOutputFormat.XLSX)) {
            throw new BadRequestException("Binary report formats must be downloaded through the file export endpoint");
        }
        return new String(file.getContent(), StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportFileDto exportReportFile(GenerateReportRequest request) {
        GenerateReportRequest preparedRequest = prepareRequest(request);
        ReportConfiguration configuration = resolveConfiguration(preparedRequest);
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            GeneratedReportDto report = buildReport(preparedRequest);
            ReportFileDto file = buildExportFile(report, preparedRequest.getFormat());
            recordExecution(configuration, report.getReportName(), preparedRequest, report.getRows().size(), null, startedAt, ReportExecutionStatus.SUCCESS);
            publishReportEvent(WebhookEventType.REPORT_GENERATED, report, preparedRequest);
            return file;
        } catch (RuntimeException exception) {
            recordExecution(configuration, reportName(preparedRequest.getReportType()), preparedRequest, 0, exception.getMessage(), startedAt, ReportExecutionStatus.FAILED);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DataExchangeTemplateDto getImportTemplate(DataExchangeDataset dataset) {
        DataExchangeTemplateDto template = new DataExchangeTemplateDto();
        template.setDataset(dataset);
        template.setFileName(dataset.name().toLowerCase() + "-template.csv");
        template.setContentType("text/csv");
        template.setTemplateContent(switch (dataset) {
            case PRODUCTS -> "sku,barcode,price,templateId\n";
            case STOCKS -> "productVariantId,warehouseId,storageLocationId,batchId,quantity,movementType,unitCost,reason,referenceId\n";
            case PURCHASE_ORDERS -> "supplierId,expectedDeliveryDate,currency,notes,itemsJson\n";
            case SALES_ORDERS -> "customerId,warehouseId,expectedDeliveryDate,priority,currency,notes,itemsJson\n";
            case SUPPLIERS -> "name,contactName,email,phoneNumber,address,paymentTerms\n";
        });
        return template;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportDataset(DataExchangeDataset dataset) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            String output = switch (dataset) {
                case PRODUCTS -> productService.exportProductsToCsv();
                case STOCKS -> toCsv(buildStockReportForExport());
                case PURCHASE_ORDERS -> toCsv(buildPurchaseOrderReportForExport());
                case SALES_ORDERS -> toCsv(buildSalesOrderReportForExport());
                case SUPPLIERS -> toCsv(buildSupplierPerformanceReportForExport());
            };
            recordExecution(null, dataset.name() + " Export", exportRequest(dataset), countRows(output), null, startedAt, ReportExecutionStatus.SUCCESS);
            return output;
        } catch (RuntimeException exception) {
            recordExecution(null, dataset.name() + " Export", exportRequest(dataset), 0, exception.getMessage(), startedAt, ReportExecutionStatus.FAILED);
            throw exception;
        }
    }

    private List<Stock> findStocks(UUID warehouseId, UUID productVariantId) {
        return stockRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (productVariantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), productVariantId));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    private ReportConfiguration getConfigurationEntity(UUID id) {
        return reportConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportConfiguration", "id", id));
    }

    private void validateConfigurationRequest(ReportConfigurationDto request, ReportConfiguration existing) {
        if (request == null) {
            throw new BadRequestException("Report configuration request is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Report configuration name is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Report configuration code is required");
        }
        if (request.getReportType() == null) {
            throw new BadRequestException("Report type is required");
        }
        boolean codeChanged = existing == null || !existing.getCode().equals(request.getCode());
        if (codeChanged && reportConfigurationRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Report configuration code already exists");
        }
    }

    private void applyConfiguration(ReportConfiguration configuration, ReportConfigurationDto request) {
        configuration.setName(request.getName());
        configuration.setCode(request.getCode());
        configuration.setDescription(request.getDescription());
        configuration.setCategory(request.getCategory() != null ? request.getCategory() : ReportCategory.CUSTOM);
        configuration.setReportType(request.getReportType());
        configuration.setWidgetType(request.getWidgetType());
        configuration.setConfigurationJson(request.getConfigurationJson());
        configuration.setFilterPresetJson(request.getFilterPresetJson());
        configuration.setColumnsJson(request.getColumnsJson());
        configuration.setScheduleCron(request.getScheduleCron());
        configuration.setSharedWith(request.getSharedWith());
        configuration.setExportFormats(request.getExportFormats());
        configuration.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
    }

    private ReportConfigurationDto mapConfiguration(ReportConfiguration configuration) {
        ReportConfigurationDto dto = new ReportConfigurationDto();
        dto.setId(configuration.getId());
        dto.setName(configuration.getName());
        dto.setCode(configuration.getCode());
        dto.setDescription(configuration.getDescription());
        dto.setCategory(configuration.getCategory());
        dto.setReportType(configuration.getReportType());
        dto.setWidgetType(configuration.getWidgetType());
        dto.setConfigurationJson(configuration.getConfigurationJson());
        dto.setFilterPresetJson(configuration.getFilterPresetJson());
        dto.setColumnsJson(configuration.getColumnsJson());
        dto.setScheduleCron(configuration.getScheduleCron());
        dto.setSharedWith(configuration.getSharedWith());
        dto.setExportFormats(configuration.getExportFormats());
        dto.setActive(configuration.getActive());
        dto.setCreatedAt(configuration.getCreatedAt());
        dto.setUpdatedAt(configuration.getUpdatedAt());
        return dto;
    }

    private ReportExecutionHistoryDto mapExecution(ReportExecutionHistory execution) {
        ReportExecutionHistoryDto dto = new ReportExecutionHistoryDto();
        dto.setId(execution.getId());
        dto.setReportConfigurationId(execution.getReportConfiguration() != null ? execution.getReportConfiguration().getId() : null);
        dto.setReportName(execution.getReportName());
        dto.setReportType(execution.getReportType());
        dto.setOutputFormat(execution.getOutputFormat());
        dto.setStatus(execution.getStatus());
        dto.setRequestedAt(execution.getRequestedAt());
        dto.setCompletedAt(execution.getCompletedAt());
        dto.setRowCount(execution.getRowCount());
        dto.setFiltersJson(execution.getFiltersJson());
        dto.setErrorMessage(execution.getErrorMessage());
        dto.setCreatedBy(execution.getCreatedBy());
        return dto;
    }

    private StockMovementReportDto mapStockMovement(StockMovement movement) {
        StockMovementReportDto dto = new StockMovementReportDto();
        dto.setMovementId(movement.getId());
        dto.setMovementDate(movement.getCreatedAt());
        dto.setProductVariantId(movement.getProductVariant().getId());
        dto.setProductName(movement.getProductVariant().getTemplate().getName());
        dto.setSku(movement.getProductVariant().getSku());
        dto.setWarehouseId(movement.getWarehouse().getId());
        dto.setWarehouseName(movement.getWarehouse().getName());
        dto.setQuantity(movement.getQuantity());
        dto.setUnitCost(movement.getUnitCost());
        dto.setTotalCost(movement.getTotalCost());
        dto.setType(movement.getType());
        dto.setReason(movement.getReason());
        dto.setReferenceId(movement.getReferenceId());
        return dto;
    }

    private PurchaseOrderReportDto mapPurchaseOrder(PurchaseOrder order) {
        BigDecimal orderedQuantity = order.getItems().stream()
                .map(PurchaseOrderItem::getQuantity)
                .map(BigDecimal::valueOf)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal receivedQuantity = order.getItems().stream()
                .map(PurchaseOrderItem::getReceivedQuantity)
                .map(BigDecimal::valueOf)
                .reduce(ZERO, BigDecimal::add);

        PurchaseOrderReportDto dto = new PurchaseOrderReportDto();
        dto.setPurchaseOrderId(order.getId());
        dto.setPoNumber(order.getPoNumber());
        dto.setSupplierId(order.getSupplier().getId());
        dto.setSupplierName(order.getSupplier().getName());
        dto.setOrderDate(order.getOrderDate());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        dto.setStatus(order.getStatus());
        dto.setItemCount(order.getItems().size());
        dto.setOrderedQuantity(orderedQuantity);
        dto.setReceivedQuantity(receivedQuantity);
        dto.setCompletionRate(ratePercentage(receivedQuantity, orderedQuantity));
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        return dto;
    }

    private SalesOrderReportDto mapSalesOrder(SalesOrder order) {
        BigDecimal orderedQuantity = order.getItems().stream()
                .map(SalesOrderItem::getQuantity)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal shippedQuantity = order.getItems().stream()
                .map(SalesOrderItem::getShippedQuantity)
                .reduce(ZERO, BigDecimal::add);

        SalesOrderReportDto dto = new SalesOrderReportDto();
        dto.setSalesOrderId(order.getId());
        dto.setSoNumber(order.getSoNumber());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getName());
        dto.setWarehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null);
        dto.setWarehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null);
        dto.setOrderDate(order.getOrderDate());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        dto.setStatus(order.getStatus());
        dto.setItemCount(order.getItems().size());
        dto.setOrderedQuantity(orderedQuantity);
        dto.setShippedQuantity(shippedQuantity);
        dto.setFulfillmentRate(ratePercentage(shippedQuantity, orderedQuantity));
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        return dto;
    }

    private DashboardWidgetDto buildDefaultWidget(String title, ReportType reportType, DashboardWidgetType widgetType,
                                                  Map<String, Object> data, int displayOrder) {
        DashboardWidgetDto widget = new DashboardWidgetDto();
        widget.setTitle(title);
        widget.setReportType(reportType);
        widget.setWidgetType(widgetType);
        widget.setDisplayOrder(displayOrder);
        widget.setData(data);
        return widget;
    }

    private Map<String, Object> buildWidgetData(ReportType reportType, UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        return switch (reportType) {
            case DASHBOARD_SUMMARY -> toMap(getDashboardSummary(warehouseId, fromDate, toDate));
            case CURRENT_STOCK -> Map.of("rows", getCurrentStockReport(warehouseId, null).stream().limit(10).map(this::toMap).toList());
            case STOCK_ALERTS -> Map.of("rows", getStockAlerts(warehouseId).stream().limit(10).map(this::toMap).toList());
            case PURCHASE_ORDER -> Map.of("rows", getPurchaseOrderReport(null, null, fromDate, toDate).stream().limit(10).map(this::toMap).toList());
            case SALES_ORDER -> Map.of("rows", getSalesOrderReport(null, warehouseId, null, fromDate, toDate).stream().limit(10).map(this::toMap).toList());
            case SUPPLIER_PERFORMANCE -> Map.of("rows", getSupplierPerformanceReport(fromDate, toDate).stream().limit(10).map(this::toMap).toList());
            case AGING_ANALYSIS -> Map.of("rows", getAgingAnalysisReport(warehouseId, 30).stream().limit(10).map(this::toMap).toList());
            case STOCK_MOVEMENT -> Map.of("rows", getStockMovementReport(warehouseId, null, fromDate, toDate).stream().limit(10).map(this::toMap).toList());
            case STOCK_VALUATION -> Map.of("rows", inventoryValuationService.getValuationReport(warehouseId).stream().limit(10).map(this::toMap).toList());
            case DATA_EXPORT -> Map.of("supportedDatasets", DataExchangeDataset.values());
        };
    }

    private GenerateReportRequest prepareRequest(GenerateReportRequest request) {
        GenerateReportRequest prepared = copyRequest(request);
        ReportConfiguration configuration = resolveConfiguration(prepared);
        if (configuration != null) {
            if (prepared.getReportType() == null) {
                prepared.setReportType(configuration.getReportType());
            }
            applyFilterPreset(prepared, configuration.getFilterPresetJson());
            applyConfigurationOptions(prepared, configuration.getConfigurationJson(), configuration.getColumnsJson());
        }
        if (prepared.getReportType() == null) {
            throw new BadRequestException("Report type is required");
        }
        if (prepared.getFormat() == null) {
            prepared.setFormat(ReportOutputFormat.CSV);
        }
        return prepared;
    }

    private GenerateReportRequest copyRequest(GenerateReportRequest request) {
        GenerateReportRequest copy = new GenerateReportRequest();
        if (request == null) {
            return copy;
        }
        copy.setConfigurationId(request.getConfigurationId());
        copy.setReportType(request.getReportType());
        copy.setWarehouseId(request.getWarehouseId());
        copy.setProductVariantId(request.getProductVariantId());
        copy.setSupplierId(request.getSupplierId());
        copy.setCustomerId(request.getCustomerId());
        copy.setFromDate(request.getFromDate());
        copy.setToDate(request.getToDate());
        copy.setSlowMovingThresholdDays(request.getSlowMovingThresholdDays());
        copy.setPurchaseOrderStatus(request.getPurchaseOrderStatus());
        copy.setSalesOrderStatus(request.getSalesOrderStatus());
        copy.setFormat(request.getFormat());
        copy.setSearch(request.getSearch());
        copy.setSortBy(request.getSortBy());
        copy.setSortDirection(request.getSortDirection());
        copy.setLimit(request.getLimit());
        copy.setColumns(request.getColumns());
        copy.setFieldFilters(request.getFieldFilters());
        return copy;
    }

    private ReportConfiguration resolveConfiguration(GenerateReportRequest request) {
        if (request == null || request.getConfigurationId() == null) {
            return null;
        }
        return getConfigurationEntity(request.getConfigurationId());
    }

    private void applyFilterPreset(GenerateReportRequest request, String filterPresetJson) {
        if (filterPresetJson == null || filterPresetJson.isBlank()) {
            return;
        }

        try {
            Map<String, Object> preset = objectMapper.readValue(filterPresetJson, MAP_TYPE);
            if (request.getWarehouseId() == null && preset.get("warehouseId") instanceof String warehouseId) {
                request.setWarehouseId(UUID.fromString(warehouseId));
            }
            if (request.getProductVariantId() == null && preset.get("productVariantId") instanceof String productVariantId) {
                request.setProductVariantId(UUID.fromString(productVariantId));
            }
            if (request.getSupplierId() == null && preset.get("supplierId") instanceof String supplierId) {
                request.setSupplierId(UUID.fromString(supplierId));
            }
            if (request.getCustomerId() == null && preset.get("customerId") instanceof String customerId) {
                request.setCustomerId(UUID.fromString(customerId));
            }
            if (request.getFromDate() == null && preset.get("fromDate") instanceof String fromDate) {
                request.setFromDate(LocalDate.parse(fromDate));
            }
            if (request.getToDate() == null && preset.get("toDate") instanceof String toDate) {
                request.setToDate(LocalDate.parse(toDate));
            }
            if (request.getSlowMovingThresholdDays() == null && preset.get("slowMovingThresholdDays") instanceof Number threshold) {
                request.setSlowMovingThresholdDays(threshold.intValue());
            }
            if (request.getPurchaseOrderStatus() == null && preset.get("purchaseOrderStatus") instanceof String purchaseOrderStatus) {
                request.setPurchaseOrderStatus(PurchaseOrderStatus.valueOf(purchaseOrderStatus));
            }
            if (request.getSalesOrderStatus() == null && preset.get("salesOrderStatus") instanceof String salesOrderStatus) {
                request.setSalesOrderStatus(SalesOrderStatus.valueOf(salesOrderStatus));
            }
            if (request.getFormat() == null && preset.get("format") instanceof String format) {
                request.setFormat(ReportOutputFormat.valueOf(format));
            }
        } catch (Exception exception) {
            throw new BadRequestException("Invalid report filter preset JSON", exception);
        }
    }

    private GeneratedReportDto buildReport(GenerateReportRequest request) {
        return switch (request.getReportType()) {
            case CURRENT_STOCK -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getCurrentStockReport(request.getWarehouseId(), request.getProductVariantId()));
            case STOCK_MOVEMENT -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getStockMovementReport(request.getWarehouseId(), request.getProductVariantId(), request.getFromDate(), request.getToDate()));
            case AGING_ANALYSIS -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getAgingAnalysisReport(request.getWarehouseId(), request.getSlowMovingThresholdDays()));
            case STOCK_VALUATION -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    inventoryValuationService.getValuationReport(request.getWarehouseId()));
            case PURCHASE_ORDER -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getPurchaseOrderReport(request.getSupplierId(), request.getPurchaseOrderStatus(), request.getFromDate(), request.getToDate()));
            case SALES_ORDER -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getSalesOrderReport(request.getCustomerId(), request.getWarehouseId(), request.getSalesOrderStatus(), request.getFromDate(), request.getToDate()));
            case SUPPLIER_PERFORMANCE -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getSupplierPerformanceReport(request.getFromDate(), request.getToDate()));
            case DASHBOARD_SUMMARY -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    Collections.singletonList(getDashboardSummary(request.getWarehouseId(), request.getFromDate(), request.getToDate())));
            case STOCK_ALERTS -> buildGeneratedReport(reportName(request.getReportType()), request.getReportType(),
                request,
                    getStockAlerts(request.getWarehouseId()));
            case DATA_EXPORT -> throw new BadRequestException("Use data exchange endpoints for bulk data export");
        };
    }

        private GeneratedReportDto buildGeneratedReport(String reportName, ReportType reportType, GenerateReportRequest request, List<?> items) {
        List<Map<String, Object>> rows = items.stream().map(this::toMap).toList();
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        rows.forEach(row -> headers.addAll(row.keySet()));

        GeneratedReportDto report = new GeneratedReportDto();
        report.setReportName(reportName);
        report.setReportType(reportType);
        report.setGeneratedAt(LocalDateTime.now());
        report.setHeaders(new ArrayList<>(headers));
        report.setRows(rows);
        report.setSummary(buildSummary(reportType, items));
        return applyDynamicQuery(report, request);
    }

    private GeneratedReportDto applyDynamicQuery(GeneratedReportDto report, GenerateReportRequest request) {
        if (request == null) {
            return report;
        }

        List<Map<String, Object>> rows = new ArrayList<>(report.getRows());

        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String needle = request.getSearch().toLowerCase();
            rows = rows.stream()
                    .filter(row -> row.values().stream()
                            .filter(Objects::nonNull)
                            .map(String::valueOf)
                            .map(String::toLowerCase)
                            .anyMatch(value -> value.contains(needle)))
                    .toList();
        }

        if (request.getFieldFilters() != null && !request.getFieldFilters().isEmpty()) {
            rows = rows.stream()
                    .filter(row -> request.getFieldFilters().entrySet().stream()
                            .allMatch(entry -> {
                                Object value = row.get(entry.getKey());
                                return value != null && String.valueOf(value).equalsIgnoreCase(entry.getValue());
                            }))
                    .toList();
        }

        if (request.getSortBy() != null && !request.getSortBy().isBlank()) {
            Comparator<Map<String, Object>> comparator = Comparator.comparing(
                    row -> String.valueOf(row.get(request.getSortBy())),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            if ("desc".equalsIgnoreCase(request.getSortDirection())) {
                comparator = comparator.reversed();
            }
            rows = rows.stream().sorted(comparator).toList();
        }

        if (request.getLimit() != null && request.getLimit() > 0 && rows.size() > request.getLimit()) {
            rows = rows.subList(0, request.getLimit());
        }

        List<String> headers = report.getHeaders();
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            List<String> selected = request.getColumns().stream()
                    .filter(headers::contains)
                    .toList();
            rows = rows.stream()
                    .map(row -> {
                        Map<String, Object> filtered = new LinkedHashMap<>();
                        for (String column : selected) {
                            filtered.put(column, row.get(column));
                        }
                        return filtered;
                    })
                    .toList();
            headers = selected;
        }

        report.setHeaders(headers);
        report.setRows(rows);
        report.getSummary().put("rowCount", rows.size());
        return report;
    }

    private void applyConfigurationOptions(GenerateReportRequest request, String configurationJson, String columnsJson) {
        if (configurationJson != null && !configurationJson.isBlank()) {
            try {
                Map<String, Object> config = objectMapper.readValue(configurationJson, MAP_TYPE);
                if (request.getSearch() == null && config.get("search") instanceof String search) {
                    request.setSearch(search);
                }
                if (request.getSortBy() == null && config.get("sortBy") instanceof String sortBy) {
                    request.setSortBy(sortBy);
                }
                if (request.getSortDirection() == null && config.get("sortDirection") instanceof String sortDirection) {
                    request.setSortDirection(sortDirection);
                }
                if (request.getLimit() == null && config.get("limit") instanceof Number limit) {
                    request.setLimit(limit.intValue());
                }
                if ((request.getFieldFilters() == null || request.getFieldFilters().isEmpty()) && config.get("fieldFilters") instanceof Map<?, ?> rawFilters) {
                    Map<String, String> filters = new LinkedHashMap<>();
                    rawFilters.forEach((key, value) -> filters.put(String.valueOf(key), String.valueOf(value)));
                    request.setFieldFilters(filters);
                }
            } catch (JsonProcessingException exception) {
                throw new BadRequestException("Invalid report configuration JSON", exception);
            }
        }

        if ((request.getColumns() == null || request.getColumns().isEmpty()) && columnsJson != null && !columnsJson.isBlank()) {
            try {
                request.setColumns(objectMapper.readValue(columnsJson, new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException exception) {
                throw new BadRequestException("Invalid report columns JSON", exception);
            }
        }
    }

    private ReportFileDto buildExportFile(GeneratedReportDto report, ReportOutputFormat format) {
        ReportOutputFormat outputFormat = format == null ? ReportOutputFormat.CSV : format;
        ReportFileDto file = new ReportFileDto();
        String baseName = slugify(report.getReportName());
        try {
            switch (outputFormat) {
                case CSV -> {
                    file.setFileName(baseName + ".csv");
                    file.setContentType("text/csv");
                    file.setContent(toCsv(report).getBytes(StandardCharsets.UTF_8));
                }
                case JSON -> {
                    file.setFileName(baseName + ".json");
                    file.setContentType("application/json");
                    file.setContent(objectMapper.writeValueAsBytes(report));
                }
                case PDF -> {
                    file.setFileName(baseName + ".pdf");
                    file.setContentType("application/pdf");
                    file.setContent(toPdf(report));
                }
                case XLSX -> {
                    file.setFileName(baseName + ".xlsx");
                    file.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    file.setContent(toXlsx(report));
                }
            }
            return file;
        } catch (IOException exception) {
            throw new BadRequestException("Failed to export report file", exception);
        }
    }

    private byte[] toPdf(GeneratedReportDto report) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, 760);
                stream.showText(report.getReportName());
                stream.newLineAtOffset(0, -20);
                stream.setFont(PDType1Font.HELVETICA, 10);
                stream.showText("Generated: " + report.getGeneratedAt());

                int lineIndex = 0;
                for (Map<String, Object> row : report.getRows()) {
                    if (lineIndex >= 28) {
                        break;
                    }
                    String line = report.getHeaders().stream()
                            .map(header -> header + "=" + truncate(String.valueOf(row.get(header)), 24))
                            .collect(Collectors.joining(" | "));
                    stream.newLineAtOffset(0, -18);
                    stream.showText(truncate(line, 140));
                    lineIndex++;
                }
                stream.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] toXlsx(GeneratedReportDto report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Report");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < report.getHeaders().size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(report.getHeaders().get(index));
            }
            int rowIndex = 1;
            for (Map<String, Object> row : report.getRows()) {
                Row sheetRow = sheet.createRow(rowIndex++);
                for (int columnIndex = 0; columnIndex < report.getHeaders().size(); columnIndex++) {
                    Object value = row.get(report.getHeaders().get(columnIndex));
                    sheetRow.createCell(columnIndex).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            for (int index = 0; index < report.getHeaders().size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void publishReportEvent(WebhookEventType eventType, GeneratedReportDto report, GenerateReportRequest request) {
        webhookService.publishEvent(eventType, Map.of(
                "reportName", report.getReportName(),
                "reportType", report.getReportType().name(),
                "generatedAt", report.getGeneratedAt().toString(),
                "rowCount", report.getRows().size(),
                "format", request.getFormat().name()));
    }

    private String slugify(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private Map<String, Object> buildSummary(ReportType reportType, List<?> items) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rowCount", items.size());
        switch (reportType) {
            case CURRENT_STOCK -> {
                List<CurrentStockReportDto> stock = castList(items, CurrentStockReportDto.class);
                summary.put("totalOnHandQuantity", stock.stream().map(CurrentStockReportDto::getOnHandQuantity).reduce(ZERO, BigDecimal::add));
                summary.put("totalInventoryValue", stock.stream().map(CurrentStockReportDto::getTotalValue).reduce(ZERO, BigDecimal::add));
            }
            case STOCK_MOVEMENT -> {
                List<StockMovementReportDto> movements = castList(items, StockMovementReportDto.class);
                summary.put("totalMovedQuantity", movements.stream().map(StockMovementReportDto::getQuantity).reduce(ZERO, BigDecimal::add));
                summary.put("totalMovementCost", movements.stream().map(dto -> nullSafe(dto.getTotalCost())).reduce(ZERO, BigDecimal::add));
            }
            case AGING_ANALYSIS -> {
                List<AgingAnalysisReportDto> aging = castList(items, AgingAnalysisReportDto.class);
                summary.put("slowMovingCount", aging.stream().filter(dto -> "SLOW_MOVING".equals(dto.getMovementClass())).count());
                summary.put("totalValueAtRisk", aging.stream().map(AgingAnalysisReportDto::getTotalValue).reduce(ZERO, BigDecimal::add));
            }
            case STOCK_VALUATION -> {
                List<InventoryValuationReportDto> valuation = castList(items, InventoryValuationReportDto.class);
                summary.put("totalInventoryValue", valuation.stream().map(InventoryValuationReportDto::getTotalValue).reduce(ZERO, BigDecimal::add));
            }
            case PURCHASE_ORDER -> {
                List<PurchaseOrderReportDto> orders = castList(items, PurchaseOrderReportDto.class);
                summary.put("totalPurchaseAmount", orders.stream().map(PurchaseOrderReportDto::getTotalAmount).reduce(ZERO, BigDecimal::add));
                summary.put("averageCompletionRate", average(orders.stream().map(PurchaseOrderReportDto::getCompletionRate).toList()));
            }
            case SALES_ORDER -> {
                List<SalesOrderReportDto> orders = castList(items, SalesOrderReportDto.class);
                summary.put("totalSalesAmount", orders.stream().map(SalesOrderReportDto::getTotalAmount).reduce(ZERO, BigDecimal::add));
                summary.put("averageFulfillmentRate", average(orders.stream().map(SalesOrderReportDto::getFulfillmentRate).toList()));
            }
            case SUPPLIER_PERFORMANCE -> {
                List<SupplierPerformanceReportDto> suppliers = castList(items, SupplierPerformanceReportDto.class);
                summary.put("totalSupplierSpend", suppliers.stream().map(SupplierPerformanceReportDto::getTotalSpend).reduce(ZERO, BigDecimal::add));
                summary.put("averageOnTimeDeliveryRate", average(suppliers.stream().map(SupplierPerformanceReportDto::getOnTimeDeliveryRate).toList()));
            }
            case DASHBOARD_SUMMARY, STOCK_ALERTS, DATA_EXPORT -> {
            }
        }
        return summary;
    }

    private <T> List<T> castList(List<?> items, Class<T> type) {
        return items.stream().map(type::cast).toList();
    }

    private void recordExecution(ReportConfiguration configuration, String reportName, GenerateReportRequest request,
                                 int rowCount, String errorMessage, LocalDateTime startedAt,
                                 ReportExecutionStatus status) {
        ReportExecutionHistory history = new ReportExecutionHistory();
        history.setReportConfiguration(configuration);
        history.setReportName(reportName);
        history.setReportType(configuration != null ? configuration.getReportType() : request.getReportType());
        history.setOutputFormat(request.getFormat() != null ? request.getFormat() : ReportOutputFormat.CSV);
        history.setStatus(status);
        history.setRequestedAt(startedAt);
        history.setCompletedAt(LocalDateTime.now());
        history.setRowCount(rowCount);
        history.setFiltersJson(writeJsonSilently(toMap(request)));
        history.setErrorMessage(errorMessage);
        reportExecutionHistoryRepository.save(history);
    }

    private GeneratedReportDto buildStockReportForExport() {
        return buildGeneratedReport("Current Stock Export", ReportType.CURRENT_STOCK, new GenerateReportRequest(), getCurrentStockReport(null, null));
    }

    private GeneratedReportDto buildPurchaseOrderReportForExport() {
        return buildGeneratedReport("Purchase Order Export", ReportType.PURCHASE_ORDER, new GenerateReportRequest(),
                getPurchaseOrderReport(null, null, LocalDate.now().minusYears(1), LocalDate.now()));
    }

    private GeneratedReportDto buildSalesOrderReportForExport() {
        return buildGeneratedReport("Sales Order Export", ReportType.SALES_ORDER, new GenerateReportRequest(),
                getSalesOrderReport(null, null, null, LocalDate.now().minusYears(1), LocalDate.now()));
    }

    private GeneratedReportDto buildSupplierPerformanceReportForExport() {
        return buildGeneratedReport("Supplier Performance Export", ReportType.SUPPLIER_PERFORMANCE, new GenerateReportRequest(),
                getSupplierPerformanceReport(LocalDate.now().minusYears(1), LocalDate.now()));
    }

    private GenerateReportRequest exportRequest(DataExchangeDataset dataset) {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportType.DATA_EXPORT);
        request.setFormat(ReportOutputFormat.CSV);
        request.setFromDate(LocalDate.now().minusYears(1));
        request.setToDate(LocalDate.now());
        request.setSupplierId(null);
        request.setCustomerId(null);
        request.setWarehouseId(null);
        request.setProductVariantId(null);
        request.setSlowMovingThresholdDays(null);
        request.setConfigurationId(null);
        request.setPurchaseOrderStatus(null);
        request.setSalesOrderStatus(null);
        return request;
    }

    private int countRows(String csv) {
        if (csv == null || csv.isBlank()) {
            return 0;
        }
        return Math.max(csv.split("\\R").length - 1, 0);
    }

    private String toCsv(GeneratedReportDto report) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", report.getHeaders())).append("\n");
        for (Map<String, Object> row : report.getRows()) {
            List<String> values = new ArrayList<>();
            for (String header : report.getHeaders()) {
                values.add(csvValue(row.get(header)));
            }
            builder.append(String.join(",", values)).append("\n");
        }
        return builder.toString();
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private String writeJsonSilently(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private LocalDateTime resolveFromDateTime(LocalDate fromDate) {
        return (fromDate == null ? LocalDate.now().minusDays(30) : fromDate).atStartOfDay();
    }

    private LocalDateTime resolveToDateTime(LocalDate toDate) {
        LocalDate date = toDate == null ? LocalDate.now() : toDate;
        return date.atTime(23, 59, 59);
    }

    private BigDecimal calculateInventoryTurnover(UUID warehouseId, LocalDate fromDate, LocalDate toDate, BigDecimal currentOnHand) {
        LocalDateTime from = resolveFromDateTime(fromDate);
        LocalDateTime to = resolveToDateTime(toDate);
        BigDecimal outbound = stockMovementRepository.findAll((root, query, cb) -> cb.and(
                        warehouseId != null ? cb.equal(root.get("warehouse").get("id"), warehouseId) : cb.conjunction(),
                        cb.or(cb.equal(root.get("type"), StockMovement.StockMovementType.OUT),
                                cb.equal(root.get("type"), StockMovement.StockMovementType.TRANSFER_OUT)),
                        cb.between(root.get("createdAt"), from, to)))
                .stream()
                .map(StockMovement::getQuantity)
                .reduce(ZERO, BigDecimal::add);
        return currentOnHand.compareTo(ZERO) <= 0 ? ZERO : scale(outbound.divide(currentOnHand, 4, RoundingMode.HALF_UP), 4);
    }

    private BigDecimal calculateWarehouseUtilization(UUID warehouseId) {
        List<Warehouse> warehouses = warehouseId == null
                ? warehouseRepository.findAll()
                : List.of(warehouseRepository.findById(warehouseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", warehouseId)));

        List<BigDecimal> utilizations = warehouses.stream()
                .filter(warehouse -> warehouse.getCapacity() != null && warehouse.getCapacity().compareTo(ZERO) > 0)
                .map(warehouse -> scale(
                        nullSafe(warehouse.getUsedCapacity())
                                .divide(warehouse.getCapacity(), 4, RoundingMode.HALF_UP)
                                .multiply(ONE_HUNDRED),
                        2))
                .toList();
        return average(utilizations);
    }

    private boolean isOpenPurchaseOrder(PurchaseOrderStatus status) {
        return status == PurchaseOrderStatus.PENDING
                || status == PurchaseOrderStatus.APPROVED
                || status == PurchaseOrderStatus.ISSUED
                || status == PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    private boolean isOpenSalesOrder(SalesOrderStatus status) {
        return status == SalesOrderStatus.DRAFT
                || status == SalesOrderStatus.PENDING_APPROVAL
                || status == SalesOrderStatus.APPROVED
                || status == SalesOrderStatus.CONFIRMED
                || status == SalesOrderStatus.BACKORDERED
                || status == SalesOrderStatus.PARTIALLY_SHIPPED;
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNullValues = values.stream().filter(Objects::nonNull).toList();
        if (nonNullValues.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = nonNullValues.stream().reduce(ZERO, BigDecimal::add);
        return scale(sum.divide(BigDecimal.valueOf(nonNullValues.size()), 4, RoundingMode.HALF_UP), 2);
    }

    private BigDecimal ratePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return scale(numerator.multiply(ONE_HUNDRED).divide(denominator, 4, RoundingMode.HALF_UP), 2);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? ZERO : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String reportName(ReportType reportType) {
        return switch (reportType) {
            case CURRENT_STOCK -> "Current Stock Report";
            case STOCK_MOVEMENT -> "Stock Movement Report";
            case AGING_ANALYSIS -> "Aging Analysis Report";
            case STOCK_VALUATION -> "Stock Valuation Report";
            case PURCHASE_ORDER -> "Purchase Order Report";
            case SALES_ORDER -> "Sales Order Report";
            case SUPPLIER_PERFORMANCE -> "Supplier Performance Report";
            case DASHBOARD_SUMMARY -> "Dashboard Summary";
            case STOCK_ALERTS -> "Stock Alerts";
            case DATA_EXPORT -> "Data Export";
        };
    }

    private String resolveCurrency(UUID warehouseId) {
        List<InventoryValuationReportDto> valuation = inventoryValuationService.getValuationReport(warehouseId);
        return valuation.isEmpty() ? "USD" : valuation.get(0).getCurrency();
    }
}