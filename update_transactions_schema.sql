-- Run this command in your Supabase SQL Editor to update the transactions table

ALTER TABLE transactions
ADD COLUMN diagnosis_id bigint,
ADD COLUMN diagnosis_name text;

-- Optional: Add foreign key constraint if you want strict referential integrity
-- ALTER TABLE transactions
-- ADD CONSTRAINT fk_transactions_diagnosis
-- FOREIGN KEY (diagnosis_id) REFERENCES diagnosis(id);
