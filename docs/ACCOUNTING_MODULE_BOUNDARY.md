# Accounting Module Boundary

Phase 3 separates the accounting module from inventory-owned implementation details while preserving existing runtime behavior.

## Public Boundary

- `com.inventory.system.accounting.api.event.FinancialEventDto`
- `com.inventory.system.accounting.api.event.FinancialEventSource`
- `com.inventory.system.accounting.api.event.AccountingSourceDocumentPort`

These ports let accounting accept event-shaped input and ask for source-document facts without directly injecting inventory repositories into `AccountingServiceImpl`.

`AccountingSourceDocumentPort` returns accounting-owned scalar references for AP/AR invoices: source party ID/name, source document ID/number, default amount, and currency. It does not expose inventory entity types.

## Inventory Adapter Package

- `com.inventory.system.accounting.adapter.inventory.InventoryEventAdapter`
- `com.inventory.system.accounting.adapter.inventory.InventoryAccountingSourceDocumentAdapter`

This package is the place for inventory-owned lookups such as suppliers, customers, purchase orders, sales orders, goods receipts, and POS shifts.

## Current Decoupling State

`AccountingServiceImpl` no longer injects these repositories directly:

- `SupplierRepository`
- `CustomerRepository`
- `PurchaseOrderRepository`
- `SalesOrderRepository`
- `GoodsReceiptNoteRepository`
- `PosShiftRepository`

AP/AR invoice entities store source references as accounting-owned fields:

- AP: `sourceSystem`, `sourceDocumentType`, `sourcePartyId`, `sourcePartyName`, `sourceDocumentId`, `sourceDocumentNumber`, `supplierId`, `supplierName`, `purchaseOrderId`, `purchaseOrderNumber`
- AR: `sourceSystem`, `sourceDocumentType`, `sourcePartyId`, `sourcePartyName`, `sourceDocumentId`, `sourceDocumentNumber`, `customerId`, `customerName`, `salesOrderId`, `salesOrderNumber`

Inventory-linked AP/AR flows still populate the legacy UUID fields for compatibility. External or non-inventory callers can now create AP/AR invoices through the generic `sourceParty*` and `sourceDocument*` fields.

## Database Boundary Migrations

- `V76__ap_ar_source_references.sql` backfills display-name/number fields from inventory tables while preserving the existing legacy UUID columns.
- `V77__ap_ar_source_metadata.sql` backfills `source_system` as `INVENTORY` and derives `source_document_type` from the linked source.
- `V78__ap_ar_external_source_references.sql` drops the AP/AR foreign-key constraints to inventory tables, relaxes `supplier_id` and `customer_id`, and backfills generic source party/document fields.

## REST Contract

The current accounting REST surface is documented in `inventory/docs/accounting-openapi.yaml`.

## Remaining Optional Work

1. Move stable payload DTOs from `com.inventory.system.payload` into `com.inventory.system.accounting.api` when client imports can be coordinated.
2. Migrate existing inventory emitters from inventory-entity service methods to `FinancialEventDto` through `InventoryEventAdapter`.
3. Split accounting into a separate deployable module if the product needs independent release/runtime ownership.
