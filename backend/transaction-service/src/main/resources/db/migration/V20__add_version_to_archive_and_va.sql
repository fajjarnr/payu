-- V20__add_version_to_archive_and_va.sql
-- ITER-52: Add @Version column to TransactionArchive + VirtualAccount entities.

ALTER TABLE transaction_archives ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE virtual_accounts    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE transaction_archives SET version = 0 WHERE version IS NULL;
UPDATE virtual_accounts    SET version = 0 WHERE version IS NULL;
