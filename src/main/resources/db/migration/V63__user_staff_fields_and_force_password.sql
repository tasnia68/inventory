-- Adds staff-management fields to users: contact + organizational info, force-password-change flag,
-- and warehouse assignment many-to-many. Enables admin to create staff directly with a temporary
-- password that the staff member must change on first login.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS department VARCHAR(128),
    ADD COLUMN IF NOT EXISTS job_title VARCHAR(128),
    ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS users_warehouses (
    user_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    PRIMARY KEY (user_id, warehouse_id),
    CONSTRAINT fk_uw_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uw_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_uw_warehouse ON users_warehouses(warehouse_id);
