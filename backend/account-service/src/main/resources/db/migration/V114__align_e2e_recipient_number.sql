-- E2E contract alignment: the canonical money-journey spec
-- (frontend/web-app/e2e/money-journey.spec.ts) sends customer2 as recipient
-- account number '1001001002' (historic number from the RELAY-002 manual seed).
-- V113 invented '1001002001'; rename to the spec number so the E2E recipient
-- resolves via GetAccountByNumber.
-- Tenant context 'default': users/accounts policies (V107/V108) have no SYSTEM hatch.

SELECT set_config('app.tenant_id', 'default', false);

UPDATE accounts SET account_number = '1001001002', updated_at = CURRENT_TIMESTAMP
WHERE id = '750e8400-e29b-41d4-a716-446655440004' AND account_number = '1001002001';
