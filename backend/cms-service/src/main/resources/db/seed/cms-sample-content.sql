-- Run manually in disposable development/test environments only.
INSERT INTO cms_contents (
    content_type, title, description, image_url, action_url, action_type,
    start_date, end_date, priority, status, targeting_rules, metadata,
    created_by, updated_by
) VALUES
('BANNER', 'Welcome Bonus Promo', 'Get Rp 50.000 bonus on your first transaction',
 'https://cdn.payu.fajjjar.my.id/images/welcome-bonus.png',
 'https://payu.fajjjar.my.id/promos/welcome-bonus', 'LINK', CURRENT_DATE,
 CURRENT_DATE + INTERVAL '30 days', 100, 'ACTIVE',
 '{"segment": "NEW_USER", "location": "ALL", "device": "ALL"}'::jsonb,
 '{"campaign": "WELCOME_2026", "abTest": "A"}'::jsonb,
 'admin@payu.fajjjar.my.id', 'admin@payu.fajjjar.my.id'),
('PROMO', 'Weekend Cashback', '20% cashback on all transactions this weekend',
 'https://cdn.payu.fajjjar.my.id/images/weekend-cashback.png',
 'https://payu.fajjjar.my.id/promos/weekend', 'DEEP_LINK', CURRENT_DATE,
 CURRENT_DATE + INTERVAL '7 days', 90, 'ACTIVE',
 '{"segment": "ALL", "location": "ALL", "device": "MOBILE"}'::jsonb,
 '{"campaign": "WEEKEND_2026"}'::jsonb,
 'admin@payu.fajjjar.my.id', 'admin@payu.fajjjar.my.id'),
('ALERT', 'Scheduled Maintenance', 'System maintenance on Sunday 2-4 AM WIB',
 NULL, NULL, 'DISMISS', CURRENT_DATE, CURRENT_DATE + INTERVAL '3 days', 200,
 'SCHEDULED', '{"segment": "ALL", "location": "ALL", "device": "ALL"}'::jsonb,
 '{"type": "SYSTEM_ALERT"}'::jsonb,
 'admin@payu.fajjjar.my.id', 'admin@payu.fajjjar.my.id');
