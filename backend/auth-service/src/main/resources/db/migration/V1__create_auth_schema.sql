-- Biometric Registrations
CREATE TABLE biometric_registrations (
    registration_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    public_key TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biometric_username ON biometric_registrations(username);
CREATE UNIQUE INDEX idx_biometric_device ON biometric_registrations(username, device_id) WHERE active = true;

-- Risk Profiles
CREATE TABLE user_risk_profiles (
    username VARCHAR(255) PRIMARY KEY,
    failed_attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_known_devices (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) REFERENCES user_risk_profiles(username),
    device_id VARCHAR(255) NOT NULL,
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(username, device_id)
);

CREATE TABLE user_known_ips (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) REFERENCES user_risk_profiles(username),
    ip_address VARCHAR(255) NOT NULL,
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(username, ip_address)
);

CREATE INDEX idx_known_devices_username ON user_known_devices(username);
CREATE INDEX idx_known_ips_username ON user_known_ips(username);
