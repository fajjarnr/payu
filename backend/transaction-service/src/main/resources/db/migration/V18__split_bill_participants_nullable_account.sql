-- V18__split_bill_participants_nullable_account.sql
-- READY-067: Split-bill participants can be created with just customerName +
-- amount. The account_id/account_name/account_number are optional at creation
-- time and get populated when the participant pays.
-- Per JPA best practice, model the schema to match the domain use case.
-- This avoids the null-constraint violation seen in production E2E.

ALTER TABLE split_bill_participants
    ALTER COLUMN account_id DROP NOT NULL,
    ALTER COLUMN account_name DROP NOT NULL,
    ALTER COLUMN account_number DROP NOT NULL;
