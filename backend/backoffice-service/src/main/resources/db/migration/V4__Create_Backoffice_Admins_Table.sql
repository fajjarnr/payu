CREATE TABLE IF NOT EXISTS backoffice_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    department VARCHAR(50),
    permissions JSONB,
    created_by VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_email ON backoffice_admins(email);
CREATE INDEX IF NOT EXISTS idx_admin_username ON backoffice_admins(username);
CREATE INDEX IF NOT EXISTS idx_admin_status ON backoffice_admins(status);
