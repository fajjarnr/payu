-- RELAY-010 follow-up: permanent dev-journey FX rate seeds.
-- Without rows here GET /v1/rates/{from}/{to} always falls through to the live
-- BI provider, which is unreachable from the cluster, so the page 500s.
-- Rates are indicative dev fixtures (BI reference mid-rates, 2026-10-08);
-- source marks them as fixtures so nobody mistakes them for live quotes.
-- fx_rates has no unique key on the currency pair, so idempotency comes from
-- the NOT EXISTS guard on the fixture rows themselves.
INSERT INTO fx_rates (from_currency, to_currency, rate, inverse_rate, valid_from, valid_until, version, source, observed_at)
SELECT v.from_currency, v.to_currency, v.rate, v.inverse_rate,
       CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '365 days',
       0, 'dev-fixture', CURRENT_TIMESTAMP
FROM (VALUES
    ('IDR', 'USD', 0.0000620000::numeric, 16129.0322580645::numeric),
    ('IDR', 'EUR', 0.0000570000, 17543.8596491228),
    ('IDR', 'SGD', 0.0000840000, 11904.7619047619),
    ('IDR', 'JPY', 0.0096000000, 104.1666666667),
    ('IDR', 'GBP', 0.0000490000, 20408.1632653061),
    ('IDR', 'AUD', 0.0000950000, 10526.3157894737),
    ('IDR', 'CNY', 0.0004400000, 2272.7272727273),
    ('USD', 'IDR', 16129.0322580645, 0.0000620000),
    ('EUR', 'IDR', 17543.8596491228, 0.0000570000),
    ('SGD', 'IDR', 11904.7619047619, 0.0000840000),
    ('JPY', 'IDR', 104.1666666667, 0.0096000000),
    ('GBP', 'IDR', 20408.1632653061, 0.0000490000),
    ('AUD', 'IDR', 10526.3157894737, 0.0000950000),
    ('CNY', 'IDR', 2272.7272727273, 0.0004400000)
) AS v(from_currency, to_currency, rate, inverse_rate)
WHERE NOT EXISTS (
    SELECT 1 FROM fx_rates r WHERE r.source = 'dev-fixture'
);
