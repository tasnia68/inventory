# Frontend Implementation — Phase 5: Advanced Inventory Features

This document defines the frontend scope for Phase 5 (Advanced Inventory Features) and aligns it with the implemented backend (Inventory Core V2 + Product module). It includes business flows, UI/UX guidance, API contracts, and updates to Product + Core Stock modules. It also notes gaps that existed in Inventory Core V2 but are now implemented.

---

## 0) Scope & Dependencies

**Depends on:**
- Inventory Core V2 (warehouses, storage locations, stocks, stock movements, stock transactions)
- Product module (product templates/variants, categories, UOM)

**Advanced Features Covered (Phase 5):**
- Batch & serial tracking
- Stock reservations + ATP
- Replenishment rules + suggestions + purchase requisitions
- Cycle count & physical inventory

**Newly implemented gaps in Inventory Core V2 (now available):**
- Warehouse capacity tracking
- Warehouse transfer API (dedicated endpoint)
- Batch/lot tracking endpoints
- Serial number listing endpoint
- Expiry date management endpoints
- Stock alert thresholds (min/max) API
- Stock transaction approval workflow API
- Cycle count schedule management endpoint
- Purchase requisition generation API

---

## 1) Business Flows

### 1.1 Batch & Serial Tracking
**Goal:** Track stock by batch, manage expiry, and trace serial numbers.

**Flow A — Receive stock with batch/serial:**
1) User selects product variant.
2) If batch‑tracked → pick existing batch or create new (with mfg + expiry dates).
3) If serial‑tracked → enter serial list.
4) Post stock adjustment (IN) with batchId + serialNumbers.
5) Show movement confirmation.

**Flow B — Expiry management:**
1) View expiring/expired batches list.
2) Drill into batch detail.
3) Update expiry/manufacturing dates if needed.

**Flow C — Serial traceability:**
1) Search serial number.
2) View history of movements and current location.

---

### 1.2 Stock Reservation & ATP
**Goal:** Reserve stock for orders, release reservations, and calculate availability.

**Flow:**
1) User creates a reservation for a product + warehouse (optional location + batch).
2) System validates ATP.
3) User can release reservation (manual or automatic expiry).
4) ATP read available from API for order validation.

---

### 1.3 Replenishment & Purchase Requisition
**Goal:** Define min/max rules, get suggestions, generate purchase requisitions.

**Flow A — Rule setup:**
1) Create/update replenishment rule (min, max, reorder qty, safety stock, lead time).
2) Enable/disable rule.

**Flow B — Suggestions:**
1) View suggestions by warehouse.
2) Inspect suggested quantity and current stock.

**Flow C — Purchase requisition generation:**
1) Generate requisition from warehouse suggestions.
2) Review items and quantities.
3) Export or forward to purchasing workflow.

---

### 1.4 Cycle Count & Physical Inventory
**Goal:** Schedule counts, assign users, record counts, approve variances.

**Flow:**
1) Create cycle count (warehouse, type, due date, assigned user).
2) Schedule (sets status ASSIGNED).
3) Start count → system takes stock snapshot.
4) Enter counts for items (by location/batch).
5) Finish and review variances.
6) Approve to auto‑adjust stock (non‑serial products).

---

## 2) UI/UX Modules & Screens

### 2.1 Batch & Serial Management
- **Batches List** (filters: product variant, expiry window, status expiring/expired)
- **Batch Detail** (mfg date, expiry, linked product)
- **Serial List** (filters: product variant, status)
- **Serial History** (movement timeline)

UX notes:
- Batch/serial fields are conditional by product template flags.
- Use badges for expiry states (Expiring, Expired).

### 2.2 Stock Reservations
- **Reservation Create Modal**
- **Reservations List** (filters: warehouse, product variant, status)
- **ATP Widget** inside sales order or reservation flow

### 2.3 Replenishment & Purchase Requisitions
- **Rules CRUD** (min/max, reorder qty, safety stock, lead time)
- **Suggestions List** (group by warehouse)
- **Generate PR** action (pre‑filled items)
- **PR List & Detail**

### 2.4 Cycle Counts
- **Cycle Counts List** (status, due date, assignee)
- **Create / Schedule**
- **Count Entry** (table by location/batch)
- **Review & Approve**

### 2.5 Warehouse Capacity & Transfers (Core upgrade)
- **Warehouse Capacity Panel** (capacity, used, utilization)
- **Warehouse Transfer** (simple transfer form; creates & confirms transfer)

---

## 3) API Contracts (Frontend Integration)

### 3.1 Batch & Expiry
- **GET /api/v1/batches?productVariantId={uuid}**
  - Response: List<BatchDto>
- **GET /api/v1/batches/{id}**
- **GET /api/v1/batches/expiring?days=30**
- **GET /api/v1/batches/expired**
- **PUT /api/v1/batches/{id}/expiry**
  - Body: { manufacturingDate?, expiryDate? }

### 3.2 Serial Numbers
- **GET /api/v1/serial-numbers?productVariantId={uuid}&status={AVAILABLE|SOLD|...}**
  - Response: List<SerialNumberDto>
- **GET /api/v1/serial-numbers/{serial}/history**
  - Response: List<StockMovementDto>

### 3.3 Stock Reservations & ATP
- **POST /api/v1/stock-reservations**
  - Body: StockReservationRequest
- **PUT /api/v1/stock-reservations/{id}/release**
- **GET /api/v1/stock-reservations?warehouseId={uuid}&productVariantId={uuid}**
- **GET /api/v1/stock-reservations/atp?productVariantId={uuid}&warehouseId={uuid}**

### 3.4 Replenishment & Alerts
- **POST /api/v1/replenishment/rules**
- **PUT /api/v1/replenishment/rules/{id}**
- **DELETE /api/v1/replenishment/rules/{id}**
- **GET /api/v1/replenishment/rules/{id}**
- **GET /api/v1/replenishment/rules?warehouseId={uuid}**
- **POST /api/v1/replenishment/rules/{id}/calculate**
- **GET /api/v1/replenishment/suggestions?warehouseId={uuid}**
- **GET /api/v1/stocks/alerts?warehouseId={uuid}**
  - Response: List<StockAlertDto>

### 3.5 Purchase Requisitions
- **POST /api/v1/purchase-requisitions/generate**
  - Body: { warehouseId, notes? }
  - Response: PurchaseRequisitionDto
- **GET /api/v1/purchase-requisitions/{id}**
- **GET /api/v1/purchase-requisitions** (pageable)
- **GET /api/v1/purchase-requisitions/by-warehouse?warehouseId={uuid}**

### 3.6 Cycle Counts
- **POST /api/v1/cycle-counts**
- **POST /api/v1/cycle-counts/{id}/schedule**
- **POST /api/v1/cycle-counts/{id}/start**
- **GET /api/v1/cycle-counts** (pageable)
- **GET /api/v1/cycle-counts/{id}**
- **GET /api/v1/cycle-counts/{id}/items**
- **POST /api/v1/cycle-counts/{id}/entries**
- **POST /api/v1/cycle-counts/{id}/finish**
- **POST /api/v1/cycle-counts/{id}/approve**

### 3.7 Warehouse Capacity & Transfers (Core upgrade)
- **GET /api/v1/warehouses/{id}/capacity**
- **PUT /api/v1/warehouses/{id}/capacity**
- **POST /api/v1/warehouse-transfers**

### 3.8 Stock Transactions Approval (Core upgrade)
- **POST /api/v1/stock-transactions/{id}/submit-approval**
- **POST /api/v1/stock-transactions/{id}/approve**
- **POST /api/v1/stock-transactions/{id}/reject**

### 3.9 Core Stock Adjustments (for batch/serial)
- **POST /api/v1/stocks/adjust**
  - Body additions:
    - batchId (required if product is batch‑tracked)
    - serialNumbers[] (required if product is serial‑tracked)

---

## 3.10 API Request/Response Examples

> **Response wrapper:** All endpoints return `ApiResponse<T>`.
> Example:
> {
>   "success": true,
>   "status": 200,
>   "message": "...",
>   "data": { ... },
>   "timestamp": "2026-02-07T12:34:56"
> }

### Batch & Expiry
**GET /api/v1/batches?productVariantId=...**
Response `data`:
[
  {
    "id": "uuid",
    "batchNumber": "B-001",
    "manufacturingDate": "2026-01-01",
    "expiryDate": "2027-01-01",
    "productVariantId": "uuid"
  }
]

**PUT /api/v1/batches/{id}/expiry**
Request:
{
  "manufacturingDate": "2026-01-01",
  "expiryDate": "2027-01-01"
}

### Serial Numbers
**GET /api/v1/serial-numbers?productVariantId=...&status=AVAILABLE**
Response `data`:
[
  {
    "id": "uuid",
    "serialNumber": "SN-0001",
    "productVariantId": "uuid",
    "productVariantSku": "SKU-001",
    "warehouseId": "uuid",
    "warehouseName": "Main",
    "storageLocationId": "uuid",
    "storageLocationName": "A-01",
    "batchId": "uuid",
    "batchNumber": "B-001",
    "status": "AVAILABLE",
    "createdAt": "2026-02-07T10:00:00",
    "updatedAt": "2026-02-07T10:00:00"
  }
]

### Stock Reservations & ATP
**POST /api/v1/stock-reservations**
Request:
{
  "productVariantId": "uuid",
  "warehouseId": "uuid",
  "storageLocationId": "uuid",
  "batchId": "uuid",
  "quantity": 5,
  "expiresAt": "2026-02-08T10:00:00",
  "priority": "MEDIUM",
  "referenceId": "SO-1001",
  "notes": "Reserve for order"
}

**GET /api/v1/stock-reservations/atp?productVariantId=...&warehouseId=...**
Response `data`:
25.0

### Replenishment Rules & Alerts
**POST /api/v1/replenishment/rules**
Request:
{
  "productVariantId": "uuid",
  "warehouseId": "uuid",
  "minStock": 10,
  "maxStock": 100,
  "reorderQuantity": 50,
  "safetyStock": 5,
  "leadTimeDays": 7,
  "isEnabled": true
}

**GET /api/v1/stocks/alerts?warehouseId=...**
Response `data`:
[
  {
    "productVariantId": "uuid",
    "productVariantName": "Widget - SKU-001",
    "sku": "SKU-001",
    "warehouseId": "uuid",
    "warehouseName": "Main",
    "currentStock": 8,
    "minStock": 10,
    "maxStock": 100,
    "suggestedQuantity": 92,
    "status": "BELOW_MIN"
  }
]

### Purchase Requisitions
**POST /api/v1/purchase-requisitions/generate**
Request:
{
  "warehouseId": "uuid",
  "notes": "Auto-generated from replenishment"
}
Response `data`:
{
  "id": "uuid",
  "reference": "PR-0001",
  "warehouseId": "uuid",
  "warehouseName": "Main",
  "status": "DRAFT",
  "notes": "Auto-generated from replenishment",
  "requestedAt": "2026-02-07T12:00:00",
  "items": [
    {
      "id": "uuid",
      "productVariantId": "uuid",
      "productVariantName": "Widget",
      "sku": "SKU-001",
      "quantity": 50,
      "suggestedQuantity": 50
    }
  ],
  "createdAt": "2026-02-07T12:00:00",
  "updatedAt": "2026-02-07T12:00:00"
}

### Cycle Counts
**POST /api/v1/cycle-counts**
Request:
{
  "warehouseId": "uuid",
  "type": "FULL",
  "dueDate": "2026-02-10",
  "description": "Monthly count",
  "assignedUserId": "uuid"
}

**POST /api/v1/cycle-counts/{id}/entries**
Request:
[
  {
    "productVariantId": "uuid",
    "storageLocationId": "uuid",
    "batchId": "uuid",
    "countedQuantity": 95,
    "notes": "Adjusted for damage"
  }
]

### Warehouse Capacity & Transfers
**PUT /api/v1/warehouses/{id}/capacity**
Request:
{
  "capacity": 1000,
  "usedCapacity": 250
}

**POST /api/v1/warehouse-transfers**
Request:
{
  "sourceWarehouseId": "uuid",
  "destinationWarehouseId": "uuid",
  "reference": "TR-1001",
  "notes": "Move to branch",
  "items": [
    {
      "productVariantId": "uuid",
      "quantity": 10,
      "unitCost": 12.5,
      "sourceStorageLocationId": "uuid",
      "destinationStorageLocationId": "uuid"
    }
  ]
}

### Stock Transaction Approval
**POST /api/v1/stock-transactions/{id}/submit-approval**
Response `data`: StockTransactionDto

**POST /api/v1/stock-transactions/{id}/approve**
Response `data`: StockTransactionDto

**POST /api/v1/stock-transactions/{id}/reject**
Response `data`: StockTransactionDto

---

## 4) Update Notes for Product & Core Stock Modules

### Product Module (Frontend)
- Product templates should expose flags: isBatchTracked, isSerialTracked.
- Variant detail should display batch/serial tracking status.
- Create/receive/adjust stock screens must conditionally show batch + serial fields.

### Core Stock Module (Frontend)
- Stock list should show batch + serial context where applicable.
- Movement history should include batch number and serial list.
- Add stock alerts dashboard widget (min/max thresholds).
- Add warehouse capacity widget on warehouse details page.

---

## 5) UX Validation Rules (Frontend)
- If batch‑tracked and no batch selected → block submission.
- If serial‑tracked:
  - Quantity must be integer.
  - serialNumbers.length must equal |quantity|.
- Expiry date cannot be earlier than manufacturing date.
- Warehouse transfer requires source + destination warehouses and items.
- Cycle count approval is blocked for serial‑tracked items (manual adjustment required).

---

## 6) Edge Cases
- Transfers should not allow negative stock; show validation error from API.
- Reservations should fail if ATP is insufficient.
- Expired batches should be visually flagged and optionally filtered out for issue/transfer.

---

## 7) Deliverables Checklist
- UI screens wired to APIs listed above.
- Forms with conditional validation (batch/serial, ATP, cycle count).
- Dashboard widgets (alerts + capacity).
- Detailed views for batches, serials, cycle counts, and requisitions.

---

## 8) Related Docs
- Inventory Core API details: [INVENTORY_CORE_DOCUMENTATION.md](INVENTORY_CORE_DOCUMENTATION.md)
- Inventory Core V2 scope: [INVENTORY_CORE_V2.md](INVENTORY_CORE_V2.md)
- Product module docs: [MODULE_03_PRODUCT.md](MODULE_03_PRODUCT.md)
