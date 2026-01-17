package com.inventory.system.service;

import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.SerialNumberStatus;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockMovementSerialNumber;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SerialNumberRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.StockMovementSerialNumberRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockServiceSerialTest {

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
    @Mock
    private StockMovementSerialNumberRepository stockMovementSerialNumberRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    void adjustStock_SerialTracked_Inbound_Success() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        BigDecimal quantity = BigDecimal.valueOf(2);
        List<String> serials = List.of("SN1", "SN2");

        ProductTemplate template = new ProductTemplate();
        template.setIsSerialTracked(true);

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTemplate(template);
        variant.setSku("SKU-SERIAL");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setName("Main WH");

        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variantId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(quantity);
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setSerialNumbers(serials);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        // Mock stock find
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

        // Mock Serial Number lookups (return empty -> create new)
        when(serialNumberRepository.findBySerialNumberAndProductVariantId(eq("SN1"), eq(variantId))).thenReturn(Optional.empty());
        when(serialNumberRepository.findBySerialNumberAndProductVariantId(eq("SN2"), eq(variantId))).thenReturn(Optional.empty());

        when(serialNumberRepository.save(any(SerialNumber.class))).thenAnswer(i -> {
            SerialNumber sn = (SerialNumber) i.getArguments()[0];
            sn.setId(UUID.randomUUID());
            return sn;
        });

        StockMovementDto result = stockService.adjustStock(dto);

        assertNotNull(result);
        assertEquals(quantity, result.getQuantity());

        // Verify serial numbers saved
        verify(serialNumberRepository, times(2)).save(any(SerialNumber.class));
        verify(stockMovementSerialNumberRepository, times(2)).save(any(StockMovementSerialNumber.class));
    }

    @Test
    void adjustStock_SerialTracked_Outbound_Success() {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        BigDecimal quantity = BigDecimal.valueOf(1);
        List<String> serials = List.of("SN1");

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
        dto.setQuantity(quantity);
        dto.setType(StockMovement.StockMovementType.OUT);
        dto.setSerialNumbers(serials);

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        // Mock stock find (assume enough stock)
        Stock existingStock = new Stock();
        existingStock.setId(UUID.randomUUID());
        existingStock.setQuantity(BigDecimal.TEN);
        existingStock.setProductVariant(variant);
        existingStock.setWarehouse(warehouse);
        when(stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(
                eq(variantId), eq(warehouseId)))
                .thenReturn(Optional.of(existingStock));

        when(stockRepository.save(any(Stock.class))).thenAnswer(i -> i.getArguments()[0]);

        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = (StockMovement) i.getArguments()[0];
            m.setId(UUID.randomUUID());
            return m;
        });

        // Mock Serial Number lookup (must exist and be AVAILABLE)
        SerialNumber sn1 = new SerialNumber();
        sn1.setId(UUID.randomUUID());
        sn1.setSerialNumber("SN1");
        sn1.setProductVariant(variant);
        sn1.setWarehouse(warehouse);
        sn1.setStatus(SerialNumberStatus.AVAILABLE);

        when(serialNumberRepository.findBySerialNumberAndProductVariantId(eq("SN1"), eq(variantId)))
                .thenReturn(Optional.of(sn1));

        when(serialNumberRepository.save(any(SerialNumber.class))).thenAnswer(i -> i.getArguments()[0]);

        StockMovementDto result = stockService.adjustStock(dto);

        assertNotNull(result);

        // Verify serial number status updated to SOLD
        assertEquals(SerialNumberStatus.SOLD, sn1.getStatus());
        verify(stockMovementSerialNumberRepository, times(1)).save(any(StockMovementSerialNumber.class));
    }
}
