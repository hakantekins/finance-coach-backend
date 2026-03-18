-- PAYMENT_METHOD field for Transaction/Expense model
-- Stores CASH/CARD as VARCHAR to keep migrations simple.

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(10);

-- Backfill existing rows
UPDATE transactions
SET payment_method = 'CASH'
WHERE payment_method IS NULL;

-- Enforce non-null after backfill
ALTER TABLE transactions
    ALTER COLUMN payment_method SET NOT NULL;

