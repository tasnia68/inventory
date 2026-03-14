ALTER TABLE stocks ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE';

UPDATE stocks SET status = 'AVAILABLE' WHERE status IS NULL;

CREATE INDEX idx_stocks_status ON stocks(status);
CREATE INDEX idx_stocks_variant_warehouse_status ON stocks(product_variant_id, warehouse_id, status);