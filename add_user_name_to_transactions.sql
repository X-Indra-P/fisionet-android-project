-- Add user_name column back to transactions table to store therapist name snapshot
ALTER TABLE transactions
ADD COLUMN user_name VARCHAR(255);
