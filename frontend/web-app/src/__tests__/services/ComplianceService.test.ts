import { describe, it, expect, vi, beforeEach } from 'vitest';
import ComplianceService, {
  type AuditReport,
  type CreateAuditReportRequest,
  type GdprAudit,
  type CreateGdprAuditRequest,
  type GdprSearchCriteria,
} from '@/services/ComplianceService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockAuditReport: AuditReport = {
  id: 'audit_001',
  transactionId: 'tx_001',
  merchantId: 'merchant_001',
  standard: 'PCI_DSS',
  checks: [{ checkId: 'chk_001', standard: 'PCI_DSS', description: 'Finding 1', status: 'PASS', checkedAt: '2026-02-18T10:00:00Z' }],
  overallStatus: 'PASS',
  createdBy: 'auditor_001',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockGdprAudit: GdprAudit = {
  auditId: 'gdpr_001',
  userId: 'user_123',
  accessedBy: 'agent_456',
  operationType: 'READ',
  serviceName: 'account-service',
  dataCategory: 'PII',
  legalBasis: 'CONSENT',
  success: true,
  timestamp: '2026-02-18T10:00:00Z',
};

describe('ComplianceService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // === Compliance Audit Report ===

  describe('createAuditReport', () => {
    it('should create an audit report', async () => {
      const request: CreateAuditReportRequest = {
        transactionId: 'tx_001',
        merchantId: 'merchant_001',
        standard: 'PCI_DSS',
        checks: [{ checkId: 'chk_001', standard: 'PCI_DSS', description: 'Check 1', status: 'PASS' }],
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockAuditReport });

      const result = await ComplianceService.createAuditReport(request);

      expect(api.post).toHaveBeenCalledWith('/compliance/audit-report', request);
      expect(result.id).toBe('audit_001');
      expect(result.overallStatus).toBe('PASS');
    });
  });

  describe('getAuditReport', () => {
    it('should fetch audit report by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAuditReport });

      const result = await ComplianceService.getAuditReport('audit_001');

      expect(api.get).toHaveBeenCalledWith('/compliance/audit-report/audit_001');
      expect(result.standard).toBe('PCI_DSS');
    });
  });

  describe('searchAuditReports', () => {
    it('should search audit reports with filters', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockAuditReport] });

      const result = await ComplianceService.searchAuditReports({ merchantId: 'merchant_001' });

      expect(api.get).toHaveBeenCalledWith('/compliance/audit-report', { params: { merchantId: 'merchant_001' } });
      expect(result).toHaveLength(1);
    });
  });

  // === GDPR Audit ===

  describe('createGdprAudit', () => {
    it('should create a GDPR audit entry', async () => {
      const request: CreateGdprAuditRequest = {
        userId: 'user_123',
        accessedBy: 'agent_456',
        operationType: 'READ',
        serviceName: 'account-service',
        dataCategory: 'PII',
        legalBasis: 'CONSENT',
        success: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockGdprAudit });

      const result = await ComplianceService.createGdprAudit(request);

      expect(api.post).toHaveBeenCalledWith('/gdpr-audit', request);
      expect(result.auditId).toBe('gdpr_001');
    });
  });

  describe('getGdprAudit', () => {
    it('should fetch GDPR audit by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockGdprAudit });

      const result = await ComplianceService.getGdprAudit('gdpr_001');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/gdpr_001');
      expect(result.success).toBe(true);
    });
  });

  describe('getUserGdprAudits', () => {
    it('should fetch GDPR audits for a user', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.getUserGdprAudits('user_123');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/users/user_123');
      expect(result).toHaveLength(1);
    });
  });

  describe('getUserGdprAuditsByDateRange', () => {
    it('should fetch GDPR audits by date range', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.getUserGdprAuditsByDateRange(
        'user_123',
        '2026-01-01',
        '2026-02-28'
      );

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/users/user_123/date-range', {
        params: { startDate: '2026-01-01', endDate: '2026-02-28' },
      });
      expect(result).toHaveLength(1);
    });
  });

  describe('getByAccessedBy', () => {
    it('should fetch audits by accessor', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.getByAccessedBy('agent_456');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/accessed-by/agent_456');
      expect(result[0].accessedBy).toBe('agent_456');
    });
  });

  describe('getByOperationType', () => {
    it('should fetch audits by operation type', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.getByOperationType('READ');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/operations/READ');
      expect(result).toHaveLength(1);
    });
  });

  describe('getByServiceName', () => {
    it('should fetch audits by service name', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.getByServiceName('account-service');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/services/account-service');
      expect(result).toHaveLength(1);
    });
  });

  describe('getUserGdprAuditCount', () => {
    it('should fetch audit count for a user', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: 42 });

      const result = await ComplianceService.getUserGdprAuditCount('user_123');

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/users/user_123/count');
      expect(result).toBe(42);
    });
  });

  describe('getFailedAccess', () => {
    it('should fetch failed access attempts', async () => {
      const failedAudit = { ...mockGdprAudit, success: false };
      vi.mocked(api.get).mockResolvedValue({ data: [failedAudit] });

      const result = await ComplianceService.getFailedAccess();

      expect(api.get).toHaveBeenCalledWith('/gdpr-audit/failed-access');
      expect(result[0].success).toBe(false);
    });
  });

  describe('searchGdprAudits', () => {
    it('should search GDPR audits by criteria', async () => {
      const criteria: GdprSearchCriteria = {
        userId: 'user_123',
        operationType: 'READ',
      };

      vi.mocked(api.post).mockResolvedValue({ data: [mockGdprAudit] });

      const result = await ComplianceService.searchGdprAudits(criteria);

      expect(api.post).toHaveBeenCalledWith('/gdpr-audit/search', criteria);
      expect(result).toHaveLength(1);
    });
  });

  describe('deleteGdprAudit', () => {
    it('should delete a GDPR audit entry', async () => {
      vi.mocked(api.delete).mockResolvedValue({});

      await ComplianceService.deleteGdprAudit('gdpr_001');

      expect(api.delete).toHaveBeenCalledWith('/gdpr-audit/gdpr_001');
    });
  });
});
