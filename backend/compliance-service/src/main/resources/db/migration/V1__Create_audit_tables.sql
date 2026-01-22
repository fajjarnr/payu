CREATE TABLE IF NOT EXISTS audit_reports (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    compliance_standard VARCHAR(50) NOT NULL,
    overall_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255)
);

CREATE INDEX idx_audit_reports_transaction_id ON audit_reports(transaction_id);
CREATE INDEX idx_audit_reports_merchant_id ON audit_reports(merchant_id);
CREATE INDEX idx_audit_reports_compliance_standard ON audit_reports(compliance_standard);
CREATE INDEX idx_audit_reports_created_at ON audit_reports(created_at);

CREATE TABLE IF NOT EXISTS compliance_checks (
    audit_report_id UUID NOT NULL,
    check_id VARCHAR(255) NOT NULL,
    compliance_standard VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    details TEXT,
    checked_at TIMESTAMP NOT NULL,
    FOREIGN KEY (audit_report_id) REFERENCES audit_reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_compliance_checks_audit_report_id ON compliance_checks(audit_report_id);
CREATE INDEX idx_compliance_checks_check_id ON compliance_checks(check_id);
