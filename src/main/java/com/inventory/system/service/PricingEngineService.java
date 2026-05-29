package com.inventory.system.service;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosTerminal;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.PricingPreviewRequest;
import com.inventory.system.payload.PricingPreviewResponse;
import com.inventory.system.payload.SalesOrderRequest;

public interface PricingEngineService {

    PricingEvaluation evaluateSalesOrder(Customer customer, Warehouse warehouse, SalesOrderRequest request);

    PricingEvaluation evaluatePosSale(Customer customer, Warehouse warehouse, PosTerminal terminal,
                                      CreatePosSaleRequest request, boolean hasManualDiscountOverride);

    void recordRedemptions(PricingEvaluation evaluation, SalesOrder salesOrder, PosSale posSale,
                           Customer customer, SalesChannel channel, String referenceNumber);

    PricingPreviewResponse preview(PricingPreviewRequest request);
}
