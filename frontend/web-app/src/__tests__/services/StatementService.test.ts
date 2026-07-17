import { describe, it, expect, vi, beforeEach } from 'vitest';
import { StatementService, type Statement, type StatementGenerationRequest, type StatementsListResponse } from '@/services/StatementService';
import api, { isAxiosError } from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  isAxiosError: vi.fn(),
}));

const mockStatement: Statement = {
  id: 'stmt_001',
  customerId: 'cust_123',
  accountNumber: '1234567890',
  statementPeriod: '2026-01',
  openingBalance: 10000000,
  closingBalance: 15000000,
  totalCredits: 8000000,
  totalDebits: 3000000,
  transactionCount: 25,
  status: 'COMPLETED',
  generatedAt: '2026-02-01T10:00:00Z',
  createdAt: '2026-02-01T10:00:00Z',
  periodFormatted: 'Januari 2026',
  openingBalanceFormatted: 'Rp 10.000.000',
  closingBalanceFormatted: 'Rp 15.000.000',
  totalCreditsFormatted: 'Rp 8.000.000',
  totalDebitsFormatted: 'Rp 3.000.000',
  downloadUrl: '/statements/stmt_001/download',
};

describe('StatementService', () => {
  let service: StatementService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = StatementService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = StatementService.getInstance();
    const instance2 = StatementService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('generateStatement', () => {
    it('should generate a statement for given period', async () => {
      const request: StatementGenerationRequest = {
        customerId: 'cust_123',
        accountNumber: '1234567890',
        year: 2026,
        month: 1,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockStatement });

      const result = await service.generateStatement(request);

      expect(api.post).toHaveBeenCalledWith('/statements/generate', request);
      expect(result.id).toBe('stmt_001');
      expect(result.status).toBe('COMPLETED');
    });
  });

  describe('getStatement', () => {
    it('should fetch statement by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockStatement });

      const result = await service.getStatement('stmt_001');

      expect(api.get).toHaveBeenCalledWith('/statements/stmt_001');
      expect(result.periodFormatted).toBe('Januari 2026');
    });
  });

  describe('listStatements', () => {
    it('should list statements with default pagination', async () => {
      const mockListResponse: StatementsListResponse = {
        content: [mockStatement],
        totalPages: 1,
        totalElements: 1,
        size: 12,
        number: 0,
        first: true,
        last: true,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockListResponse });

      const result = await service.listStatements();

      expect(api.get).toHaveBeenCalledWith('/statements', {
        params: { page: 0, size: 12, sort: 'statementPeriod,desc' },
      });
      expect(result.content).toHaveLength(1);
      expect(result.totalElements).toBe(1);
    });

    it('should support custom pagination', async () => {
      const mockListResponse = { content: [], totalPages: 5, totalElements: 50, size: 10, number: 2, first: false, last: false };
      vi.mocked(api.get).mockResolvedValue({ data: mockListResponse });

      await service.listStatements(2, 10);

      expect(api.get).toHaveBeenCalledWith('/statements', {
        params: { page: 2, size: 10, sort: 'statementPeriod,desc' },
      });
    });
  });

  describe('getLatestStatement', () => {
    it('should fetch latest statement', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockStatement });

      const result = await service.getLatestStatement();

      expect(api.get).toHaveBeenCalledWith('/statements/latest');
      expect(result).toEqual(mockStatement);
    });

    it('should return null when no statements exist (404)', async () => {
      const axiosError = { response: { status: 404 } };
      vi.mocked(api.get).mockRejectedValue(axiosError);
      vi.mocked(isAxiosError).mockReturnValue(true);

      const result = await service.getLatestStatement();

      expect(result).toBeNull();
    });

    it('should re-throw non-404 errors', async () => {
      const axiosError = { response: { status: 500 } };
      vi.mocked(api.get).mockRejectedValue(axiosError);
      vi.mocked(isAxiosError).mockReturnValue(true);

      await expect(service.getLatestStatement()).rejects.toEqual(axiosError);
    });
  });

  describe('downloadStatement', () => {
    it('should download statement as blob', async () => {
      const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
      vi.mocked(api.get).mockResolvedValue({ data: mockBlob });

      const result = await service.downloadStatement('stmt_001');

      expect(api.get).toHaveBeenCalledWith('/statements/stmt_001/download', {
        responseType: 'blob',
      });
      expect(result).toBeInstanceOf(Blob);
    });
  });

  describe('formatPeriodType', () => {
    it('should format monthly period type', () => {
      expect(service.formatPeriodType('monthly')).toBe('Bulanan');
    });

    it('should format quarterly period type', () => {
      expect(service.formatPeriodType('quarterly')).toBe('Kuartalan');
    });

    it('should format annually period type', () => {
      expect(service.formatPeriodType('annually')).toBe('Tahunan');
    });
  });

  describe('formatStatementStatus', () => {
    it('should format GENERATING status', () => {
      expect(service.formatStatementStatus('GENERATING')).toBe('Sedang Dibuat');
    });

    it('should format COMPLETED status', () => {
      expect(service.formatStatementStatus('COMPLETED')).toBe('Siap Diunduh');
    });

    it('should format FAILED status', () => {
      expect(service.formatStatementStatus('FAILED')).toBe('Gagal');
    });
  });

  describe('getStatusColor', () => {
    it('should return warning colors for GENERATING', () => {
      const color = service.getStatusColor('GENERATING');
      expect(color).toContain('warning');
    });

    it('should return primary colors for COMPLETED', () => {
      const color = service.getStatusColor('COMPLETED');
      expect(color).toContain('primary');
    });

    it('should return destructive colors for FAILED', () => {
      const color = service.getStatusColor('FAILED');
      expect(color).toContain('destructive');
    });
  });
});
