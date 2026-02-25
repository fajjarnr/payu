import api from '@/lib/api';

// --- Interfaces matching backend ComplianceAuditController + GdprAuditController ---
// XBUG-083 FIX: Aligned with actual backend DTOs (AuditReportResponse, AuditReportRequest)

export type ComplianceStandard = 'PCI_DSS' | 'OJK' | 'AML' | 'CFT' | 'GDPR';
export type ComplianceCheckResult = 'PASS' | 'FAIL' | 'WARNING' | 'NOT_APPLICABLE';

export interface ComplianceCheckItem {
  checkId: string;
  standard: ComplianceStandard;
  description: string;
  status: ComplianceCheckResult;
  details?: string;
  checkedAt: string;
}

export interface AuditReport {
  id: string;
  transactionId: string;
  merchantId: string;
  standard: ComplianceStandard;
  checks: ComplianceCheckItem[];
  overallStatus: ComplianceCheckResult;
  createdAt: string;
  createdBy: string;
}

export interface CreateAuditReportRequest {
  transactionId: string;
  merchantId: string;
  standard: ComplianceStandard;
  checks: Omit<ComplianceCheckItem, 'checkedAt'>[];
}

export interface GdprAudit {
  auditId: string;
  userId: string;
  accessedBy: string;
  operationType: string;
  serviceName: string;
  dataCategory: string;
  legalBasis: string;
  success: boolean;
  timestamp: string;
  details?: string;
}

export interface CreateGdprAuditRequest {
  userId: string;
  accessedBy: string;
  operationType: string;
  serviceName: string;
  dataCategory: string;
  legalBasis: string;
  success: boolean;
  details?: string;
}

export interface GdprSearchCriteria {
  userId?: string;
  accessedBy?: string;
  operationType?: string;
  serviceName?: string;
  startDate?: string;
  endDate?: string;
}

// Legacy types preserved for compat (renamed to avoid collision with ComplianceCheckItem)
export interface LegacyComplianceCheck {
  id: string;
  userId: string;
  type: ComplianceCheckType;
  status: ComplianceStatus;
  result?: string;
  riskScore: number;
  checkedAt: string;
  expiresAt?: string;
}
export interface SanctionsScreening {
  id: string;
  userId: string;
  matched: boolean;
  matchDetails?: string[];
  screenedAt: string;
}
export interface RiskAssessment {
  userId: string;
  overallRisk: RiskLevel;
  transactionRisk: RiskLevel;
  identityRisk: RiskLevel;
  lastAssessedAt: string;
}

export type ComplianceCheckType = 'AML' | 'CFT' | 'SANCTIONS' | 'PEP' | 'RISK_ASSESSMENT';
export type ComplianceStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'REVIEW_REQUIRED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

class ComplianceService {
  private static instance: ComplianceService;

  static getInstance(): ComplianceService {
    if (!ComplianceService.instance) {
      ComplianceService.instance = new ComplianceService();
    }
    return ComplianceService.instance;
  }

  // === Compliance Audit Report ===

  /** POST /compliance/audit-report — Create audit report */
  async createAuditReport(request: CreateAuditReportRequest): Promise<AuditReport> {
    const response = await api.post('/compliance/audit-report', request);
    return response.data;
  }

  /** GET /compliance/audit-report/{id} — Get audit report by ID */
  async getAuditReport(id: string): Promise<AuditReport> {
    const response = await api.get(`/compliance/audit-report/${id}`);
    return response.data;
  }

  /** GET /compliance/audit-report — Search audit reports (requires at least one filter) */
  async searchAuditReports(params: {
    transactionId?: string;
    merchantId?: string;
    standard?: ComplianceStandard;
    fromDate?: string;
    toDate?: string;
  }): Promise<AuditReport[]> {
    const response = await api.get('/compliance/audit-report', { params });
    return response.data;
  }

  // === GDPR Audit ===

  /** POST /gdpr-audit — Create GDPR audit entry */
  async createGdprAudit(request: CreateGdprAuditRequest): Promise<GdprAudit> {
    const response = await api.post('/gdpr-audit', request);
    return response.data;
  }

  /** GET /gdpr-audit/{auditId} — Get GDPR audit by ID */
  async getGdprAudit(auditId: string): Promise<GdprAudit> {
    const response = await api.get(`/gdpr-audit/${auditId}`);
    return response.data;
  }

  /** GET /gdpr-audit/users/{userId} — Get GDPR audits for user */
  async getUserGdprAudits(userId: string): Promise<GdprAudit[]> {
    const response = await api.get(`/gdpr-audit/users/${userId}`);
    return response.data;
  }

  /** GET /gdpr-audit/users/{userId}/date-range — Query by date range */
  async getUserGdprAuditsByDateRange(userId: string, startDate: string, endDate: string): Promise<GdprAudit[]> {
    const response = await api.get(`/gdpr-audit/users/${userId}/date-range`, {
      params: { startDate, endDate },
    });
    return response.data;
  }

  /** GET /gdpr-audit/accessed-by/{accessedBy} — Get by accessor */
  async getByAccessedBy(accessedBy: string): Promise<GdprAudit[]> {
    const response = await api.get(`/gdpr-audit/accessed-by/${accessedBy}`);
    return response.data;
  }

  /** GET /gdpr-audit/operations/{operationType} — Get by operation type */
  async getByOperationType(operationType: string): Promise<GdprAudit[]> {
    const response = await api.get(`/gdpr-audit/operations/${operationType}`);
    return response.data;
  }

  /** GET /gdpr-audit/services/{serviceName} — Get by service name */
  async getByServiceName(serviceName: string): Promise<GdprAudit[]> {
    const response = await api.get(`/gdpr-audit/services/${serviceName}`);
    return response.data;
  }

  /** GET /gdpr-audit/users/{userId}/count — Get audit count for user */
  async getUserGdprAuditCount(userId: string): Promise<number> {
    const response = await api.get(`/gdpr-audit/users/${userId}/count`);
    return response.data;
  }

  /** GET /gdpr-audit/failed-access — Get all failed access attempts */
  async getFailedAccess(): Promise<GdprAudit[]> {
    const response = await api.get('/gdpr-audit/failed-access');
    return response.data;
  }

  /** POST /gdpr-audit/search — Search GDPR audits */
  async searchGdprAudits(criteria: GdprSearchCriteria): Promise<GdprAudit[]> {
    const response = await api.post('/gdpr-audit/search', criteria);
    return response.data;
  }

  /** DELETE /gdpr-audit/{auditId} — Delete GDPR audit entry */
  async deleteGdprAudit(auditId: string): Promise<void> {
    await api.delete(`/gdpr-audit/${auditId}`);
  }
}

export default ComplianceService.getInstance();
