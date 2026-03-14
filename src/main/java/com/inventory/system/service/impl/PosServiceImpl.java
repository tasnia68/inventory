package com.inventory.system.service.impl;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.CategoryDto;
import com.inventory.system.payload.ClosePosShiftRequest;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.CreatePosShiftRequest;
import com.inventory.system.payload.CreatePosTerminalRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.PosBootstrapDto;
import com.inventory.system.payload.PosCatalogItemDto;
import com.inventory.system.payload.PosKpiDto;
import com.inventory.system.payload.PosSaleDto;
import com.inventory.system.payload.PosSaleItemDto;
import com.inventory.system.payload.PosSaleItemRequest;
import com.inventory.system.payload.PosShiftDto;
import com.inventory.system.payload.PosTerminalDto;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.UpdatePosTerminalStatusRequest;
import com.inventory.system.payload.WarehouseDto;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.PosSaleRepository;
import com.inventory.system.repository.PosShiftRepository;
import com.inventory.system.repository.PosTerminalRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.StockTransactionRepository;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.repository.WarehouseRepository;
import com.inventory.system.service.PosService;
import com.inventory.system.service.PricingEngineService;
import com.inventory.system.service.PricingEvaluation;
import com.inventory.system.service.PricingEvaluationLine;
import com.inventory.system.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    private static final String WALK_IN_CUSTOMER_NAME = "Walk-in Customer";

    private final PosTerminalRepository posTerminalRepository;
    private final PosShiftRepository posShiftRepository;
    private final PosSaleRepository posSaleRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final UserRepository userRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final StockService stockService;
    private final PricingEngineService pricingEngineService;

    @Override
    @Transactional(readOnly = true)
    public List<PosTerminalDto> getTerminals() {
        return getTerminals(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosTerminalDto> getTerminals(boolean includeInactive) {
        List<PosTerminal> terminals = includeInactive
                ? posTerminalRepository.findAllByOrderByNameAsc()
                : posTerminalRepository.findByActiveTrueOrderByNameAsc();
        if (terminals.isEmpty()) {
            terminals = List.of(posTerminalRepository.save(createDefaultTerminal()));
        }
        return terminals.stream().map(this::mapTerminal).toList();
    }

    @Override
    @Transactional
    public PosTerminalDto createTerminal(CreatePosTerminalRequest request) {
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());

        String terminalCode = request.getTerminalCode() == null || request.getTerminalCode().isBlank()
                ? generateTerminalCode()
                : request.getTerminalCode().trim().toUpperCase();

        posTerminalRepository.findByTerminalCode(terminalCode).ifPresent(existing -> {
            throw new BadRequestException("POS terminal code already exists: " + terminalCode);
        });

        PosTerminal terminal = new PosTerminal();
        terminal.setTerminalCode(terminalCode);
        terminal.setName(request.getName().trim());
        terminal.setWarehouse(warehouse);
        terminal.setActive(true);
        terminal.setNotes(request.getNotes());
        return mapTerminal(posTerminalRepository.save(terminal));
    }

    @Override
    @Transactional
    public PosTerminalDto updateTerminalStatus(UUID terminalId, UpdatePosTerminalStatusRequest request) {
        PosTerminal terminal = posTerminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("POS terminal not found with ID: " + terminalId));

        if (Boolean.FALSE.equals(request.getActive())) {
            posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminalId, PosShiftStatus.OPEN)
                    .ifPresent(existing -> {
                        throw new BadRequestException("Close the open shift before deactivating this terminal");
                    });
        }

        terminal.setActive(request.getActive());
        if (request.getNotes() != null) {
            terminal.setNotes(request.getNotes().trim().isEmpty() ? null : request.getNotes().trim());
        }

        return mapTerminal(posTerminalRepository.save(terminal));
    }

    @Override
    @Transactional(readOnly = true)
    public PosBootstrapDto getBootstrap(UUID terminalId) {
        List<PosTerminal> terminals = posTerminalRepository.findByActiveTrueOrderByNameAsc();
        if (terminals.isEmpty()) {
            terminals = List.of(posTerminalRepository.save(createDefaultTerminal()));
        }

        PosBootstrapDto dto = new PosBootstrapDto();
        dto.setTerminals(terminals.stream().map(this::mapTerminal).toList());
        dto.setWarehouses(warehouseRepository.findAll().stream().map(this::mapWarehouse).toList());
        dto.setCustomers(customerRepository.findByStatus(CustomerStatus.ACTIVE).stream().map(this::mapCustomer).toList());
        dto.setCategories(categoryRepository.findAll().stream().map(this::mapCategory).toList());

        if (terminalId != null) {
            posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminalId, PosShiftStatus.OPEN)
                    .ifPresent(shift -> dto.setActiveShift(mapShift(shift)));
        } else {
            posShiftRepository.findFirstByCashierIdAndStatusOrderByOpenedAtDesc(getCurrentUser().getId(), PosShiftStatus.OPEN)
                    .ifPresent(shift -> dto.setActiveShift(mapShift(shift)));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PosCatalogItemDto> searchCatalog(String query, UUID categoryId, UUID warehouseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sku"));
        Page<ProductVariant> results;
        if ((query == null || query.isBlank()) && categoryId == null) {
            results = productVariantRepository.findAll(pageable);
        } else if (categoryId != null && (query == null || query.isBlank())) {
            results = productVariantRepository.findByTemplateCategoryId(categoryId, pageable);
        } else {
            String normalized = query == null ? "" : query.trim();
            results = productVariantRepository.searchByQuery(normalized, pageable);
            if (categoryId != null) {
                List<ProductVariant> filtered = results.getContent().stream()
                        .filter(item -> item.getTemplate() != null
                                && item.getTemplate().getCategory() != null
                                && categoryId.equals(item.getTemplate().getCategory().getId()))
                        .toList();
                results = new PageImpl<>(filtered, pageable, filtered.size());
            }
        }

        List<PosCatalogItemDto> content = results.getContent().stream()
                .map(item -> mapCatalog(item, warehouseId))
                .toList();

        return new PageImpl<>(content, pageable, results.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PosCatalogItemDto scanBarcode(String barcode, UUID warehouseId) {
        if (barcode == null || barcode.isBlank()) {
            throw new BadRequestException("Barcode is required");
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<ProductVariant> matches = productVariantRepository.searchByQuery(barcode.trim(), pageable)
            .stream()
            .filter(item -> barcode.trim().equalsIgnoreCase(item.getBarcode()) || barcode.trim().equalsIgnoreCase(item.getSku()))
                .toList();

        ProductVariant variant = matches.stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No catalog item found for barcode: " + barcode));

        return mapCatalog(variant, warehouseId);
    }

    @Override
    @Transactional
    public PosShiftDto openShift(CreatePosShiftRequest request) {
        PosTerminal terminal = resolveTerminal(request.getTerminalId());
        User cashier = getCurrentUser();

        posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminal.getId(), PosShiftStatus.OPEN)
                .ifPresent(existing -> {
                    throw new BadRequestException("An open shift already exists for this terminal");
                });

        PosShift shift = new PosShift();
        shift.setTerminal(terminal);
        shift.setCashier(cashier);
        shift.setStatus(PosShiftStatus.OPEN);
        shift.setOpenedAt(LocalDateTime.now());
        shift.setOpeningFloat(scale(request.getOpeningFloat()));

        return mapShift(posShiftRepository.save(shift));
    }

    @Override
    @Transactional
    public PosShiftDto closeShift(UUID shiftId, ClosePosShiftRequest request) {
        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + shiftId));

        if (shift.getStatus() != PosShiftStatus.OPEN) {
            throw new BadRequestException("Only open shifts can be closed");
        }

        shift.setStatus(PosShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosingNotes(request.getClosingNotes());
        return mapShift(posShiftRepository.save(shift));
    }

    @Override
    @Transactional(readOnly = true)
    public PosShiftDto getCurrentShift(UUID terminalId) {
        PosShift shift = posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminalId, PosShiftStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("No open shift found for terminal: " + terminalId));
        return mapShift(shift);
    }

    @Override
    @Transactional
    public PosSaleDto createSale(CreatePosSaleRequest request, boolean offlineSync) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("POS sale must contain at least one item");
        }
        if (request.getClientSaleId() != null && !request.getClientSaleId().isBlank()) {
            PosSale existing = posSaleRepository.findByClientSaleId(request.getClientSaleId()).orElse(null);
            if (existing != null) {
                return mapSale(existing);
            }
        }

        PosTerminal terminal = resolveTerminal(request.getTerminalId());
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        if (!warehouse.getId().equals(terminal.getWarehouse().getId())) {
            throw new BadRequestException("POS terminal warehouse and sale warehouse must match");
        }

        User cashier = getCurrentUser();
        PosShift shift = resolveShift(request.getShiftId(), terminal, cashier);
        Customer customer = resolveCustomer(request.getCustomerId());
        PricingEvaluation pricingEvaluation = pricingEngineService.evaluatePosSale(customer, warehouse, terminal, request, hasManualDiscountOverridePermission());
        Map<UUID, Deque<PricingEvaluationLine>> pricedLines = buildLineQueues(pricingEvaluation);

        Set<UUID> variantIds = request.getItems().stream().map(PosSaleItemRequest::getProductVariantId).collect(Collectors.toSet());
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);
        if (variants.size() != variantIds.size()) {
            throw new BadRequestException("One or more POS items reference missing product variants");
        }

        var variantMap = variants.stream().collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
        List<PosSaleItem> saleItems = new ArrayList<>();
        List<StockTransactionItem> stockTransactionItems = new ArrayList<>();
        List<SalesOrderItem> salesOrderItems = new ArrayList<>();
        String receiptNumber = generateReceiptNumber();

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSoNumber(generateSalesOrderNumber());
        salesOrder.setCustomer(customer);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setOrderDate(LocalDateTime.now());
        salesOrder.setStatus(SalesOrderStatus.DELIVERED);
        salesOrder.setPriority(OrderPriority.MEDIUM);
        salesOrder.setCurrency(defaultCurrency(request.getCurrency()));
        salesOrder.setNotes(buildSalesOrderNotes(request, terminal, shift));
        salesOrder.setSalesChannel(SalesChannel.POS);
        salesOrder.setSubtotalAmount(pricingEvaluation.getBaseSubtotal());
        salesOrder.setDiscountAmount(pricingEvaluation.getTotalDiscount());
        salesOrder.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));

        for (PosSaleItemRequest itemRequest : request.getItems()) {
            ProductVariant variant = variantMap.get(itemRequest.getProductVariantId());
            PricingEvaluationLine pricedLine = popPricedLine(pricedLines, variant.getId());
            BigDecimal quantity = scale(itemRequest.getQuantity());
            BigDecimal onHand = stockRepository.countTotalQuantityByProductVariantAndWarehouse(variant.getId(), warehouse.getId());
            if (onHand == null || onHand.compareTo(quantity) < 0) {
                throw new BadRequestException("Insufficient stock for variant " + variant.getSku());
            }

            PosSaleItem saleItem = new PosSaleItem();
            saleItem.setProductVariant(variant);
            saleItem.setSkuSnapshot(variant.getSku());
            saleItem.setBarcodeSnapshot(variant.getBarcode());
            saleItem.setDescriptionSnapshot(buildVariantDescription(variant));
            saleItem.setQuantity(quantity);
            saleItem.setUnitPrice(pricedLine.getBaseUnitPrice());
            saleItem.setLineDiscount(pricedLine.getLineDiscountAmount());
            saleItem.setLineTotal(pricedLine.getLineTotalAmount());
            saleItems.add(saleItem);

            StockTransactionItem stockTransactionItem = new StockTransactionItem();
            stockTransactionItem.setProductVariant(variant);
            stockTransactionItem.setQuantity(quantity);
            stockTransactionItems.add(stockTransactionItem);

            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setSalesOrder(salesOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(quantity);
            orderItem.setBaseUnitPrice(pricedLine.getBaseUnitPrice());
            orderItem.setUnitPrice(pricedLine.getFinalUnitPrice());
            orderItem.setLineDiscount(pricedLine.getLineDiscountAmount());
            orderItem.setAppliedPromotionCodes(String.join(", ", pricedLine.getAppliedPromotionCodes()));
            orderItem.setTotalPrice(pricedLine.getLineTotalAmount());
            orderItem.setShippedQuantity(quantity);
            salesOrderItems.add(orderItem);
        }

        BigDecimal tax = scale(request.getTaxAmount());
        BigDecimal subtotal = pricingEvaluation.getBaseSubtotal();
        BigDecimal discount = pricingEvaluation.getTotalDiscount();
        BigDecimal total = pricingEvaluation.getNetSubtotal().add(tax).setScale(6, RoundingMode.HALF_UP);
        BigDecimal tendered = scale(request.getTenderedAmount());
        BigDecimal change = tendered.subtract(total).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);

        salesOrder.setItems(salesOrderItems);
        salesOrder.setTotalAmount(total);
        salesOrder = salesOrderRepository.save(salesOrder);

        StockTransaction stockTransaction = new StockTransaction();
        stockTransaction.setTransactionNumber(generateStockTransactionNumber());
        stockTransaction.setType(StockTransactionType.OUTBOUND);
        stockTransaction.setStatus(com.inventory.system.common.entity.StockTransactionStatus.COMPLETED);
        stockTransaction.setReference("POS " + salesOrder.getSoNumber());
        stockTransaction.setNotes("POS sale " + terminal.getTerminalCode() + " receipt " + receiptNumber);
        stockTransaction.setTransactionDate(LocalDateTime.now());
        stockTransaction.setSourceWarehouse(warehouse);
        for (StockTransactionItem transactionItem : stockTransactionItems) {
            transactionItem.setStockTransaction(stockTransaction);
            stockTransaction.getItems().add(transactionItem);
        }
        stockTransaction = stockTransactionRepository.save(stockTransaction);

        PosSale sale = new PosSale();
        sale.setReceiptNumber(receiptNumber);
        sale.setClientSaleId(blankToNull(request.getClientSaleId()));
        sale.setTerminal(terminal);
        sale.setShift(shift);
        sale.setCashier(cashier);
        sale.setCustomer(customer);
        sale.setWarehouse(warehouse);
        sale.setSalesOrder(salesOrder);
        sale.setStockTransaction(stockTransaction);
        sale.setPaymentMethod(request.getPaymentMethod() == null ? PosPaymentMethod.OTHER : request.getPaymentMethod());
        sale.setSaleTime(LocalDateTime.now());
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discount);
        sale.setTaxAmount(tax);
        sale.setTotalAmount(total);
        sale.setTenderedAmount(tendered);
        sale.setChangeAmount(change);
        sale.setCurrency(defaultCurrency(request.getCurrency()));
        sale.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));
        sale.setNotes(request.getNotes());
        sale.setSyncStatus(offlineSync ? PosSyncStatus.OFFLINE_SYNCED : PosSyncStatus.ONLINE);

        int itemIndex = 0;
        for (PosSaleItem saleItem : saleItems) {
            saleItem.setSale(sale);
            sale.getItems().add(saleItem);

            StockAdjustmentDto outbound = new StockAdjustmentDto();
            outbound.setProductVariantId(saleItem.getProductVariant().getId());
            outbound.setWarehouseId(warehouse.getId());
            outbound.setQuantity(saleItem.getQuantity());
            outbound.setType(StockMovement.StockMovementType.OUT);
            outbound.setReason("POS sale " + sale.getReceiptNumber());
            outbound.setReferenceId(salesOrder.getId().toString());
            var movement = stockService.adjustStock(outbound);
            stockTransaction.getItems().get(itemIndex).setUnitCost(movement.getUnitCost());
            itemIndex += 1;
        }
        stockTransactionRepository.save(stockTransaction);

        PosSale savedSale = posSaleRepository.save(sale);
        pricingEngineService.recordRedemptions(pricingEvaluation, salesOrder, savedSale, customer, SalesChannel.POS, savedSale.getReceiptNumber());
        return mapSale(savedSale);
    }

    @Override
    @Transactional
    public List<PosSaleDto> syncOfflineSales(List<CreatePosSaleRequest> requests) {
        return requests.stream().map(request -> createSale(request, true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PosSaleDto> getSales(UUID cashierId, UUID terminalId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleTime"));
        Page<PosSale> sales;
        if (cashierId != null) {
            sales = posSaleRepository.findByCashierIdOrderBySaleTimeDesc(cashierId, pageable);
        } else if (terminalId != null) {
            sales = posSaleRepository.findByTerminalIdOrderBySaleTimeDesc(terminalId, pageable);
        } else {
            sales = posSaleRepository.findAll(pageable);
        }
        return sales.map(this::mapSale);
    }

    @Override
    @Transactional(readOnly = true)
    public PosKpiDto getKpis(UUID cashierId, UUID terminalId, LocalDate businessDate) {
        LocalDate date = businessDate == null ? LocalDate.now() : businessDate;
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(23, 59, 59);

        BigDecimal gross = posSaleRepository.sumTotalAmount(cashierId, terminalId, from, to);
        BigDecimal units = posSaleRepository.sumUnits(cashierId, terminalId, from, to);
        long tickets = posSaleRepository.countCompletedSales(cashierId, terminalId, from, to);

        PosKpiDto dto = new PosKpiDto();
        dto.setGrossSales(scale(gross));
        dto.setUnitsSold(scale(units));
        dto.setTicketCount(tickets);
        dto.setAverageTicket(tickets == 0 ? ZERO : scale(gross.divide(BigDecimal.valueOf(tickets), 6, RoundingMode.HALF_UP)));
        return dto;
    }

    private PosTerminal resolveTerminal(UUID terminalId) {
        if (terminalId == null) {
            List<PosTerminal> terminals = posTerminalRepository.findByActiveTrueOrderByNameAsc();
            if (terminals.isEmpty()) {
                return posTerminalRepository.save(createDefaultTerminal());
            }
            return terminals.get(0);
        }
        PosTerminal terminal = posTerminalRepository.findById(terminalId)
            .orElseThrow(() -> new ResourceNotFoundException("POS terminal not found with ID: " + terminalId));
        if (!Boolean.TRUE.equals(terminal.getActive())) {
            throw new BadRequestException("Selected POS terminal is inactive");
        }
        return terminal;
    }

    private Warehouse resolveWarehouse(UUID warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId));
    }

    private PosShift resolveShift(UUID shiftId, PosTerminal terminal, User cashier) {
        if (shiftId != null) {
            PosShift shift = posShiftRepository.findById(shiftId)
                    .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + shiftId));
            if (shift.getStatus() != PosShiftStatus.OPEN) {
                throw new BadRequestException("POS shift is closed");
            }
            return shift;
        }

        return posShiftRepository.findFirstByTerminalIdAndStatusOrderByOpenedAtDesc(terminal.getId(), PosShiftStatus.OPEN)
                .orElseGet(() -> {
                    PosShift shift = new PosShift();
                    shift.setTerminal(terminal);
                    shift.setCashier(cashier);
                    shift.setStatus(PosShiftStatus.OPEN);
                    shift.setOpenedAt(LocalDateTime.now());
                    shift.setOpeningFloat(ZERO);
                    return posShiftRepository.save(shift);
                });
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private boolean hasManualDiscountOverridePermission() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "POS:MANUAL_DISCOUNT_OVERRIDE".equals(authority.getAuthority()));
    }

    private Customer resolveCustomer(UUID customerId) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        }

        return customerRepository.findAll().stream()
                .filter(customer -> WALK_IN_CUSTOMER_NAME.equalsIgnoreCase(customer.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setName(WALK_IN_CUSTOMER_NAME);
                    customer.setContactName("Counter sale");
                    customer.setCategory(CustomerCategory.OTHER);
                    customer.setCreditLimit(BigDecimal.ZERO);
                    customer.setOutstandingBalance(BigDecimal.ZERO);
                    customer.setIsActive(true);
                    customer.setStatus(CustomerStatus.ACTIVE);
                    return customerRepository.save(customer);
                });
    }

    private PosTerminal createDefaultTerminal() {
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("At least one warehouse is required before using POS"));

        PosTerminal terminal = new PosTerminal();
        terminal.setTerminalCode("POS-" + TenantContext.getTenantId().replaceAll("[^A-Za-z0-9]", "").toUpperCase() + "-01");
        terminal.setName("Main Counter");
        terminal.setWarehouse(warehouse);
        terminal.setActive(true);
        terminal.setNotes("Auto-provisioned default POS terminal");
        return terminal;
    }

    private String buildVariantDescription(ProductVariant variant) {
        return variant.getTemplate() != null ? variant.getTemplate().getName() : variant.getSku();
    }

    private String buildSalesOrderNotes(CreatePosSaleRequest request, PosTerminal terminal, PosShift shift) {
        List<String> parts = new ArrayList<>();
        parts.add("POS terminal " + terminal.getTerminalCode());
        if (shift != null) {
            parts.add("Shift " + shift.getId());
        }
        parts.add("Payment " + request.getPaymentMethod());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            parts.add(request.getNotes().trim());
        }
        return String.join(" | ", parts);
    }

    private String generateReceiptNumber() {
        return "RCPT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateSalesOrderNumber() {
        return "SO-POS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateStockTransactionNumber() {
        return "ST-POS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateTerminalCode() {
        return "POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String defaultCurrency(String currency) {
        return (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(6, RoundingMode.HALF_UP);
    }

    private PosTerminalDto mapTerminal(PosTerminal terminal) {
        PosTerminalDto dto = new PosTerminalDto();
        dto.setId(terminal.getId());
        dto.setTerminalCode(terminal.getTerminalCode());
        dto.setName(terminal.getName());
        dto.setWarehouseId(terminal.getWarehouse().getId());
        dto.setWarehouseName(terminal.getWarehouse().getName());
        dto.setActive(terminal.getActive());
        dto.setNotes(terminal.getNotes());
        return dto;
    }

    private PosShiftDto mapShift(PosShift shift) {
        PosShiftDto dto = new PosShiftDto();
        dto.setId(shift.getId());
        dto.setTerminalId(shift.getTerminal().getId());
        dto.setTerminalName(shift.getTerminal().getName());
        dto.setCashierId(shift.getCashier().getId());
        dto.setCashierName(buildCashierName(shift.getCashier()));
        dto.setStatus(shift.getStatus());
        dto.setOpenedAt(shift.getOpenedAt());
        dto.setClosedAt(shift.getClosedAt());
        dto.setOpeningFloat(shift.getOpeningFloat());
        dto.setClosingNotes(shift.getClosingNotes());
        return dto;
    }

    private PosSaleDto mapSale(PosSale sale) {
        PosSaleDto dto = new PosSaleDto();
        dto.setId(sale.getId());
        dto.setReceiptNumber(sale.getReceiptNumber());
        dto.setClientSaleId(sale.getClientSaleId());
        dto.setTerminalId(sale.getTerminal().getId());
        dto.setTerminalName(sale.getTerminal().getName());
        dto.setShiftId(sale.getShift() != null ? sale.getShift().getId() : null);
        dto.setCashierId(sale.getCashier().getId());
        dto.setCashierName(buildCashierName(sale.getCashier()));
        dto.setCustomerId(sale.getCustomer().getId());
        dto.setCustomerName(sale.getCustomer().getName());
        dto.setWarehouseId(sale.getWarehouse().getId());
        dto.setWarehouseName(sale.getWarehouse().getName());
        dto.setSalesOrderId(sale.getSalesOrder() != null ? sale.getSalesOrder().getId() : null);
        dto.setSoNumber(sale.getSalesOrder() != null ? sale.getSalesOrder().getSoNumber() : null);
        dto.setStockTransactionId(sale.getStockTransaction() != null ? sale.getStockTransaction().getId() : null);
        dto.setSaleStatus(sale.getSaleStatus());
        dto.setSyncStatus(sale.getSyncStatus());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setSaleTime(sale.getSaleTime());
        dto.setSubtotal(sale.getSubtotal());
        dto.setDiscountAmount(sale.getDiscountAmount());
        dto.setTaxAmount(sale.getTaxAmount());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setTenderedAmount(sale.getTenderedAmount());
        dto.setChangeAmount(sale.getChangeAmount());
        dto.setCurrency(sale.getCurrency());
        dto.setAppliedCouponCodes(sale.getAppliedCouponCodes());
        dto.setNotes(sale.getNotes());
        dto.setItems(sale.getItems().stream().map(this::mapSaleItem).toList());
        return dto;
    }

    private PosSaleItemDto mapSaleItem(PosSaleItem item) {
        PosSaleItemDto dto = new PosSaleItemDto();
        dto.setId(item.getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setSku(item.getSkuSnapshot());
        dto.setBarcode(item.getBarcodeSnapshot());
        dto.setDescription(item.getDescriptionSnapshot());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineDiscount(item.getLineDiscount());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private WarehouseDto mapWarehouse(Warehouse warehouse) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setLocation(warehouse.getLocation());
        dto.setType(warehouse.getType());
        dto.setContactNumber(warehouse.getContactNumber());
        dto.setIsActive(warehouse.getIsActive());
        dto.setCapacity(warehouse.getCapacity());
        dto.setUsedCapacity(warehouse.getUsedCapacity());
        return dto;
    }

    private CustomerDto mapCustomer(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setContactName(customer.getContactName());
        dto.setEmail(customer.getEmail());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setAddress(customer.getAddress());
        dto.setCreditLimit(customer.getCreditLimit());
        dto.setOutstandingBalance(customer.getOutstandingBalance());
        dto.setCategory(customer.getCategory());
        dto.setIsActive(customer.getIsActive());
        dto.setStatus(customer.getStatus());
        return dto;
    }

    private CategoryDto mapCategory(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setAttributeIds(category.getAttributes().stream().map(attribute -> attribute.getId()).collect(Collectors.toSet()));
        return dto;
    }

    private PosCatalogItemDto mapCatalog(ProductVariant item, UUID warehouseId) {
        PosCatalogItemDto dto = new PosCatalogItemDto();
        dto.setId(item.getId());
        dto.setSku(item.getSku());
        dto.setBarcode(item.getBarcode());
        dto.setName(item.getTemplate() != null ? item.getTemplate().getName() : item.getSku());
        dto.setDescription(item.getTemplate() != null ? item.getTemplate().getDescription() : item.getSku());
        dto.setPrice(item.getPrice());
        dto.setOnHand(warehouseId == null ? null : scale(stockRepository.countTotalQuantityByProductVariantAndWarehouse(item.getId(), warehouseId)));
        return dto;
    }

    private String buildCashierName(User user) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }

    private Map<UUID, Deque<PricingEvaluationLine>> buildLineQueues(PricingEvaluation evaluation) {
        Map<UUID, Deque<PricingEvaluationLine>> lines = new HashMap<>();
        for (PricingEvaluationLine line : evaluation.getLines()) {
            lines.computeIfAbsent(line.getProductVariant().getId(), ignored -> new ArrayDeque<>()).add(line);
        }
        return lines;
    }

    private PricingEvaluationLine popPricedLine(Map<UUID, Deque<PricingEvaluationLine>> linesByVariant, UUID productVariantId) {
        Deque<PricingEvaluationLine> lines = linesByVariant.get(productVariantId);
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("Unable to resolve pricing line for product variant: " + productVariantId);
        }
        return lines.removeFirst();
    }
}