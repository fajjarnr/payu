-- V3: Subscription & Recurring Billing tables (GAP-008)
-- Supports partner subscription plans, user subscriptions, and charge history with dunning

CREATE TABLE subscription_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id      VARCHAR(50)    NOT NULL,
    plan_name       VARCHAR(255)   NOT NULL,
    description     TEXT,
    billing_interval VARCHAR(20)   NOT NULL,
    price           NUMERIC(19, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'IDR',
    trial_days      INT            NOT NULL DEFAULT 0,
    grace_period_days INT          NOT NULL DEFAULT 0,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_plans_partner_id ON subscription_plans(partner_id);
CREATE INDEX idx_subscription_plans_active ON subscription_plans(active);

CREATE TABLE subscriptions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id            VARCHAR(50)    NOT NULL,
    plan_id               UUID           NOT NULL REFERENCES subscription_plans(id),
    partner_id            VARCHAR(50)    NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'TRIAL',
    current_price         NUMERIC(19, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL DEFAULT 'IDR',
    external_reference_id VARCHAR(100),
    trial_end_at          TIMESTAMP,
    current_period_start  TIMESTAMP,
    current_period_end    TIMESTAMP,
    next_billing_at       TIMESTAMP,
    dunning_attempts      INT            NOT NULL DEFAULT 0,
    last_charge_at        TIMESTAMP,
    cancelled_at          TIMESTAMP,
    cancellation_reason   TEXT,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_account_id ON subscriptions(account_id);
CREATE INDEX idx_subscriptions_partner_id ON subscriptions(partner_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_next_billing ON subscriptions(next_billing_at);

CREATE TABLE subscription_charges (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id     UUID           NOT NULL REFERENCES subscriptions(id),
    account_id          VARCHAR(50)    NOT NULL,
    amount              NUMERIC(19, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'IDR',
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    attempt_number      INT            NOT NULL DEFAULT 1,
    failure_reason      TEXT,
    idempotency_key     VARCHAR(255)   NOT NULL UNIQUE,
    billing_period_start TIMESTAMP,
    billing_period_end   TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_charges_subscription_id ON subscription_charges(subscription_id);
CREATE INDEX idx_subscription_charges_idempotency ON subscription_charges(idempotency_key);
CREATE INDEX idx_subscription_charges_account_id ON subscription_charges(account_id);
