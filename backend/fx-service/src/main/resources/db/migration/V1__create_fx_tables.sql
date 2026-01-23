-- FX Service Database Schema
-- Create fx_rates table
CREATE TABLE IF NOT EXISTS fx_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19,8) NOT NULL,
    inverse_rate DECIMAL(19,8) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create fx_conversions table
CREATE TABLE IF NOT EXISTS fx_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    from_amount DECIMAL(19,2) NOT NULL,
    to_amount DECIMAL(19,2) NOT NULL,
    exchange_rate DECIMAL(19,8) NOT NULL,
    fee DECIMAL(19,2),
    conversion_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_fx_rates_pair_valid ON fx_rates(from_currency, to_currency, valid_from, valid_until);
CREATE INDEX IF NOT EXISTS idx_fx_rates_valid_until ON fx_rates(valid_until);
CREATE INDEX IF NOT EXISTS idx_fx_conversions_account_id ON fx_conversions(account_id);
CREATE INDEX IF NOT EXISTS idx_fx_conversions_conversion_date ON fx_conversions(conversion_date);
CREATE INDEX IF NOT EXISTS idx_fx_conversions_status ON fx_conversions(status);
