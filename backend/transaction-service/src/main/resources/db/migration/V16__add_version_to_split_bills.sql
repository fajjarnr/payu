-- BUG-TXN-SPLITBILL-001: Add version column for JPA optimistic locking
-- (Spring Data JpaMetamodelEntityInformation.isNew() checks @Version
-- instead of @Id when the field is present, fixing the
-- "StaleObjectStateException" caused by @GeneratedValue(UUID) + no @Version
-- in combination with Spring Data 3.5 + Hibernate 6).
ALTER TABLE split_bills
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE split_bill_participants
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
