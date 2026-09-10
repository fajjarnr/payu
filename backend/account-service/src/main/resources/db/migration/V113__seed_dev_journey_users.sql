-- RELAY-010 + FE-AUDIT-007: permanent dev-journey User/Profile/Account seeds.
-- Links pre-existing Keycloak dev users (external_id = Keycloak sub, accountId attrs
-- in keycloak-realm-import.yaml) to local rows so ownership-gated endpoints stop 403.
-- Plaintext PII is read-safe: EncryptedStringConverter.decryptFromDatabase passes
-- through values without the ENC() wrapper (re-encrypted on next app write).
-- Mirrors V122 shape: explicit tenant + ON CONFLICT DO NOTHING per table.
-- Tenant context: users/accounts policies (V107/V108) have no SYSTEM escape hatch,
-- so seed under the 'default' tenant directly. Profiles (V112) accepts either.
SELECT set_config('app.tenant_id', 'default', false);

INSERT INTO users (id, external_id, username, email, phone_number, status, kyc_status, tenant_id, created_at, updated_at)
VALUES
    ('650e8400-e29b-41d4-a716-446655440001', '8d177115-af87-47d5-8b94-a09e7cfa55bc', 'customer1', 'customer1@payu.fajjjar.my.id', '+6281234567890', 'ACTIVE', 'VERIFIED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440004', 'a4c42cbd-9549-4467-9916-837713941c48', 'customer2', 'customer2@payu.fajjjar.my.id', '+6281234567891', 'ACTIVE', 'PENDING', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440005', 'fc1eafbb-ea56-4bde-8d16-7253297f4756', 'admin', 'admin@payu.fajjjar.my.id', '+628111111111', 'ACTIVE', 'VERIFIED', 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (external_id) DO NOTHING;

-- profiles.id shares the User PK (ProfileEntity: id = user id, no user_id column)
INSERT INTO profiles (id, full_name, nik, tenant_id)
VALUES
    ('650e8400-e29b-41d4-a716-446655440001', 'Customer One', '3201234567890001', 'default'),
    ('650e8400-e29b-41d4-a716-446655440004', 'Customer Two', '3201234567890002', 'default'),
    ('650e8400-e29b-41d4-a716-446655440005', 'System Administrator', '3201234567890005', 'default')
ON CONFLICT (id) DO NOTHING;

-- accounts.id = wallet account_id = Keycloak accountId attr (money lives in wallet-service)
INSERT INTO accounts (id, user_id, account_number, type, status, currency, balance, tenant_id, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', '1001001001', 'SAVINGS', 'ACTIVE', 'IDR', 0.00, 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('750e8400-e29b-41d4-a716-446655440004', '650e8400-e29b-41d4-a716-446655440004', '1001002001', 'SAVINGS', 'ACTIVE', 'IDR', 0.00, 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('750e8400-e29b-41d4-a716-446655440005', '650e8400-e29b-41d4-a716-446655440005', '1001009001', 'SAVINGS', 'ACTIVE', 'IDR', 0.00, 'default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
