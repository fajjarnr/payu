-- ADR-0051 Support Ticket and FAQ Lifecycle: ITIL Ticket + FAQ CMS, SLA 24h, RLS tenant_id, encrypt PII (ponytail: minimal, encrypt at app layer if needed)
CREATE TABLE IF NOT EXISTS support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'payu',
    user_id VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_ticket_category CHECK (category IN ('ACCOUNT','TRANSACTION','CARD','LOAN','TECHNICAL','OTHER')),
    CONSTRAINT chk_ticket_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT chk_ticket_status CHECK (status IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER','RESOLVED','CLOSED'))
);
CREATE INDEX IF NOT EXISTS idx_tickets_user ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_tenant ON support_tickets(tenant_id);

CREATE TABLE IF NOT EXISTS faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
INSERT INTO faqs (question, answer, category) VALUES
 ('How to reset PIN?', 'Go to Settings > Security > Reset PIN and follow OTP verification.', 'ACCOUNT'),
 ('Transfer failed?', 'Check balance and recipient. Contact support if deducted not received within 24h.', 'TRANSACTION')
ON CONFLICT DO NOTHING;
