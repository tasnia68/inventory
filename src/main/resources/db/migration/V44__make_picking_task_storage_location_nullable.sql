-- Allow picking tasks without a storage location (stock may not have a location assigned)
ALTER TABLE picking_tasks ALTER COLUMN storage_location_id DROP NOT NULL;
