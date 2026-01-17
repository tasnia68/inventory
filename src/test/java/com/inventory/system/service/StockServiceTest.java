package com.inventory.system.service;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SerialNumberRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StorageLocationRepository storageLocationRepository;
    @Mock
    private InventoryValuationService valuationService;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private SerialNumberRepository serialNumberRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    void adjustStock_BatchTracked_Success() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        BigDecimal quantity = BigDecimal.TEN;

        ProductTemplate template = new ProductTemplate();
        template.setIsBatchTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);
        variant.setSku("SKU-BATCH");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Main WH");

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-001");

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(quantity);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setBatchId(batchId);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        // Mock find stock to return empty, so it creates new
        when(stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchId(
                eq(variantId), eq(warehouseId), eq(batchId)))
                .thenReturn(Optional.empty());

        when(stockRepository.save(any(Stock.class))).thenAnswer(i -> {
            Stock s = (Stock) i.getArguments()[0];
            s.setId(UUID.randomUUID());
            return s;
        });

        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = (StockMovement) i.getArguments()[0];
            m.setId(UUID.randomUUID());
            return m;
        });

        StockMovementDto result = stockService.adjustStock(dto);

        assertNotNull(result);
        assertEquals(quantity, result.getQuantity());
        assertEquals(batchId, result.getBatchId());

        verify(stockRepository).save(any(Stock.class));
        verify(valuationService).processInbound(any(StockMovement.class), any());
    }

    @Test
    void adjustStock_BatchTracked_MissingBatchId_ThrowsException() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        ProductTemplate template = new ProductTemplate();
        template.setIsBatchTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(BigDecimal.TEN);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setBatchId(null); // Missing batch

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        assertThrows(IllegalArgumentException.class, () -> stockService.adjustStock(dto));
    }

    @Test
    void adjustStock_NotBatchTracked_Success() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        BigDecimal quantity = BigDecimal.TEN;

        ProductTemplate template = new ProductTemplate();
        template.setIsBatchTracked(false);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);
        variant.setSku("SKU-NORMAL");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Main WH");

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(quantity);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setBatchId(null);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        // Mock find stock for no batch
        when(stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(
                eq(variantId), eq(warehouseId)))
                .thenReturn(Optional.empty());

        when(stockRepository.save(any(Stock.class))).thenAnswer(i -> {
            Stock s = (Stock) i.getArguments()[0];
            s.setId(UUID.randomUUID());
            return s;
        });

        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = (StockMovement) i.getArguments()[0];
            m.setId(UUID.randomUUID());
            return m;
        });

        StockMovementDto result = stockService.adjustStock(dto);

        assertNotNull(result);
        assertEquals(quantity, result.getQuantity());
        assertEquals(null, result.getBatchId());
    }

    @Test
    void adjustStock_NotBatchTracked_WithBatchId_ThrowsException() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        ProductTemplate template = new ProductTemplate();
        template.setIsBatchTracked(false);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        Batch batch = new Batch();
        batch.setId(batchId);

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(BigDecimal.TEN);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setBatchId(batchId); // Provided when not needed

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        assertThrows(IllegalArgumentException.class, () -> stockService.adjustStock(dto));
    }

    @Test
    void adjustStock_SerialTracked_Success() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        BigDecimal quantity = BigDecimal.ONE;

        ProductTemplate template = new ProductTemplate();
        template.setIsSerialTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);
        variant.setSku("SKU-SERIAL");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(quantity);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setSerialNumbers(java.util.Collections.singletonList("SN-001"));

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        when(stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(
                eq(variantId), eq(warehouseId)))
                .thenReturn(Optional.empty());

        when(stockRepository.save(any(Stock.class))).thenAnswer(i -> {
            Stock s = (Stock) i.getArguments()[0];
            s.setId(UUID.randomUUID());
            return s;
        });

        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = (StockMovement) i.getArguments()[0];
            m.setId(UUID.randomUUID());
            return m;
        });

        when(serialNumberRepository.findBySerialNumberAndProductVariantId("SN-001", variantId))
            .thenReturn(Optional.empty());

        StockMovementDto result = stockService.adjustStock(dto);

        assertNotNull(result);
        verify(serialNumberRepository).save(any(SerialNumber.class));
    }

    @Test
    void adjustStock_SerialTracked_MissingSerials_ThrowsException() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        ProductTemplate template = new ProductTemplate();
        template.setIsSerialTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(BigDecimal.ONE);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setSerialNumbers(null); // Missing serials

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        assertThrows(IllegalArgumentException.class, () -> stockService.adjustStock(dto));
    }

    @Test
    void adjustStock_SerialTracked_CountMismatch_ThrowsException() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        ProductTemplate template = new ProductTemplate();
        template.setIsSerialTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(BigDecimal.valueOf(2));
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setSerialNumbers(java.util.Collections.singletonList("SN-001")); // 1 serial for qty 2

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        assertThrows(IllegalArgumentException.class, () -> stockService.adjustStock(dto));
    }
}
