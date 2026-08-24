-- TXN-HARDEN-003/004: inbox_events (rail callback dedup) + aggregate_results (LIFO compensation)
-- ADR-0060 inbox + result table, PADG 14/2025 reconciliation, ADR-0041 outbox

CREATE TABLE IF NOT EXISTS inbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_no VARCHAR(64) NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inbox_reference_no UNIQUE (reference_no)
);
CREATE INDEX IF NOT EXISTS idx_inbox_reference_no ON inbox_events(reference_no);
CREATE INDEX IF NOT EXISTS idx_inbox_created_at ON inbox_events(created_at);

CREATE TABLE IF NOT EXISTS aggregate_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_no VARCHAR(64) NOT NULL,
    result JSONB,
    fanout_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_aggregate_results_reference_no ON aggregate_results(reference_no);
CREATE INDEX IF NOT EXISTS idx_aggregate_results_created_at ON aggregate_results(created_at);

COMMENT ON TABLE inbox_events IS 'TXN-HARDEN-003: rail callback dedup by referenceNo (inbox pattern)';
COMMENT ON TABLE aggregate_results IS 'TXN-HARDEN-003: stores rail response + fanout_order for LIFO compensation';
