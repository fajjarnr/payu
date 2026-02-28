-- Create product definitions table for database-driven product configuration
-- This enables adding new products without code redeployment

CREATE TABLE IF NOT EXISTS product_definitions (
    product_code VARCHAR(50) PRIMARY KEY,
    product_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    parameters JSONB,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_product_type ON product_definitions(product_type);
CREATE INDEX IF NOT EXISTS idx_product_active ON product_definitions(active);
CREATE INDEX IF NOT EXISTS idx_product_type_active ON product_definitions(product_type, active);

-- Add comment for documentation
COMMENT ON TABLE product_definitions IS 'Product catalog definitions with JSONB parameters for flexible configuration';
COMMENT ON COLUMN product_definitions.product_code IS 'Unique product code (e.g., SAVINGS_BASIC, LOAN_PERSONAL)';
COMMENT ON COLUMN product_definitions.product_type IS 'Product category: SAVINGS, LOAN, PAYLATER, INVESTMENT, INSURANCE, CREDIT_CARD, DEPOSIT';
COMMENT ON COLUMN product_definitions.parameters IS 'JSONB field containing product-specific configuration parameters';
