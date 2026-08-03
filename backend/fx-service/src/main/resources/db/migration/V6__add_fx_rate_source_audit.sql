ALTER TABLE fx_rates ADD COLUMN IF NOT EXISTS source VARCHAR(100);
ALTER TABLE fx_rates ADD COLUMN IF NOT EXISTS observed_at TIMESTAMP;

UPDATE fx_rates
SET source = 'legacy-import', observed_at = valid_from
WHERE source IS NULL OR observed_at IS NULL;

ALTER TABLE fx_rates ALTER COLUMN source SET NOT NULL;
ALTER TABLE fx_rates ALTER COLUMN observed_at SET NOT NULL;
