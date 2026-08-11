-- PARTNER-PROD-005: reconciliation cases for SNAP payment/refund vs wallet ledger.
-- One row per unmatched reference; OPEN until resolved by ops (reversal-only correction).
CREATE TABLE snap_reconciliation_cases (
    id              BIGSERIAL PRIMARY KEY,
    reference_type  VARCHAR(32)  NOT NULL,
    reference_id    VARCHAR(128) NOT NULL,
    detail          VARCHAR(1024),
    status          VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    detected_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP,
    tenant_id       VARCHAR(64)  NOT NULL DEFAULT 'default',
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_snap_reconciliation_case ON snap_reconciliation_cases (reference_type, reference_id);
CREATE INDEX idx_snap_reconciliation_status ON snap_reconciliation_cases (status, detected_at);
