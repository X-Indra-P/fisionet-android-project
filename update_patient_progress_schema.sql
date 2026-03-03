-- Add diagnosis_id column to patient_progress table to link progress to a specific diagnosis
-- This allows "One Diagnosis -> Many Progresses"

ALTER TABLE patient_progress
ADD COLUMN diagnosis_id bigint REFERENCES diagnosis(id);
