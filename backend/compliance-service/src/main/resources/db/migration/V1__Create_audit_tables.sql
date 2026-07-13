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
    FOREIGN KEY (audit_report_id) REFERENCES audit_reports(id)
);

CREATE INDEX idx_compliance_checks_audit_report_id ON compliance_checks(audit_report_id);
CREATE INDEX idx_compliance_checks_check_id ON compliance_checks(check_id);

CREATE TABLE IF NOT EXISTS data_access_audits (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    accessed_by VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id VARCHAR(255),
    operation_type VARCHAR(50) NOT NULL,
    purpose VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000),
    accessed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_id ON data_access_audits(user_id);
CREATE INDEX idx_accessed_by ON data_access_audits(accessed_by);
CREATE INDEX idx_accessed_at ON data_access_audits(accessed_at);
CREATE INDEX idx_service_name ON data_access_audits(service_name);
