-- ARCH-TXN-001: append-only VA payment records (immutable ledger).
-- The VA row itself only transitions status; every payment fact is an
-- immutable insert. No UPDATE/DELETE on this table, ever.

CREATE TABLE va_payment_records (
    id UUID PRIMARY KEY,
    va_id UUID NOT NULL,
    va_number VARCHAR(64) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    payment_reference VARCHAR(255),
    paid_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_va_payment_va_id ON va_payment_records(va_id);
CREATE INDEX idx_va_payment_paid_at ON va_payment_records(paid_at);
