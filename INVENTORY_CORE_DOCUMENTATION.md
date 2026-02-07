# Inventory Core — Documentation

This document covers **stock levels**, **warehouses**, and **stock transactions** only. It intentionally excludes: Batches & Lots, Serial Numbers, Reservations, Replenishment, Cycle Counts, Valuation, and Advanced Inventory features.

## 1) Overview
Inventory Core manages:
- Warehouses (where stock is stored)
- Stock levels (current quantity by product variant + warehouse)
- Stock movements (audit of adjustments)
- Stock transactions (structured inbound/outbound/transfer workflows)

## 2) Business Rules (Core)
- Stock is tracked at **product variant** level.
- Stock levels are scoped to **warehouse** (and optionally storage location).
- Adjustments create **stock movements** for audit.
- Transactions are created then **confirmed** or **cancelled**.
- OUT/TRANSFER_OUT require sufficient stock; stock cannot go negative.
- ADJUSTMENT can be positive or negative; final stock cannot be negative.

## 3) Warehouses API

### 3.1 Create Warehouse
POST /api/v1/warehouses

Request:
{
  "name": "Main Warehouse",
  "location": "Dhaka",
  "type": "PRIMARY",
  "contactNumber": "+8801...",
  "isActive": true
}

Response (201):
{
  "status": 201,
  "success": true,
  "message": "Warehouse created successfully",
  "data": {
    "id": "<uuid>",
    "name": "Main Warehouse",
    "location": "Dhaka",
    "type": "PRIMARY",
    "contactNumber": "+8801...",
    "isActive": true,
    "createdAt": "2026-02-07T10:00:00"
  }
}

**Allowed values**
- Warehouse `type`: string (no enum enforced in backend). Suggested values: `PRIMARY`, `SECONDARY`, `DISTRIBUTION`, `RETAIL`.

### 3.2 Get Warehouse by ID
GET /api/v1/warehouses/{id}

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Warehouse retrieved successfully",
  "data": { "id": "<uuid>", "name": "Main Warehouse", "type": "PRIMARY" }
}

### 3.3 List Warehouses
GET /api/v1/warehouses

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Warehouses retrieved successfully",
  "data": [ { "id": "<uuid>", "name": "Main Warehouse" } ]
}

**Pagination**: Not paged (returns full list).

### 3.4 Update Warehouse
PUT /api/v1/warehouses/{id}

Request:
{
  "name": "Main Warehouse - Updated",
  "location": "Dhaka",
  "type": "PRIMARY",
  "contactNumber": "+8801...",
  "isActive": true
}

### 3.5 Delete Warehouse
DELETE /api/v1/warehouses/{id}

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Warehouse deleted successfully",
  "data": null
}

## 4) Stock Levels API

### 4.1 Get Stock Levels
GET /api/v1/stocks?warehouseId={warehouseId}&productVariantId={variantId}

Both params are optional. Results are paged.

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Stocks retrieved successfully",
  "data": {
    "content": [
      {
        "id": "<uuid>",
        "productVariantId": "<uuid>",
        "productVariantSku": "SKU-001",
        "warehouseId": "<uuid>",
        "warehouseName": "Main Warehouse",
        "quantity": 120,
        "createdAt": "2026-02-07T10:00:00"
      }
    ]
  }
}

**Pagination fields** in list responses (Spring `Page`):
- `content` (array)
- `totalElements`
- `totalPages`
- `size`
- `number`
- `first`
- `last`
- `numberOfElements`
- `empty`

## 5) Stock Adjustments (Movements)

### 5.1 Adjust Stock
POST /api/v1/stocks/adjust

Request:
{
  "productVariantId": "<uuid>",
  "warehouseId": "<uuid>",
  "quantity": 50,
  "unitCost": 12.5,
  "type": "INBOUND",
  "reason": "Initial stock",
  "referenceId": "PO-123"
}

**Allowed values**
- Adjustment `type`: `IN`, `OUT`, `ADJUSTMENT`, `TRANSFER_IN`, `TRANSFER_OUT`

**Validation rules**
- `productVariantId`, `warehouseId`, `quantity`, `type` are required.
- `quantity` must be positive for `IN`, `OUT`, `TRANSFER_IN`, `TRANSFER_OUT`.
- `ADJUSTMENT` can be positive or negative.
- Stock cannot go below 0.
- `unitCost` is optional.

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Stock adjusted successfully",
  "data": {
    "id": "<uuid>",
    "productVariantId": "<uuid>",
    "productVariantSku": "SKU-001",
    "warehouseId": "<uuid>",
    "warehouseName": "Main Warehouse",
    "quantity": 50,
    "unitCost": 12.5,
    "totalCost": 625,
    "type": "INBOUND",
    "reason": "Initial stock",
    "referenceId": "PO-123",
    "createdAt": "2026-02-07T10:10:00"
  }
}

### 5.2 Get Stock Movements
GET /api/v1/stocks/movements?warehouseId={warehouseId}&productVariantId={variantId}

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Stock movements retrieved successfully",
  "data": {
    "content": [
      {
        "id": "<uuid>",
        "productVariantSku": "SKU-001",
        "quantity": 50,
        "type": "INBOUND",
        "createdAt": "2026-02-07T10:10:00"
      }
    ]
  }
}

## 6) Stock Transactions API

### 6.1 Create Transaction
POST /api/v1/stock-transactions

Request:
{
  "type": "TRANSFER",
  "sourceWarehouseId": "<uuid>",
  "destinationWarehouseId": "<uuid>",
  "reference": "TR-001",
  "notes": "Move stock to branch",
  "items": [
    {
      "productVariantId": "<uuid>",
      "quantity": 10,
      "unitCost": 12.5,
      "sourceStorageLocationId": null,
      "destinationStorageLocationId": null
    }
  ]
}

**Allowed values**
- Transaction `type`: `INBOUND`, `OUTBOUND`, `TRANSFER`, `ADJUSTMENT`
- Transaction `status`: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `COMPLETED`, `CANCELLED`

**Validation rules**
- `type` is required.
- `items` must be non‑empty.
- `items[].productVariantId` and `items[].quantity` are required.
- `items[].unitCost` is optional.
- Warehouse requirements by type:
  - `INBOUND`: destination required
  - `OUTBOUND`: source required
  - `TRANSFER`: source and destination required
  - `ADJUSTMENT`: source required

Response (201):
{
  "status": 201,
  "success": true,
  "message": "Stock transaction created successfully",
  "data": {
    "id": "<uuid>",
    "transactionNumber": "TXN-0001",
    "type": "TRANSFER",
    "status": "DRAFT",
    "sourceWarehouseId": "<uuid>",
    "destinationWarehouseId": "<uuid>",
    "items": [ { "productVariantId": "<uuid>", "quantity": 10 } ],
    "transactionDate": "2026-02-07T10:15:00"
  }
}

### 6.2 Confirm Transaction
POST /api/v1/stock-transactions/{id}/confirm

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Stock transaction confirmed successfully",
  "data": { "id": "<uuid>", "status": "CONFIRMED" }
}

**Behavior**
- Confirmation creates stock movements (IN/OUT/TRANSFER/ADJUSTMENT) via stock adjustment logic.
- Availability is checked during confirmation; insufficient stock returns error.

### 6.3 Cancel Transaction
POST /api/v1/stock-transactions/{id}/cancel

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Stock transaction cancelled successfully",
  "data": { "id": "<uuid>", "status": "CANCELLED" }
}

### 6.4 Get Transaction by ID
GET /api/v1/stock-transactions/{id}

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Success",
  "data": {
    "id": "<uuid>",
    "transactionNumber": "TRX-ABCDEFGH",
    "type": "TRANSFER",
    "status": "DRAFT",
    "sourceWarehouseId": "<uuid>",
    "destinationWarehouseId": "<uuid>",
    "reference": "TR-001",
    "notes": "Move stock",
    "transactionDate": "2026-02-07T10:15:00",
    "items": [
      {
        "id": "<uuid>",
        "productVariantId": "<uuid>",
        "productVariantSku": "SKU-001",
        "quantity": 10,
        "unitCost": 12.5
      }
    ]
  }
}

### 6.5 List Transactions
GET /api/v1/stock-transactions

Response (200):
{
  "status": 200,
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "<uuid>",
        "transactionNumber": "TRX-ABCDEFGH",
        "type": "INBOUND",
        "status": "COMPLETED",
        "transactionDate": "2026-02-07T10:15:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true,
    "numberOfElements": 1,
    "empty": false
  }
}

## 7) User Flows

### Flow A — Initial Stock Entry
1) Create warehouse
2) Create product variant
3) POST /api/v1/stocks/adjust with type=INBOUND
4) Verify with GET /api/v1/stocks

### Flow B — Stock Movement Audit
1) GET /api/v1/stocks/movements
2) Filter by warehouseId or productVariantId

### Flow C — Warehouse Transfer
1) POST /api/v1/stock-transactions (type=TRANSFER)
2) POST /api/v1/stock-transactions/{id}/confirm
3) Check levels with GET /api/v1/stocks

---

## 8) Error Responses & Status Codes

**Error response format** (standard API wrapper):
{
  "status": 400,
  "success": false,
  "message": "<error message>",
  "data": null,
  "timestamp": "2026-02-07T10:00:00"
}

**Common status codes**
- 400: validation error, insufficient stock, invalid state
- 401: unauthenticated
- 403: forbidden
- 404: not found
- 409: conflict (e.g., duplicate)
- 500: unexpected error

If you want a UI‑specific guide or CSV import format, say the word.
