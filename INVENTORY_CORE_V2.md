# Inventory Core — V2 (Phase 4 Coverage)

This document maps the Phase 4 checklist to the current backend implementation and specifies what is covered vs. not yet implemented.

## Phase 4: Inventory Core Module

### 4.1 Warehouse Management
- ✅ Create Warehouse entity (name, location, type)
  - Model: Warehouse
  - API: /api/v1/warehouses
- ✅ Warehouse CRUD endpoints
  - POST/GET/PUT/DELETE /api/v1/warehouses
- ✅ StorageLocation entity (bins, racks, zones)
  - Entity exists; locations are linked to warehouse
- ✅ Warehouse–location hierarchy
  - Locations are scoped to warehouse
- ✅ Location management endpoints
  - Controller: StorageLocationController
- ⛔ Add warehouse capacity tracking
  - Not implemented
- ✅ Create warehouse transfer logic
  - Implemented via stock transactions (TRANSFER + confirm)

### 4.2 Stock Management
- ✅ Stock entity (product, warehouse, quantity, location)
- ✅ Real‑time stock level tracking
- ✅ Stock adjustment endpoints
  - POST /api/v1/stocks/adjust
- ✅ Stock movement history
  - GET /api/v1/stocks/movements
- ⛔ Batch/lot tracking (excluded)
- ⛔ Serial number tracking (excluded)
- ⛔ Expiry date management (excluded)
- ⛔ Stock alert thresholds (min/max) (not implemented)

### 4.3 Stock Transactions
- ✅ StockTransaction entity (type, quantity, reference)
- ✅ Stock‑in endpoints (INBOUND)
- ✅ Stock‑out endpoints (OUTBOUND)
- ✅ Stock transfer between warehouses (TRANSFER)
- ✅ Stock adjustment with reasons (ADJUSTMENT)
- ✅ Transaction cancellation
- ✅ Transaction audit trail (stock movements)
- ⛔ Approval workflow (not implemented)

### 4.4 Inventory Valuation
- ⛔ Valuation features are intentionally excluded from Inventory Core V2

## APIs Included (V2)

### Warehouses
- POST /api/v1/warehouses
- GET /api/v1/warehouses
- GET /api/v1/warehouses/{id}
- PUT /api/v1/warehouses/{id}
- DELETE /api/v1/warehouses/{id}

### Storage Locations
- GET /api/v1/storage-locations?warehouseId={warehouseId}
- POST /api/v1/storage-locations
- PUT /api/v1/storage-locations/{id}
- DELETE /api/v1/storage-locations/{id}

### Stock Levels
- GET /api/v1/stocks?warehouseId={warehouseId}&productVariantId={variantId}

### Stock Adjustments
- POST /api/v1/stocks/adjust
- GET /api/v1/stocks/movements?warehouseId={warehouseId}&productVariantId={variantId}

### Stock Transactions
- POST /api/v1/stock-transactions
- POST /api/v1/stock-transactions/{id}/confirm
- POST /api/v1/stock-transactions/{id}/cancel
- GET /api/v1/stock-transactions/{id}
- GET /api/v1/stock-transactions

## Business Rules (V2)
- Stock tracked per product variant per warehouse (optionally location).
- OUT/TRANSFER_OUT require sufficient stock; negative stock not allowed.
- ADJUSTMENT can be +/‑ but final stock cannot be negative.
- Transfers are created as a transaction and applied on confirm (creates movements).

## Notes
- This doc is a scope map for Phase 4 coverage. For request/response schemas and validation rules, see [INVENTORY_CORE_DOCUMENTATION.md](INVENTORY_CORE_DOCUMENTATION.md).
