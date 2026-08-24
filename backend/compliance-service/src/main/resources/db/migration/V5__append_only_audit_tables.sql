-- V5: Append-only audit trail (ADR-0063 / COMPLIANCE-HARDEN-001).
-- Audit tables must be INSERT/SELECT only for the application DB role:
-- no row may ever be updated or deleted once written.
--
-- Application role per environment (application-*.yml):
--   local/dev -> payu, container/test -> payu_test,
--   sit -> payu_sit_app, uat -> payu_uat_app, preprod -> payu_preprod_app.
-- Revoke from each role that exists; skip absent ones so this runs anywhere.
-- Never revoked: the migration/admin role that owns the tables (owners retain
-- full privileges regardless of grants), so Flyway keeps working.

DO $$
DECLARE
    app_roles TEXT[] := ARRAY['payu', 'payu_test', 'payu_sit_app', 'payu_uat_app', 'payu_preprod_app'];
    audit_tables TEXT[] := ARRAY['audit_reports', 'compliance_checks', 'data_access_audits'];
    r TEXT;
    t TEXT;
BEGIN
    FOREACH r IN ARRAY app_roles LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            FOREACH t IN ARRAY audit_tables LOOP
                EXECUTE format('REVOKE UPDATE, DELETE ON TABLE %I FROM %I', t, r);
            END LOOP;
        END IF;
    END LOOP;
END $$;
