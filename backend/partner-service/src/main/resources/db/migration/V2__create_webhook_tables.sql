-- GAP-001: Outbound webhook subscriptions and delivery tracking
-- Enables partners to receive real-time event notifications via HTTP POST

CREATE TABLE webhook_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    partner_id      BIGINT NOT NULL REFERENCES partners(id) ON DELETE CASCADE,
    url             VARCHAR(2048) NOT NULL,
    events          VARCHAR(1024) NOT NULL,
    secret          VARCHAR(512) NOT NULL,
    description     VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries     INTEGER NOT NULL DEFAULT 5,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_webhook_partner_url UNIQUE (partner_id, url)
);

CREATE TABLE webhook_deliveries (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_id        VARCHAR(64) NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    max_attempts    INTEGER NOT NULL DEFAULT 5,
    last_attempt_at TIMESTAMP,
    next_retry_at   TIMESTAMP,
    response_code   INTEGER,
    response_body   VARCHAR(2048),
    error_message   VARCHAR(1024),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at    TIMESTAMP
);

-- Indexes for webhook_subscriptions
CREATE INDEX idx_webhook_sub_partner ON webhook_subscriptions(partner_id);
CREATE INDEX idx_webhook_sub_active ON webhook_subscriptions(active);

-- Indexes for webhook_deliveries
CREATE INDEX idx_delivery_subscription ON webhook_deliveries(subscription_id);
CREATE INDEX idx_delivery_status ON webhook_deliveries(status);
CREATE INDEX idx_delivery_next_retry ON webhook_deliveries(next_retry_at);
CREATE INDEX idx_delivery_event_id ON webhook_deliveries(event_id);

-- Composite index for retry processor query
CREATE INDEX idx_delivery_retry_eligible
    ON webhook_deliveries(status, next_retry_at);
