# Project Modules

Based on the project roadmap and codebase structure, the application is divided into the following modules:

## 1. Authentication & User Management
**Controllers:** `AuthController`, `UserController`, `RoleController`
**Purpose:** Handles user authentication (login), user management (CRUD, profiles), invitation system, and role-based access control (RBAC).

## 2. Tenant Management
**Controllers:** `TenantController`, `TenantSettingController`
**Purpose:** Manages multi-tenancy aspects including tenant registration, onboarding, configuration, and settings.

## 3. Product & Catalog Management
**Controllers:** `CategoryController`, `ProductTemplateController`, `ProductAttributeController`, `ProductController`, `UnitOfMeasureController`
**Purpose:** Defines the product structure, including dynamic attributes, categories, hierarchies, and units of measure.

## 4. Inventory Core
**Controllers:** `WarehouseController`, `StorageLocationController`, `StockController`, `StockTransactionController`
**Purpose:** The core of the system managing physical inventory locations (warehouses, bins), current stock levels, and fundamental stock transactions (movements).

## 5. Advanced Inventory
**Controllers:** `BatchController`, `SerialNumberController`, `StockReservationController`, `ReplenishmentController`, `CycleCountController`, `InventoryValuationController`
**Purpose:** Enterprise-grade features such as batch/lot tracking, serial number tracing, stock reservations, automated replenishment logic, cycle counting, and inventory valuation reporting.

## 6. Supplier Management
**Controllers:** `SupplierController`, `SupplierProductController`
**Purpose:** Manages supplier relationships, product sourcing details (pricing, lead times), and supplier-specific product data.
