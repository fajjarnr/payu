-- Persist the recovery state used by the per-leg wallet transfer saga.
ALTER TABLE split_payment_executions
    ALTER COLUMN status TYPE VARCHAR(32);
