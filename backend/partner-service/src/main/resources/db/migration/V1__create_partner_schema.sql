CREATE TABLE IF NOT EXISTS partners (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    api_key VARCHAR(255),
    client_id VARCHAR(255),
    client_secret VARCHAR(255),
    public_key TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS partner_certificates (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT REFERENCES partners(id),
    certificate_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    public_key_fingerprint VARCHAR(255),
    certificate_type VARCHAR(255),
    key_algorithm VARCHAR(255),
    key_size INTEGER,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    issuer VARCHAR(255),
    subject VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_partners_client_id ON partners(client_id);
CREATE INDEX IF NOT EXISTS idx_partner_certificates_partner_id ON partner_certificates(partner_id);
