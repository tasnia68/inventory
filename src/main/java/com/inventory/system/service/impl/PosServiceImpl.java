package com.inventory.system.service.impl;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.CategoryDto;
import com.inventory.system.payload.ClosePosShiftRequest;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.CreateSuspendedPosSaleRequest;
import com.inventory.system.payload.CreatePosShiftRequest;
import com.inventory.system.payload.CreatePosTerminalRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.PosBootstrapDto;
import com.inventory.system.payload.PosCashMovementDto;
import com.inventory.system.payload.PosCashMovementRequest;
import com.inventory.system.payload.PosCatalogItemDto;
import com.inventory.system.payload.PosDailySettlementDto;
import com.inventory.system.payload.PosKpiDto;
import com.inventory.system.payload.PosSaleDto;
import com.inventory.system.payload.PosSaleItemDto;
import com.inventory.system.payload.PosSaleItemRequest;
import com.inventory.system.payload.PosSalePaymentDto;
import com.inventory.system.payload.PosSalePaymentRequest;
import com.inventory.system.payload.PosShiftDto;
import com.inventory.system.payload.PosShiftSettlementDto;
import com.inventory.system.payload.PosShiftTenderCountDto;
import com.inventory.system.payload.PosShiftTenderCountRequest;
import com.inventory.system.payload.PosSettlementApprovalRequest;
import com.inventory.system.payload.PosRefundSettlementImpactDto;
import com.inventory.system.payload.PosSuspendedSaleDto;
import com.inventory.system.payload.PosTerminalDto;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.SuspendedPosSaleItemDto;
import com.inventory.system.payload.SuspendedPosSaleItemRequest;
import com.inventory.system.payload.UpdatePosTerminalStatusRequest;
import com.inventory.system.payload.WarehouseDto;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.PosCashMovementRepository;
import com.inventory.system.repository.PosRefundSettlementImpactRepository;
import com.inventory.system.repository.PosSaleRepository;
import com.inventory.system.repository.PosSalePaymentRepository;
import com.inventory.system.repository.PosShiftRepository;
import com.inventory.system.repository.PosShiftTenderCountRepository;
import com.inventory.system.repository.PosSuspendedSaleRepository;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final PosSalePaymentRepository posSalePaymentRepository;
    private final PosCashMovementRepository posCashMovementRepository;
    private final PosShiftTenderCountRepository posShiftTenderCountRepository;
    private final PosSuspendedSaleRepository posSuspendedSaleRepository;
    private final PosRefundSettlementImpactRepository posRefundSettlementImpactRepository;
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

        SettlementSnapshot snapshot = buildSettlementSnapshot(shift);
        persistTenderCounts(shift, snapshot.expectedByMethod, request == null ? List.of() : request.getTenderCounts());
        SettlementSnapshot updatedSnapshot = buildSettlementSnapshot(shift);

        shift.setStatus(PosShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosingNotes(request == null ? null : blankToNull(request.getClosingNotes()));
        shift.setExpectedCashAmount(updatedSnapshot.expectedByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO));
        shift.setDeclaredCashAmount(updatedSnapshot.declaredByMethod.getOrDefault(PosPaymentMethod.CASH, updatedSnapshot.expectedByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO)));
        shift.setOverShortAmount(updatedSnapshot.varianceByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO));
        boolean hasVariance = updatedSnapshot.varianceByMethod.values().stream().anyMatch(value -> value.compareTo(ZERO) != 0);
        shift.setSettlementApprovalStatus(hasVariance ? PosSettlementApprovalStatus.PENDING_APPROVAL : PosSettlementApprovalStatus.APPROVED);
        if (!hasVariance) {
            shift.setSettlementApprovedAt(LocalDateTime.now());
            shift.setSettlementApprovedBy(getCurrentUser());
            shift.setSettlementApprovalNotes("Auto-approved: no settlement variance detected");
        }
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
    public PosCashMovementDto recordCashMovement(UUID shiftId, PosCashMovementRequest request) {
        if (!hasAuthority("POS:CASH_CONTROL")) {
            throw new BadRequestException("Current user does not have permission to record POS cash movements");
        }
        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + shiftId));
        if (shift.getStatus() != PosShiftStatus.OPEN) {
            throw new BadRequestException("Cash movements can only be recorded against open shifts");
        }

        PosCashMovement movement = new PosCashMovement();
        movement.setShift(shift);
        movement.setTerminal(shift.getTerminal());
        movement.setCashier(getCurrentUser());
        movement.setType(request.getType());
        movement.setAmount(scale(request.getAmount()));
        movement.setOccurredAt(LocalDateTime.now());
        movement.setReason(request.getReason().trim());
        movement.setReferenceNumber(blankToNull(request.getReferenceNumber()));
        movement.setNotes(blankToNull(request.getNotes()));
        return mapCashMovement(posCashMovementRepository.save(movement));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosCashMovementDto> getCashMovements(UUID shiftId) {
        return posCashMovementRepository.findByShiftIdOrderByOccurredAtDesc(shiftId).stream()
                .map(this::mapCashMovement)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosShiftSettlementDto getShiftSettlement(UUID shiftId) {
        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + shiftId));
        return mapSettlement(buildSettlementSnapshot(shift));
    }

    @Override
    @Transactional
    public PosShiftSettlementDto approveShiftSettlement(UUID shiftId, PosSettlementApprovalRequest request) {
        if (!hasAuthority("POS:SETTLEMENT_APPROVE")) {
            throw new BadRequestException("Current user does not have permission to approve POS settlements");
        }

        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("POS shift not found with ID: " + shiftId));
        if (shift.getStatus() != PosShiftStatus.CLOSED) {
            throw new BadRequestException("Only closed shifts can be approved or rejected");
        }

        shift.setSettlementApprovalStatus(Boolean.TRUE.equals(request.getApproved())
                ? PosSettlementApprovalStatus.APPROVED
                : PosSettlementApprovalStatus.REJECTED);
        shift.setSettlementApprovedAt(LocalDateTime.now());
        shift.setSettlementApprovedBy(getCurrentUser());
        shift.setSettlementApprovalNotes(blankToNull(request.getNotes()));
        return mapSettlement(buildSettlementSnapshot(posShiftRepository.save(shift)));
    }

    @Override
    @Transactional(readOnly = true)
    public PosDailySettlementDto getDailySettlement(LocalDate businessDate, UUID terminalId) {
        LocalDate date = businessDate == null ? LocalDate.now() : businessDate;
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(23, 59, 59);
        List<PosShift> shifts = terminalId == null
                ? posShiftRepository.findByOpenedAtBetweenOrderByOpenedAtDesc(from, to)
                : posShiftRepository.findByTerminalIdAndOpenedAtBetweenOrderByOpenedAtDesc(terminalId, from, to);

        PosDailySettlementDto dto = new PosDailySettlementDto();
        dto.setBusinessDate(date);
        if (terminalId != null && !shifts.isEmpty()) {
            dto.setTerminalId(shifts.get(0).getTerminal().getId());
            dto.setTerminalName(shifts.get(0).getTerminal().getName());
        }
        dto.setShiftCount(shifts.size());
        dto.setOpenShiftCount(shifts.stream().filter(shift -> shift.getStatus() == PosShiftStatus.OPEN).count());
        dto.setTotalSales(ZERO);
        dto.setTotalRefunds(ZERO);
        dto.setTotalCashInflows(ZERO);
        dto.setTotalCashOutflows(ZERO);
        dto.setExpectedCash(ZERO);
        dto.setDeclaredCash(ZERO);
        dto.setOverShortAmount(ZERO);

        for (PosShift shift : shifts) {
            SettlementSnapshot snapshot = buildSettlementSnapshot(shift);
            dto.setTotalSales(dto.getTotalSales().add(snapshot.totalSales));
            dto.setTotalRefunds(dto.getTotalRefunds().add(snapshot.totalRefunds));
            dto.setTotalCashInflows(dto.getTotalCashInflows().add(snapshot.totalCashInflows));
            dto.setTotalCashOutflows(dto.getTotalCashOutflows().add(snapshot.totalCashOutflows));
            dto.setExpectedCash(dto.getExpectedCash().add(snapshot.shift.getExpectedCashAmount() == null ? ZERO : snapshot.shift.getExpectedCashAmount()));
            dto.setDeclaredCash(dto.getDeclaredCash().add(snapshot.shift.getDeclaredCashAmount() == null ? ZERO : snapshot.shift.getDeclaredCashAmount()));
            dto.setOverShortAmount(dto.getOverShortAmount().add(snapshot.shift.getOverShortAmount() == null ? ZERO : snapshot.shift.getOverShortAmount()));
            if (dto.getTerminalId() == null) {
                dto.setTerminalId(shift.getTerminal().getId());
                dto.setTerminalName(shift.getTerminal().getName());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public PosSuspendedSaleDto suspendSale(CreateSuspendedPosSaleRequest request) {
        if (!hasAuthority("POS:SUSPEND_SALE")) {
            throw new BadRequestException("Current user does not have permission to suspend POS sales");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Suspended POS sale must contain at least one item");
        }

        PosTerminal terminal = resolveTerminal(request.getTerminalId());
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        if (!warehouse.getId().equals(terminal.getWarehouse().getId())) {
            throw new BadRequestException("POS terminal warehouse and suspended sale warehouse must match");
        }

        Customer customer = resolveCustomer(request.getCustomerId());
        Map<UUID, ProductVariant> variantMap = resolveVariantMap(request.getItems().stream()
                .map(SuspendedPosSaleItemRequest::getProductVariantId)
                .collect(Collectors.toSet()));

        PosSuspendedSale suspendedSale = new PosSuspendedSale();
        suspendedSale.setSuspendedNumber(generateSuspendedSaleNumber());
        suspendedSale.setTerminal(terminal);
        suspendedSale.setCashier(getCurrentUser());
        suspendedSale.setCustomer(customer);
        suspendedSale.setWarehouse(warehouse);
        suspendedSale.setStatus(PosSuspendedSaleStatus.SUSPENDED);
        suspendedSale.setSuspendedAt(LocalDateTime.now());
        suspendedSale.setManualDiscountAmount(scale(request.getManualDiscountAmount()));
        suspendedSale.setTaxAmount(scale(request.getTaxAmount()));
        suspendedSale.setCurrency(defaultCurrency(request.getCurrency()));
        suspendedSale.setCouponCodes(joinCouponCodes(request.getCouponCodes()));
        suspendedSale.setNotes(blankToNull(request.getNotes()));

        BigDecimal subtotal = ZERO;
        for (SuspendedPosSaleItemRequest itemRequest : request.getItems()) {
            ProductVariant variant = variantMap.get(itemRequest.getProductVariantId());
            BigDecimal quantity = scale(itemRequest.getQuantity());
            BigDecimal unitPrice = scale(itemRequest.getUnitPrice());
            BigDecimal lineDiscount = scale(itemRequest.getLineDiscount());
            BigDecimal lineTotal = unitPrice.multiply(quantity).subtract(lineDiscount).setScale(6, RoundingMode.HALF_UP);
            if (lineTotal.compareTo(ZERO) < 0) {
                throw new BadRequestException("Suspended sale line total cannot be negative");
            }

            PosSuspendedSaleItem item = new PosSuspendedSaleItem();
            item.setSuspendedSale(suspendedSale);
            item.setProductVariant(variant);
            item.setSkuSnapshot(variant.getSku());
            item.setDescriptionSnapshot(buildVariantDescription(variant));
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            suspendedSale.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
        }

        suspendedSale.setSubtotalAmount(subtotal);
        suspendedSale.setTotalAmount(subtotal.subtract(suspendedSale.getManualDiscountAmount()).add(suspendedSale.getTaxAmount()).max(ZERO).setScale(6, RoundingMode.HALF_UP));
        return mapSuspendedSale(posSuspendedSaleRepository.save(suspendedSale));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosSuspendedSaleDto> getSuspendedSales(UUID terminalId) {
        return posSuspendedSaleRepository.findByTerminalIdAndStatusOrderBySuspendedAtDesc(terminalId, PosSuspendedSaleStatus.SUSPENDED).stream()
                .map(this::mapSuspendedSale)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosSuspendedSaleDto resumeSuspendedSale(UUID suspendedSaleId) {
        PosSuspendedSale suspendedSale = posSuspendedSaleRepository.findByIdAndStatus(suspendedSaleId, PosSuspendedSaleStatus.SUSPENDED)
                .orElseThrow(() -> new ResourceNotFoundException("Suspended POS sale not found with ID: " + suspendedSaleId));
        return mapSuspendedSale(suspendedSale);
    }

    @Override
    @Transactional
    public PosSuspendedSaleDto cancelSuspendedSale(UUID suspendedSaleId) {
        if (!hasAuthority("POS:SUSPEND_SALE")) {
            throw new BadRequestException("Current user does not have permission to cancel suspended POS sales");
        }
        PosSuspendedSale suspendedSale = posSuspendedSaleRepository.findByIdAndStatus(suspendedSaleId, PosSuspendedSaleStatus.SUSPENDED)
                .orElseThrow(() -> new ResourceNotFoundException("Suspended POS sale not found with ID: " + suspendedSaleId));
        suspendedSale.setStatus(PosSuspendedSaleStatus.CANCELLED);
        suspendedSale.setCancelledAt(LocalDateTime.now());
        return mapSuspendedSale(posSuspendedSaleRepository.save(suspendedSale));
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
    PosSuspendedSale suspendedSale = resolveSuspendedSale(request.getSuspendedSaleId());
        if (suspendedSale != null) {
            if (!suspendedSale.getTerminal().getId().equals(terminal.getId())) {
                throw new BadRequestException("Suspended sale terminal does not match the POS terminal for this sale");
            }
            if (!suspendedSale.getWarehouse().getId().equals(warehouse.getId())) {
                throw new BadRequestException("Suspended sale warehouse does not match the POS sale warehouse");
            }
        }
    Customer customer = suspendedSale != null ? suspendedSale.getCustomer() : resolveCustomer(request.getCustomerId());
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
        List<PosSalePaymentRequest> paymentRequests = normalizePaymentRequests(request, total);
        PosPaymentMethod resolvedPaymentMethod = derivePaymentMethod(request, paymentRequests);
        salesOrder.setNotes(buildSalesOrderNotes(request, terminal, shift, resolvedPaymentMethod));

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
        sale.setPaymentMethod(resolvedPaymentMethod);
        sale.setSaleTime(LocalDateTime.now());
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discount);
        sale.setTaxAmount(tax);
        sale.setTotalAmount(total);
        sale.setTenderedAmount(tendered);
        sale.setChangeAmount(change);
        sale.setCurrency(defaultCurrency(request.getCurrency()));
        sale.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));
        sale.setNotes(blankToNull(request.getNotes()));
        sale.setSyncStatus(offlineSync ? PosSyncStatus.OFFLINE_SYNCED : PosSyncStatus.ONLINE);
        sale.setSuspendedSale(suspendedSale);

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

        for (PosSalePaymentRequest paymentRequest : paymentRequests) {
            PosSalePayment payment = new PosSalePayment();
            payment.setSale(sale);
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());
            payment.setAmount(scale(paymentRequest.getAmount()));
            payment.setReferenceNumber(blankToNull(paymentRequest.getReferenceNumber()));
            payment.setNotes(blankToNull(paymentRequest.getNotes()));
            sale.getPayments().add(payment);
        }

        PosSale savedSale = posSaleRepository.save(sale);
        if (suspendedSale != null) {
            suspendedSale.setStatus(PosSuspendedSaleStatus.COMPLETED);
            suspendedSale.setCompletedAt(LocalDateTime.now());
            suspendedSale.setCompletedSale(savedSale);
            posSuspendedSaleRepository.save(suspendedSale);
        }
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

    private String buildSalesOrderNotes(CreatePosSaleRequest request, PosTerminal terminal, PosShift shift, PosPaymentMethod paymentMethod) {
        List<String> parts = new ArrayList<>();
        parts.add("POS terminal " + terminal.getTerminalCode());
        if (shift != null) {
            parts.add("Shift " + shift.getId());
        }
        parts.add("Payment " + paymentMethod);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            parts.add(request.getNotes().trim());
        }
        return String.join(" | ", parts);
    }

    private String generateSuspendedSaleNumber() {
        return "HOLD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
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

    private String joinCouponCodes(List<String> couponCodes) {
        if (couponCodes == null || couponCodes.isEmpty()) {
            return null;
        }
        return couponCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private List<String> splitCouponCodes(String couponCodes) {
        if (couponCodes == null || couponCodes.isBlank()) {
            return List.of();
        }
        return List.of(couponCodes.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(6, RoundingMode.HALF_UP);
    }

    private PosSuspendedSale resolveSuspendedSale(UUID suspendedSaleId) {
        if (suspendedSaleId == null) {
            return null;
        }
        return posSuspendedSaleRepository.findByIdAndStatus(suspendedSaleId, PosSuspendedSaleStatus.SUSPENDED)
                .orElseThrow(() -> new ResourceNotFoundException("Suspended POS sale not found with ID: " + suspendedSaleId));
    }

    private Map<UUID, ProductVariant> resolveVariantMap(Set<UUID> variantIds) {
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);
        if (variants.size() != variantIds.size()) {
            throw new BadRequestException("One or more POS items reference missing product variants");
        }
        return variants.stream().collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
    }

    private List<PosSalePaymentRequest> normalizePaymentRequests(CreatePosSaleRequest request, BigDecimal total) {
        if (request.getPayments() == null || request.getPayments().isEmpty()) {
            if (request.getPaymentMethod() == null) {
                throw new BadRequestException("Payment method is required when no payment allocation lines are provided");
            }
            if (request.getPaymentMethod() == PosPaymentMethod.MIXED) {
                throw new BadRequestException("Mixed payment sales must provide payment allocation lines");
            }
            PosSalePaymentRequest payment = new PosSalePaymentRequest();
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setAmount(total);
            return List.of(payment);
        }

        BigDecimal allocated = request.getPayments().stream()
                .map(PosSalePaymentRequest::getAmount)
                .map(this::scale)
                .reduce(ZERO, BigDecimal::add);
        if (allocated.compareTo(total) != 0) {
            throw new BadRequestException("Payment allocations must equal the POS sale total");
        }
        return request.getPayments().stream().toList();
    }

    private PosPaymentMethod derivePaymentMethod(CreatePosSaleRequest request, List<PosSalePaymentRequest> payments) {
        Set<PosPaymentMethod> methods = payments.stream().map(PosSalePaymentRequest::getPaymentMethod).collect(Collectors.toSet());
        PosPaymentMethod derived = methods.size() > 1 ? PosPaymentMethod.MIXED : methods.iterator().next();
        if (request.getPaymentMethod() != null
                && request.getPaymentMethod() != PosPaymentMethod.MIXED
                && request.getPaymentMethod() != derived) {
            throw new BadRequestException("Payment method does not match payment allocation lines");
        }
        return derived;
    }

    private void persistTenderCounts(PosShift shift,
                                     Map<PosPaymentMethod, BigDecimal> expectedByMethod,
                                     List<PosShiftTenderCountRequest> declaredCounts) {
        Map<PosPaymentMethod, BigDecimal> declaredByMethod = new EnumMap<>(PosPaymentMethod.class);
        if (declaredCounts != null) {
            for (PosShiftTenderCountRequest countRequest : declaredCounts) {
                declaredByMethod.put(countRequest.getPaymentMethod(), scale(countRequest.getDeclaredAmount()));
            }
        }

        posShiftTenderCountRepository.deleteByShiftId(shift.getId());

        for (PosPaymentMethod method : EnumSet.of(PosPaymentMethod.CASH, PosPaymentMethod.CARD, PosPaymentMethod.TRANSFER, PosPaymentMethod.WALLET, PosPaymentMethod.OTHER)) {
            BigDecimal expected = expectedByMethod.getOrDefault(method, ZERO);
            BigDecimal declared = declaredByMethod.getOrDefault(method, expected);
            if (expected.compareTo(ZERO) == 0 && declared.compareTo(ZERO) == 0 && method != PosPaymentMethod.CASH) {
                continue;
            }

            PosShiftTenderCount count = new PosShiftTenderCount();
            count.setShift(shift);
            count.setPaymentMethod(method);
            count.setExpectedAmount(expected);
            count.setDeclaredAmount(declared);
            count.setVarianceAmount(declared.subtract(expected).setScale(6, RoundingMode.HALF_UP));
            posShiftTenderCountRepository.save(count);
        }
    }

    private SettlementSnapshot buildSettlementSnapshot(PosShift shift) {
        List<PosSalePayment> salePayments = posSalePaymentRepository.findByShiftId(shift.getId());
        List<PosCashMovement> cashMovements = posCashMovementRepository.findByShiftIdOrderByOccurredAtDesc(shift.getId());
        List<PosRefundSettlementImpact> refundImpacts = posRefundSettlementImpactRepository.findByShiftIdOrderByOccurredAtDesc(shift.getId());
        List<PosShiftTenderCount> tenderCounts = posShiftTenderCountRepository.findByShiftIdOrderByPaymentMethodAsc(shift.getId());

        Map<PosPaymentMethod, BigDecimal> salesByMethod = new EnumMap<>(PosPaymentMethod.class);
        Map<PosPaymentMethod, BigDecimal> refundsByMethod = new EnumMap<>(PosPaymentMethod.class);
        Map<PosPaymentMethod, BigDecimal> expectedByMethod = new EnumMap<>(PosPaymentMethod.class);
        Map<PosPaymentMethod, BigDecimal> declaredByMethod = new EnumMap<>(PosPaymentMethod.class);
        Map<PosPaymentMethod, BigDecimal> varianceByMethod = new EnumMap<>(PosPaymentMethod.class);
        Set<PosPaymentMethod> countedMethods = tenderCounts.stream()
            .map(PosShiftTenderCount::getPaymentMethod)
            .collect(Collectors.toSet());
        for (PosPaymentMethod method : PosPaymentMethod.values()) {
            salesByMethod.put(method, ZERO);
            refundsByMethod.put(method, ZERO);
            expectedByMethod.put(method, ZERO);
            declaredByMethod.put(method, ZERO);
            varianceByMethod.put(method, ZERO);
        }

        BigDecimal totalSales = ZERO;
        for (PosSalePayment payment : salePayments) {
            BigDecimal amount = scale(payment.getAmount());
            salesByMethod.put(payment.getPaymentMethod(), salesByMethod.get(payment.getPaymentMethod()).add(amount));
            totalSales = totalSales.add(amount);
        }

        BigDecimal totalRefunds = ZERO;
        for (PosRefundSettlementImpact impact : refundImpacts) {
            BigDecimal amount = scale(impact.getAmount());
            refundsByMethod.put(impact.getPaymentMethod(), refundsByMethod.getOrDefault(impact.getPaymentMethod(), ZERO).add(amount));
            totalRefunds = totalRefunds.add(amount);
        }

        BigDecimal totalCashInflows = ZERO;
        BigDecimal totalCashOutflows = ZERO;
        for (PosCashMovement movement : cashMovements) {
            if (isCashInflow(movement.getType())) {
                totalCashInflows = totalCashInflows.add(scale(movement.getAmount()));
            } else {
                totalCashOutflows = totalCashOutflows.add(scale(movement.getAmount()));
            }
        }

        for (PosPaymentMethod method : EnumSet.of(PosPaymentMethod.CARD, PosPaymentMethod.TRANSFER, PosPaymentMethod.WALLET, PosPaymentMethod.OTHER)) {
            expectedByMethod.put(method, salesByMethod.getOrDefault(method, ZERO).subtract(refundsByMethod.getOrDefault(method, ZERO)).setScale(6, RoundingMode.HALF_UP));
        }

        BigDecimal expectedCash = scale(shift.getOpeningFloat())
                .add(salesByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO))
                .subtract(refundsByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO))
                .add(totalCashInflows)
                .subtract(totalCashOutflows)
                .setScale(6, RoundingMode.HALF_UP);
        expectedByMethod.put(PosPaymentMethod.CASH, expectedCash);

        for (PosShiftTenderCount count : tenderCounts) {
            declaredByMethod.put(count.getPaymentMethod(), scale(count.getDeclaredAmount()));
            varianceByMethod.put(count.getPaymentMethod(), scale(count.getVarianceAmount()));
        }
        for (PosPaymentMethod method : expectedByMethod.keySet()) {
            if (!countedMethods.contains(method)) {
                declaredByMethod.put(method, expectedByMethod.getOrDefault(method, ZERO));
            }
            if (!countedMethods.contains(method)) {
                varianceByMethod.put(method, declaredByMethod.get(method).subtract(expectedByMethod.getOrDefault(method, ZERO)).setScale(6, RoundingMode.HALF_UP));
            }
        }

        return new SettlementSnapshot(shift, cashMovements, refundImpacts, tenderCounts, expectedByMethod, declaredByMethod,
                varianceByMethod, totalSales, totalRefunds, totalCashInflows, totalCashOutflows);
    }

    private boolean isCashInflow(PosCashMovementType type) {
        return type == PosCashMovementType.PAY_IN || type == PosCashMovementType.FLOAT_ADJUSTMENT;
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
        dto.setExpectedCashAmount(shift.getExpectedCashAmount());
        dto.setDeclaredCashAmount(shift.getDeclaredCashAmount());
        dto.setOverShortAmount(shift.getOverShortAmount());
        dto.setSettlementApprovalStatus(shift.getSettlementApprovalStatus());
        dto.setSettlementApprovedAt(shift.getSettlementApprovedAt());
        dto.setSettlementApprovedBy(shift.getSettlementApprovedBy() == null ? null : shift.getSettlementApprovedBy().getId());
        dto.setSettlementApprovedByName(shift.getSettlementApprovedBy() == null ? null : buildCashierName(shift.getSettlementApprovedBy()));
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
        dto.setSuspendedSaleId(sale.getSuspendedSale() == null ? null : sale.getSuspendedSale().getId());
        dto.setItems(sale.getItems().stream().map(this::mapSaleItem).toList());
        dto.setPayments(sale.getPayments().stream().map(this::mapSalePayment).toList());
        return dto;
    }

    private PosSalePaymentDto mapSalePayment(PosSalePayment payment) {
        PosSalePaymentDto dto = new PosSalePaymentDto();
        dto.setId(payment.getId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setNotes(payment.getNotes());
        return dto;
    }

    private PosCashMovementDto mapCashMovement(PosCashMovement movement) {
        PosCashMovementDto dto = new PosCashMovementDto();
        dto.setId(movement.getId());
        dto.setShiftId(movement.getShift().getId());
        dto.setTerminalId(movement.getTerminal().getId());
        dto.setTerminalName(movement.getTerminal().getName());
        dto.setCashierId(movement.getCashier().getId());
        dto.setCashierName(buildCashierName(movement.getCashier()));
        dto.setType(movement.getType());
        dto.setAmount(movement.getAmount());
        dto.setOccurredAt(movement.getOccurredAt());
        dto.setReason(movement.getReason());
        dto.setReferenceNumber(movement.getReferenceNumber());
        dto.setNotes(movement.getNotes());
        return dto;
    }

    private PosSuspendedSaleDto mapSuspendedSale(PosSuspendedSale sale) {
        PosSuspendedSaleDto dto = new PosSuspendedSaleDto();
        dto.setId(sale.getId());
        dto.setSuspendedNumber(sale.getSuspendedNumber());
        dto.setTerminalId(sale.getTerminal().getId());
        dto.setTerminalName(sale.getTerminal().getName());
        dto.setCashierId(sale.getCashier().getId());
        dto.setCashierName(buildCashierName(sale.getCashier()));
        dto.setCustomerId(sale.getCustomer().getId());
        dto.setCustomerName(sale.getCustomer().getName());
        dto.setWarehouseId(sale.getWarehouse().getId());
        dto.setWarehouseName(sale.getWarehouse().getName());
        dto.setStatus(sale.getStatus());
        dto.setSuspendedAt(sale.getSuspendedAt());
        dto.setCompletedAt(sale.getCompletedAt());
        dto.setCancelledAt(sale.getCancelledAt());
        dto.setManualDiscountAmount(sale.getManualDiscountAmount());
        dto.setTaxAmount(sale.getTaxAmount());
        dto.setSubtotalAmount(sale.getSubtotalAmount());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setCurrency(sale.getCurrency());
        dto.setCouponCodes(splitCouponCodes(sale.getCouponCodes()));
        dto.setNotes(sale.getNotes());
        dto.setItems(sale.getItems().stream().map(this::mapSuspendedSaleItem).toList());
        return dto;
    }

    private SuspendedPosSaleItemDto mapSuspendedSaleItem(PosSuspendedSaleItem item) {
        SuspendedPosSaleItemDto dto = new SuspendedPosSaleItemDto();
        dto.setId(item.getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setSku(item.getSkuSnapshot());
        dto.setDescription(item.getDescriptionSnapshot());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineDiscount(item.getLineDiscount());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private PosRefundSettlementImpactDto mapRefundImpact(PosRefundSettlementImpact impact) {
        PosRefundSettlementImpactDto dto = new PosRefundSettlementImpactDto();
        dto.setId(impact.getId());
        dto.setSalesRefundId(impact.getSalesRefund().getId());
        dto.setShiftId(impact.getShift() == null ? null : impact.getShift().getId());
        dto.setTerminalId(impact.getTerminal() == null ? null : impact.getTerminal().getId());
        dto.setPaymentMethod(impact.getPaymentMethod());
        dto.setAmount(impact.getAmount());
        dto.setOccurredAt(impact.getOccurredAt());
        dto.setReferenceNumber(impact.getReferenceNumber());
        dto.setNotes(impact.getNotes());
        return dto;
    }

    private PosShiftSettlementDto mapSettlement(SettlementSnapshot snapshot) {
        PosShiftSettlementDto dto = new PosShiftSettlementDto();
        dto.setShiftId(snapshot.shift.getId());
        dto.setTerminalId(snapshot.shift.getTerminal().getId());
        dto.setTerminalName(snapshot.shift.getTerminal().getName());
        dto.setCashierId(snapshot.shift.getCashier().getId());
        dto.setCashierName(buildCashierName(snapshot.shift.getCashier()));
        dto.setShiftStatus(snapshot.shift.getStatus());
        dto.setOpenedAt(snapshot.shift.getOpenedAt());
        dto.setClosedAt(snapshot.shift.getClosedAt());
        dto.setOpeningFloat(snapshot.shift.getOpeningFloat());
        dto.setTotalSales(snapshot.totalSales);
        dto.setTotalRefunds(snapshot.totalRefunds);
        dto.setTotalCashInflows(snapshot.totalCashInflows);
        dto.setTotalCashOutflows(snapshot.totalCashOutflows);
        dto.setExpectedCashAmount(snapshot.expectedByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO));
        dto.setDeclaredCashAmount(snapshot.declaredByMethod.getOrDefault(PosPaymentMethod.CASH, snapshot.expectedByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO)));
        dto.setOverShortAmount(snapshot.varianceByMethod.getOrDefault(PosPaymentMethod.CASH, ZERO));
        dto.setSettlementApprovalStatus(snapshot.shift.getSettlementApprovalStatus());
        dto.setSettlementApprovedAt(snapshot.shift.getSettlementApprovedAt());
        dto.setSettlementApprovedBy(snapshot.shift.getSettlementApprovedBy() == null ? null : snapshot.shift.getSettlementApprovedBy().getId());
        dto.setSettlementApprovedByName(snapshot.shift.getSettlementApprovedBy() == null ? null : buildCashierName(snapshot.shift.getSettlementApprovedBy()));
        dto.setSettlementApprovalNotes(snapshot.shift.getSettlementApprovalNotes());
        dto.setClosingNotes(snapshot.shift.getClosingNotes());
        dto.setTenderCounts(snapshot.tenderCounts.stream().map(this::mapTenderCount).toList());
        dto.setCashMovements(snapshot.cashMovements.stream().map(this::mapCashMovement).toList());
        dto.setRefundImpacts(snapshot.refundImpacts.stream().map(this::mapRefundImpact).toList());
        return dto;
    }

    private PosShiftTenderCountDto mapTenderCount(PosShiftTenderCount count) {
        PosShiftTenderCountDto dto = new PosShiftTenderCountDto();
        dto.setId(count.getId());
        dto.setPaymentMethod(count.getPaymentMethod());
        dto.setExpectedAmount(count.getExpectedAmount());
        dto.setDeclaredAmount(count.getDeclaredAmount());
        dto.setVarianceAmount(count.getVarianceAmount());
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

    private boolean hasAuthority(String authorityName) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authorityName.equals(authority.getAuthority()));
    }

    private static class SettlementSnapshot {
        private final PosShift shift;
        private final List<PosCashMovement> cashMovements;
        private final List<PosRefundSettlementImpact> refundImpacts;
        private final List<PosShiftTenderCount> tenderCounts;
        private final Map<PosPaymentMethod, BigDecimal> expectedByMethod;
        private final Map<PosPaymentMethod, BigDecimal> declaredByMethod;
        private final Map<PosPaymentMethod, BigDecimal> varianceByMethod;
        private final BigDecimal totalSales;
        private final BigDecimal totalRefunds;
        private final BigDecimal totalCashInflows;
        private final BigDecimal totalCashOutflows;

        private SettlementSnapshot(PosShift shift,
                                   List<PosCashMovement> cashMovements,
                                   List<PosRefundSettlementImpact> refundImpacts,
                                   List<PosShiftTenderCount> tenderCounts,
                                   Map<PosPaymentMethod, BigDecimal> expectedByMethod,
                                   Map<PosPaymentMethod, BigDecimal> declaredByMethod,
                                   Map<PosPaymentMethod, BigDecimal> varianceByMethod,
                                   BigDecimal totalSales,
                                   BigDecimal totalRefunds,
                                   BigDecimal totalCashInflows,
                                   BigDecimal totalCashOutflows) {
            this.shift = shift;
            this.cashMovements = cashMovements;
            this.refundImpacts = refundImpacts;
            this.tenderCounts = tenderCounts;
            this.expectedByMethod = expectedByMethod;
            this.declaredByMethod = declaredByMethod;
            this.varianceByMethod = varianceByMethod;
            this.totalSales = totalSales;
            this.totalRefunds = totalRefunds;
            this.totalCashInflows = totalCashInflows;
            this.totalCashOutflows = totalCashOutflows;
        }
    }
}