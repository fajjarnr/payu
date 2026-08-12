-- WL-001 / CB-012: immutable financial ledger enforced at the database level.
-- UPDATE/DELETE on ledger_entries and journal_entries are rejected by a trigger.
-- Escape hatch for deliberate migration backfills: run
--   SET payu.allow_ledger_mutation = 'true';
-- at the top of a Flyway migration that must rewrite ledger rows (the trigger
-- does not block DDL like ALTER TABLE). Application code never sets it.

CREATE OR REPLACE FUNCTION payu_guard_immutable_ledger()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF COALESCE(current_setting('payu.allow_ledger_mutation', true), '') <> 'true' THEN
        RAISE EXCEPTION 'Immutable ledger: % on % is not allowed (WL-001). '
                        'Use a reversal/compensation entry instead.',
                        TG_OP, TG_TABLE_NAME;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER ledger_entries_immutability
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION payu_guard_immutable_ledger();

CREATE TRIGGER journal_entries_immutability
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION payu_guard_immutable_ledger();
