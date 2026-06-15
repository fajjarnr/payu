-- V17__add_disbursement_version.sql
-- READY-063: Add optimistic locking @Version column to disbursements.
-- Per JPA best practice (context7/spring-projects/spring-data-jpa):
-- "If a non-primitive Version-property exists, the entity is new if its value is null."
-- This allows manual id assignment to coexist with @GeneratedValue UUID by
-- routing save() to EntityManager.persist() (INSERT) when version is null.

ALTER TABLE disbursements
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Ensure existing rows have version=0
UPDATE disbursements SET version = 0 WHERE version IS NULL;
