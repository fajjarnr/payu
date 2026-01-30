-- Debezium CDC Prerequisites for PayU Platform
-- Run this script on PostgreSQL before deploying Debezium connectors

-- =====================================================
-- 1. Create Heartbeat Table (Required for all connectors)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.debezium_heartbeat (
    id INTEGER PRIMARY KEY,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert initial heartbeat
INSERT INTO public.debezium_heartbeat (id, updated_at)
VALUES (1, NOW())
ON CONFLICT (id) DO NOTHING;

-- Grant permissions
GRANT SELECT, INSERT, UPDATE ON public.debezium_heartbeat TO debezium;

-- =====================================================
-- 2. Create Publications for Logical Replication
-- =====================================================

-- Outbox publication
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication WHERE pubname = 'dbz_outbox_publication'
    ) THEN
        CREATE PUBLICATION dbz_outbox_publication FOR TABLE public.outbox_events;
    END IF;
END $$;

-- Saga publication
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication WHERE pubname = 'dbz_saga_publication'
    ) THEN
        CREATE PUBLICATION dbz_saga_publication FOR TABLE public.saga_instances, public.saga_steps;
    END IF;
END $$;

-- Wallet publication
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication WHERE pubname = 'dbz_wallet_publication'
    ) THEN
        CREATE PUBLICATION dbz_wallet_publication FOR TABLE public.wallets, public.ledger_entries, public.wallet_transactions;
    END IF;
END $$;

-- =====================================================
-- 3. Verify Publications
-- =====================================================
SELECT pubname, pubinsert, pubupdate, pubdelete, pubtruncate
FROM pg_publication
WHERE pubname LIKE 'dbz_%';

-- =====================================================
-- 4. Create Debezium User (if not exists)
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'debezium'
    ) THEN
        CREATE USER debezium WITH REPLICATION LOGIN PASSWORD 'changeme_in_production';
    END IF;
END $$;

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO debezium;

-- =====================================================
-- 5. Create Outbox Events Table (if not exists)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_processed ON public.outbox_events(processed, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate ON public.outbox_events(aggregate_type, aggregate_id);

-- =====================================================
-- 6. Create Saga Tables (if not exists)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.saga_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
    current_step INTEGER DEFAULT 0,
    payload JSONB,
    compensating_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS public.saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_instance_id UUID NOT NULL REFERENCES public.saga_instances(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    action_data JSONB,
    compensating_data JSONB,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(saga_instance_id, step_number)
);

CREATE INDEX IF NOT EXISTS idx_saga_instances_status ON public.saga_instances(status, created_at);
CREATE INDEX IF NOT EXISTS idx_saga_steps_instance ON public.saga_steps(saga_instance_id);

-- =====================================================
-- 7. Create Wallet Tables (if not exists)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    pin_hash VARCHAR(255),
    security_question_hash VARCHAR(255),
    biometric_hash VARCHAR(255),
    daily_limit DECIMAL(19, 4) DEFAULT 100000000.00,
    monthly_limit DECIMAL(19, 4) DEFAULT 1000000000.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES public.wallets(id),
    transaction_id VARCHAR(255) NOT NULL,
    entry_type VARCHAR(50) NOT NULL, -- DEBIT or CREDIT
    amount DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_ref VARCHAR(255) NOT NULL UNIQUE,
    source_wallet_id UUID REFERENCES public.wallets(id),
    destination_wallet_id UUID REFERENCES public.wallets(id),
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    metadata JSONB,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON public.wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_status ON public.wallets(status);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_wallet ON public.ledger_entries(wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction ON public.ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_source ON public.wallet_transactions(source_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_destination ON public.wallet_transactions(destination_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_idempotency ON public.wallet_transactions(idempotency_key);

-- =====================================================
-- 8. Grant Permissions to Debezium User
-- =====================================================
GRANT SELECT ON public.outbox_events TO debezium;
GRANT SELECT ON public.saga_instances TO debezium;
GRANT SELECT ON public.saga_steps TO debezium;
GRANT SELECT ON public.wallets TO debezium;
GRANT SELECT ON public.ledger_entries TO debezium;
GRANT SELECT ON public.wallet_transactions TO debezium;

-- =====================================================
-- 9. Verify Setup
-- =====================================================
SELECT 'Publications:' AS info;
SELECT pubname FROM pg_publication WHERE pubname LIKE 'dbz_%';

SELECT 'Replication Slots:' AS info;
SELECT slot_name, plugin, slot_type, active FROM pg_replication_slots WHERE slot_name LIKE 'debezium_%';

SELECT 'Tables for CDC:' AS info;
SELECT schemaname, tablename, pubname
FROM pg_publication_tables
WHERE pubname LIKE 'dbz_%'
ORDER BY pubname, tablename;
