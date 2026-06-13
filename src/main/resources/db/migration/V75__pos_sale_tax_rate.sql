ALTER TABLE pos_sales
    ADD COLUMN tax_rate_id UUID,
    ADD CONSTRAINT fk_pos_sales_tax_rate
        FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(id);
