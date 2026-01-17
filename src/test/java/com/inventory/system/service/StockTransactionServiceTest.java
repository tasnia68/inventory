package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.StockTransaction;
import com.inventory.system.common.entity.StockTransactionStatus;
import com.inventory.system.common.entity.StockTransactionType;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.StockTransactionRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockTransactionServiceImpl stockTransactionService;

    @Test
    void createTransaction_Success() {
        CreateStockTransactionRequest request = new CreateStockTransactionRequest();
        request.setType(StockTransactionType.INBOUND);
        UUID destId = UUID.randomUUID();
        request.setDestinationWarehouseId(destId);

        CreateStockTransactionRequest.ItemRequest itemReq = new CreateStockTransactionRequest.ItemRequest();
        itemReq.setProductVariantId(UUID.randomUUID());
        itemReq.setQuantity(BigDecimal.TEN);
        request.setItems(Collections.singletonList(itemReq));

        Warehouse warehouse = new Warehouse();
        warehouse.setId(destId);
        warehouse.setName("Dest WH");

        ProductVariant variant = new ProductVariant();
        variant.setId(itemReq.getProductVariantId());
        variant.setSku("SKU-123");

        when(warehouseRepository.findById(destId)).thenReturn(Optional.of(warehouse));
        when(productVariantRepository.findById(itemReq.getProductVariantId())).thenReturn(Optional.of(variant));
        when(stockTransactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> {
            StockTransaction t = (StockTransaction) i.getArguments()[0];
            t.setId(UUID.randomUUID());
            return t;
        });

        StockTransactionDto dto = stockTransactionService.createTransaction(request);

        assertNotNull(dto.getId());
        assertEquals(StockTransactionStatus.DRAFT, dto.getStatus());
        assertEquals(StockTransactionType.INBOUND, dto.getType());
        assertEquals(destId, dto.getDestinationWarehouseId());
    }

    @Test
    void confirmTransaction_Success() {
        UUID id = UUID.randomUUID();
        StockTransaction transaction = new StockTransaction();
        transaction.setId(id);
        transaction.setStatus(StockTransactionStatus.DRAFT);
        transaction.setType(StockTransactionType.INBOUND);

        Warehouse dest = new Warehouse();
        dest.setId(UUID.randomUUID());
        dest.setName("Dest");
        transaction.setDestinationWarehouse(dest);

        when(stockTransactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(stockTransactionRepository.save(any(StockTransaction.class))).thenReturn(transaction);

        StockTransactionDto dto = stockTransactionService.confirmTransaction(id);

        assertEquals(StockTransactionStatus.COMPLETED, dto.getStatus());
        // Verify stock movements are processed implies adjustStock is called.
        // We need to add items to transaction to check loop logic, but simple test checks status transition.
    }

    @Test
    void confirmTransaction_AlreadyCompleted_ThrowsException() {
        UUID id = UUID.randomUUID();
        StockTransaction transaction = new StockTransaction();
        transaction.setId(id);
        transaction.setStatus(StockTransactionStatus.COMPLETED);

        when(stockTransactionRepository.findById(id)).thenReturn(Optional.of(transaction));

        assertThrows(BadRequestException.class, () -> stockTransactionService.confirmTransaction(id));
    }
}
