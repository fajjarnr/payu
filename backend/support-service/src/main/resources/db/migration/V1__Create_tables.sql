-- Support Agents Table
CREATE TABLE IF NOT EXISTS support_agents (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    department VARCHAR(100) NOT NULL,
    level VARCHAR(20) NOT NULL DEFAULT 'JUNIOR',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Training Modules Table
CREATE TABLE IF NOT EXISTS training_modules (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    mandatory BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Agent Training Table
CREATE TABLE IF NOT EXISTS agent_training (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES support_agents(id) ON DELETE CASCADE,
    training_module_id BIGINT NOT NULL REFERENCES training_modules(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    score INT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (agent_id, training_module_id)
);

-- Insert initial training modules
INSERT INTO training_modules (code, title, description, category, duration_minutes, status, mandatory)
VALUES 
    ('ONBOARDING-001', 'PayU Platform Overview', 'Introduction to the PayU digital banking platform architecture and features', 'ONBOARDING', 60, 'ACTIVE', true),
    ('COMPLIANCE-001', 'Banking Compliance & Regulations', 'Understanding OJK, BI regulations, and compliance requirements', 'COMPLIANCE', 90, 'ACTIVE', true),
    ('SECURITY-001', 'Security Awareness Training', 'Data protection, fraud detection, and security best practices', 'SECURITY', 60, 'ACTIVE', true),
    ('PRODUCT-001', 'Account Management', 'Training on account creation, eKYC, and multi-pocket features', 'PRODUCT_KNOWLEDGE', 75, 'ACTIVE', true),
    ('PRODUCT-002', 'Transactions & Payments', 'Understanding BI-FAST, QRIS, and transfer flows', 'PRODUCT_KNOWLEDGE', 75, 'ACTIVE', true),
    ('SYSTEMS-001', 'Support Tools & Systems', 'Using internal support tools and CRM systems', 'SYSTEMS', 45, 'ACTIVE', false),
    ('COMMUNICATION-001', 'Customer Communication', 'Best practices for handling customer inquiries and disputes', 'COMMUNICATION', 60, 'ACTIVE', false),
    ('DISPUTE-001', 'Dispute Resolution', 'Handling transaction disputes and escalations', 'DISPUTE_RESOLUTION', 90, 'ACTIVE', false);

-- Insert initial support agents
INSERT INTO support_agents (employee_id, name, email, department, level, active)
VALUES 
    ('EMP001', 'Budi Santoso', 'budi.santoso@payu.id', 'Customer Support', 'TEAM_LEAD', true),
    ('EMP002', 'Siti Rahayu', 'siti.rahayu@payu.id', 'Customer Support', 'SENIOR', true),
    ('EMP003', 'Ahmad Faisal', 'ahmad.faisal@payu.id', 'Customer Support', 'JUNIOR', true),
    ('EMP004', 'Dewi Lestari', 'dewi.lestari@payu.id', 'Customer Support', 'SENIOR', true),
    ('EMP005', 'Rudi Hartono', 'rudi.hartono@payu.id', 'Customer Support', 'JUNIOR', true),
    ('EMP006', 'Maya Sari', 'maya.sari@payu.id', 'Customer Support', 'JUNIOR', true),
    ('EMP007', 'Andi Wijaya', 'andi.wijaya@payu.id', 'Customer Support', 'MANAGER', true),
    ('EMP008', 'Rina Kusuma', 'rina.kusuma@payu.id', 'Customer Support', 'SENIOR', true);

-- Mark initial agents as trained for all mandatory modules
INSERT INTO agent_training (agent_id, training_module_id, status, score, started_at, completed_at)
SELECT 
    a.id,
    tm.id,
    'PASSED',
    95,
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP - INTERVAL '7 days'
FROM support_agents a
CROSS JOIN training_modules tm
WHERE tm.mandatory = true AND tm.status = 'ACTIVE';
