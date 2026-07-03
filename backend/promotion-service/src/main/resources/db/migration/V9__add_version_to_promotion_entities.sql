-- V9__add_version_to_promotion_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to promotion-service entities.

ALTER TABLE promotions                          ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE referrals                           ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customer_segments                   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rewards                             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE segment_memberships                 ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE loyalty_points                      ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cashbacks                           ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE promotions                          SET version = 0 WHERE version IS NULL;
UPDATE referrals                           SET version = 0 WHERE version IS NULL;
UPDATE customer_segments                   SET version = 0 WHERE version IS NULL;
UPDATE rewards                             SET version = 0 WHERE version IS NULL;
UPDATE segment_memberships                 SET version = 0 WHERE version IS NULL;
UPDATE loyalty_points                      SET version = 0 WHERE version IS NULL;
UPDATE cashbacks                           SET version = 0 WHERE version IS NULL;
