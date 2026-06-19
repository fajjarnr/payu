-- V21__add_shedlock_table.sql
-- ITER-53: ShedLock table for distributed locking of @Scheduled methods.
-- Prevents double-execution when running multiple replicas.

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
