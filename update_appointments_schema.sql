-- Update data types for the appointments table based on the suggestions

-- 1. ID: int4 -> bigint (bigserial is for auto-increment, usually handled by IDENTITY)
ALTER TABLE appointments 
ALTER COLUMN id TYPE bigint;

-- 2. Created_at: timestamp -> timestamptz
ALTER TABLE appointments 
ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';

-- 3. Patient_id: int4 -> bigint (to match patients.id)
ALTER TABLE appointments 
ALTER COLUMN patient_id TYPE bigint;

-- 4. Status: text -> varchar(20) and SET DEFAULT 'Terjadwal'
ALTER TABLE appointments 
ALTER COLUMN status TYPE varchar(20);

ALTER TABLE appointments 
ALTER COLUMN status SET DEFAULT 'Terjadwal';

-- 5. Patient_name: text -> varchar(255)
ALTER TABLE appointments 
ALTER COLUMN patient_name TYPE varchar(255);

-- Note: 'date', 'time', and 'notes' are already correct or appropriate as is.
