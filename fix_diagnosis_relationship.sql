-- Ensure the Foreign Key relationship exists for PostgREST to detect it
-- This script adds the constraint if it was missing or dropped

ALTER TABLE transactions
DROP CONSTRAINT IF EXISTS fk_transactions_diagnosis;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_diagnosis
FOREIGN KEY (diagnosis_id)
REFERENCES diagnosis(id);

-- Also ensure diagnosis_id is the correct type (matching diagnosis.id)
-- If diagnosis.id is SERIAL/INT, this should be INTEGER.
-- If you reverted types to INT in Kotlin, good to keep DB consistent, but BIGINT usually works too.
-- Just strictly adding the FK is the most important part for the Relationship error.
