-- PAYU-TB-001: Seed TokoBapak MVP partner for SNAP-BI integration
-- clientId=tokobapak-mvp, clientSecret=tokobapak-mvp-dev-secret-32chars-long!
-- status=ACTIVE so SnapBiController.isActive() passes, type=SANDBOX bypasses dual-control (PartnerType.SANDBOX)
-- tenant_id=tokobapak for RLS isolation, partner_code unique
-- Uses plaintext fallback for EncryptedStringConverter (V18 dual-read) — next JPA write will re-encrypt to ENC(...)
-- RLS FORCE bypass: set app.tenant_id = SYSTEM for seed

SELECT set_config('app.tenant_id', 'SYSTEM', false);

INSERT INTO partners (name, type, email, phone, client_id, client_secret, active, status, partner_code, tenant_id, maker_id, checker_id, requested_at, decided_at, webhook_url)
VALUES (
    'TokoBapak MVP',
    'SANDBOX',
    'tokobapak@payu.co.id',
    '+6281234567890',
    'tokobapak-mvp',
    'tokobapak-mvp-dev-secret-32chars-long!',
    true,
    'ACTIVE',
    'TOKOBAPAK_MVP',
    'tokobapak',
    'system-maker',
    'system-checker',
    NOW(),
    NOW(),
    'http://tokobapak-notification-service:3009/v1/webhooks/payu'
) ON CONFLICT DO NOTHING;

-- Ensure partner is ACTIVE after insert (idempotent update for existing seed)
UPDATE partners SET status='ACTIVE', active=true, type='SANDBOX', tenant_id='tokobapak'
WHERE client_id='tokobapak-mvp' AND status <> 'ACTIVE';
