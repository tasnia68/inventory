package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoodsReceiptNoteServiceImplTest {

    @Mock
    private GoodsReceiptNoteRepository grnRepository;
    @Mock
    private GoodsReceiptNoteItemRepository grnItemRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockTransactionService stockTransactionService;
    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private GoodsReceiptNoteServiceImpl grnService;

    @Test
    void createGrn_Success() {
        UUID poId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        CreateGoodsReceiptNoteRequest request = new CreateGoodsReceiptNoteRequest();
        request.setPurchaseOrderId(poId);
        request.setWarehouseId(warehouseId);
        request.setNotes("Test GRN");

        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setName("Test Supplier");

        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setSku("SKU-1");

        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setId(UUID.randomUUID());
        poItem.setProductVariant(variant);
        poItem.setQuantity(10);
        poItem.setReceivedQuantity(0);

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setPoNumber("PO-001");
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrderStatus.ISSUED);
        po.setItems(Collections.singletonList(poItem));
        poItem.setPurchaseOrder(po);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Main Warehouse");

        when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(po));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(grnRepository.save(any(GoodsReceiptNote.class))).thenAnswer(i -> {
            GoodsReceiptNote grn = (GoodsReceiptNote) i.getArguments()[0];
            grn.setId(UUID.randomUUID());
            return grn;
        });

        GoodsReceiptNoteDto result = grnService.createGrn(request);

        assertNotNull(result);
        assertEquals("PO-001", result.getPurchaseOrderNumber());
        assertEquals(GoodsReceiptNoteStatus.DRAFT, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(10, result.getItems().get(0).getReceivedQuantity());
    }

    @Test
    void createGrn_ThrowsIfPOCompleted() {
        UUID poId = UUID.randomUUID();
        CreateGoodsReceiptNoteRequest request = new CreateGoodsReceiptNoteRequest();
        request.setPurchaseOrderId(poId);

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.COMPLETED);

        when(purchaseOrderRepository.findById(poId)).thenReturn(Optional.of(po));

        assertThrows(IllegalArgumentException.class, () -> grnService.createGrn(request));
    }

    @Test
    void confirmGrn_Success() {
        UUID grnId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());

        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setQuantity(10);
        poItem.setReceivedQuantity(0);
        poItem.setUnitPrice(BigDecimal.TEN);
        poItem.setProductVariant(variant);

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setItems(Collections.singletonList(poItem));

        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("WH");

        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setId(grnId);
        grn.setGrnNumber("GRN-001");
        grn.setStatus(GoodsReceiptNoteStatus.DRAFT);
        grn.setPurchaseOrder(po);
        grn.setWarehouse(warehouse);
        Supplier s = new Supplier();
        s.setId(UUID.randomUUID());
        s.setName("Sup");
        grn.setSupplier(s);

        GoodsReceiptNoteItem grnItem = new GoodsReceiptNoteItem();
        grnItem.setId(UUID.randomUUID());
        grnItem.setPurchaseOrderItem(poItem);
        grnItem.setProductVariant(variant);
        grnItem.setReceivedQuantity(5);
        grnItem.setAcceptedQuantity(5);
        grnItem.setRejectedQuantity(0);
        grn.setItems(Collections.singletonList(grnItem));
        grnItem.setGoodsReceiptNote(grn);

        when(grnRepository.findById(grnId)).thenReturn(Optional.of(grn));

        StockTransactionDto mockTrx = new StockTransactionDto();
        mockTrx.setId(UUID.randomUUID());
        when(stockTransactionService.createTransaction(any(CreateStockTransactionRequest.class))).thenReturn(mockTrx);

        when(grnRepository.save(any(GoodsReceiptNote.class))).thenReturn(grn);

        GoodsReceiptNoteDto result = grnService.confirmGrn(grnId);

        assertEquals(GoodsReceiptNoteStatus.COMPLETED, result.getStatus());
        assertEquals(5, poItem.getReceivedQuantity());

        verify(purchaseOrderService).updatePurchaseOrderStatus(eq(poId), eq(PurchaseOrderStatus.PARTIALLY_RECEIVED));
        verify(stockTransactionService).confirmTransaction(mockTrx.getId());
    }
}
