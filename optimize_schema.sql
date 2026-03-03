-- Optimizing ID columns to use smallint (2 bytes) instead of int/bigint
-- Recommended for tables with < 32,000 records (e.g., packages)

-- 1. Change packages.id to smallint
ALTER TABLE packages 
ALTER COLUMN id TYPE smallint;

-- 2. Change transactions.package_id to match (Required for Foreign Key consistency)
ALTER TABLE transactions 
ALTER COLUMN package_id TYPE smallint;

-- Note: smallint range is -32,768 to +32,767.
