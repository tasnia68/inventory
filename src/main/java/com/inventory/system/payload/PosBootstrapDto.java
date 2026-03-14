package com.inventory.system.payload;

import lombok.Data;

import java.util.List;

@Data
public class PosBootstrapDto {
    private List<PosTerminalDto> terminals;
    private List<WarehouseDto> warehouses;
    private List<CustomerDto> customers;
    private List<CategoryDto> categories;
    private PosShiftDto activeShift;
}