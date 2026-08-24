-- V4: Align compliance_checks schema with the JPA mapping.
-- ComplianceCheck (@Embeddable) maps @Column(name = "standard"), but V1 created
-- the column as compliance_standard, so Hibernate ddl-auto=validate fails with
-- "missing column [standard] in table [compliance_checks]".
ALTER TABLE compliance_checks RENAME COLUMN compliance_standard TO standard;
