-- V46 renamed PENDING_APPROVAL -> PENDING and the SalesOrderStatus enum has grown
-- since the original constraint was written. Sync the CHECK constraint to the full
-- current enum so storefront checkout (which inserts PENDING) and the lifecycle
-- transitions for HOLD / PACKAGING / PARTIALLY_DELIVERED / PARTIALLY_CANCELLED /
-- UNASSIGNED / DELIVERY_FAILED can persist.

ALTER TABLE sales_orders DROP CONSTRAINT IF EXISTS sales_orders_status_check;

ALTER TABLE sales_orders ADD CONSTRAINT sales_orders_status_check CHECK (status IN (
    'DRAFT',
    'PENDING',
    'HOLD',
    'APPROVED',
    'CONFIRMED',
    'PACKAGING',
    'BACKORDERED',
    'PARTIALLY_SHIPPED',
    'SHIPPED',
    'DELIVERED',
    'PARTIALLY_DELIVERED',
    'PARTIALLY_CANCELLED',
    'CANCELLED',
    'RETURNED',
    'UNASSIGNED',
    'DELIVERY_FAILED'
));
