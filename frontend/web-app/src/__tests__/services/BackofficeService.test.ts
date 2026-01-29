import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  BackofficeService,
  KycStatus,
  FraudRiskLevel,
  FraudCaseStatus,
  CustomerCasePriority,
  CustomerCaseStatus,
  CustomerCaseType,
  type KycReviewResponse,
  type KycReviewDecisionRequest,
  type FraudCaseResponse,
  type FraudCaseDecisionRequest,
  type CustomerCaseResponse,
  type CustomerCaseUpdateRequest,
} from '@/services/BackofficeService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('BackofficeService', () => {
  let service: BackofficeService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = BackofficeService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = BackofficeService.getInstance();
    const instance2 = BackofficeService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('KYC Reviews', () => {
    describe('getKycReviews', () => {
      it('should fetch KYC reviews with default pagination', async () => {
        const mockReviews: KycReviewResponse[] = [
          {
            id: 'kyc_1',
            userId: 'user_1',
            accountNumber: '1234567890',
            documentType: 'KTP',
            documentNumber: '1234567890123456',
            documentUrl: 'https://example.com/doc1.pdf',
            fullName: 'John Doe',
            address: 'Jl. Sudirman No. 1',
            phoneNumber: '+628123456789',
            status: KycStatus.PENDING,
            notes: '',
            reviewedBy: '',
            reviewedAt: '',
            createdAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockReviews });

        const result = await service.getKycReviews();

        expect(api.get).toHaveBeenCalledWith('/backoffice/kyc-reviews?page=0&size=20');
        expect(result).toEqual(mockReviews);
      });

      it('should fetch KYC reviews with status filter', async () => {
        const mockReviews: KycReviewResponse[] = [];

        vi.mocked(api.get).mockResolvedValue({ data: mockReviews });

        const result = await service.getKycReviews(KycStatus.PENDING, 1, 10);

        expect(api.get).toHaveBeenCalledWith('/backoffice/kyc-reviews?status=PENDING&page=1&size=10');
        expect(result).toEqual(mockReviews);
      });

      it('should fetch KYC reviews with all status types', async () => {
        const statuses = [KycStatus.PENDING, KycStatus.APPROVED, KycStatus.REJECTED, KycStatus.REQUIRES_ADDITIONAL_INFO];

        for (const status of statuses) {
          const mockReviews: KycReviewResponse[] = [];
          vi.mocked(api.get).mockResolvedValue({ data: mockReviews });

          await service.getKycReviews(status);

          expect(api.get).toHaveBeenCalledWith(expect.stringContaining(`status=${status}`));
        }
      });
    });

    describe('getKycReview', () => {
      it('should fetch single KYC review by ID', async () => {
        const mockReview: KycReviewResponse = {
          id: 'kyc_123',
          userId: 'user_123',
          accountNumber: '1234567890',
          documentType: 'PASSPORT',
          documentNumber: 'P1234567',
          documentUrl: 'https://example.com/passport.pdf',
          fullName: 'Jane Doe',
          address: 'Jl. Thamrin No. 2',
          phoneNumber: '+628987654321',
          status: KycStatus.PENDING,
          notes: 'Need additional verification',
          reviewedBy: '',
          reviewedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockReview });

        const result = await service.getKycReview('kyc_123');

        expect(api.get).toHaveBeenCalledWith('/backoffice/kyc-reviews/kyc_123');
        expect(result).toEqual(mockReview);
      });
    });

    describe('reviewKyc', () => {
      it('should submit KYC review decision', async () => {
        const mockDecision: KycReviewDecisionRequest = {
          status: KycStatus.APPROVED,
          notes: 'All documents verified',
        };

        const mockReview: KycReviewResponse = {
          id: 'kyc_123',
          userId: 'user_123',
          accountNumber: '1234567890',
          documentType: 'KTP',
          documentNumber: '1234567890123456',
          documentUrl: 'https://example.com/doc.pdf',
          fullName: 'Test User',
          address: 'Test Address',
          phoneNumber: '+628123456789',
          status: KycStatus.APPROVED,
          notes: 'All documents verified',
          reviewedBy: 'admin_1',
          reviewedAt: '2024-01-01T11:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReview });

        const result = await service.reviewKyc('kyc_123', mockDecision);

        expect(api.post).toHaveBeenCalledWith('/backoffice/kyc-reviews/kyc_123/review', mockDecision);
        expect(result.status).toBe(KycStatus.APPROVED);
      });

      it('should submit KYC review with optional notes', async () => {
        const mockDecision: KycReviewDecisionRequest = {
          status: KycStatus.REJECTED,
        };

        const mockReview: KycReviewResponse = {
          id: 'kyc_456',
          userId: 'user_456',
          accountNumber: '0987654321',
          documentType: 'KTP',
          documentNumber: '9876543210987654',
          documentUrl: 'https://example.com/doc2.pdf',
          fullName: 'Another User',
          address: 'Another Address',
          phoneNumber: '+628987654321',
          status: KycStatus.REJECTED,
          notes: '',
          reviewedBy: 'admin_2',
          reviewedAt: '2024-01-01T12:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockReview });

        const result = await service.reviewKyc('kyc_456', mockDecision);

        expect(result.status).toBe(KycStatus.REJECTED);
      });
    });
  });

  describe('Fraud Cases', () => {
    describe('getFraudCases', () => {
      it('should fetch fraud cases with default pagination', async () => {
        const mockCases: FraudCaseResponse[] = [
          {
            id: 'fraud_1',
            userId: 'user_1',
            accountNumber: '1234567890',
            transactionId: 'tx_1',
            transactionType: 'TRANSFER',
            amount: 10000000,
            fraudType: 'ACCOUNT_TAKEOVER',
            riskLevel: FraudRiskLevel.HIGH,
            status: FraudCaseStatus.OPEN,
            description: 'Suspicious login pattern',
            evidence: 'https://example.com/evidence.pdf',
            notes: '',
            assignedTo: 'analyst_1',
            resolvedBy: '',
            resolvedAt: '',
            createdAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockCases });

        const result = await service.getFraudCases();

        expect(api.get).toHaveBeenCalledWith('/backoffice/fraud-cases?page=0&size=20');
        expect(result).toEqual(mockCases);
      });

      it('should fetch fraud cases with status and risk level filters', async () => {
        const mockCases: FraudCaseResponse[] = [];

        vi.mocked(api.get).mockResolvedValue({ data: mockCases });

        await service.getFraudCases(FraudCaseStatus.UNDER_INVESTIGATION, FraudRiskLevel.CRITICAL, 0, 50);

        expect(api.get).toHaveBeenCalledWith(
          '/backoffice/fraud-cases?status=UNDER_INVESTIGATION&riskLevel=CRITICAL&page=0&size=50'
        );
      });

      it('should fetch fraud cases with all risk levels', async () => {
        const riskLevels = [FraudRiskLevel.LOW, FraudRiskLevel.MEDIUM, FraudRiskLevel.HIGH, FraudRiskLevel.CRITICAL];

        for (const riskLevel of riskLevels) {
          const mockCases: FraudCaseResponse[] = [];
          vi.mocked(api.get).mockResolvedValue({ data: mockCases });

          await service.getFraudCases(undefined, riskLevel);

          expect(api.get).toHaveBeenCalledWith(expect.stringContaining(`riskLevel=${riskLevel}`));
        }
      });
    });

    describe('getFraudCase', () => {
      it('should fetch single fraud case by ID', async () => {
        const mockCase: FraudCaseResponse = {
          id: 'fraud_123',
          userId: 'user_123',
          accountNumber: '1234567890',
          transactionId: 'tx_123',
          transactionType: 'BI_FAST_TRANSFER',
          amount: 5000000,
          fraudType: 'PHISHING',
          riskLevel: FraudRiskLevel.MEDIUM,
          status: FraudCaseStatus.UNDER_INVESTIGATION,
          description: 'Suspected phishing attempt',
          evidence: 'https://example.com/phishing.pdf',
          notes: 'Investigating IP address',
          assignedTo: 'analyst_2',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockCase });

        const result = await service.getFraudCase('fraud_123');

        expect(api.get).toHaveBeenCalledWith('/backoffice/fraud-cases/fraud_123');
        expect(result).toEqual(mockCase);
      });
    });

    describe('resolveFraudCase', () => {
      it('should resolve fraud case', async () => {
        const mockDecision: FraudCaseDecisionRequest = {
          status: FraudCaseStatus.RESOLVED,
          notes: 'Confirmed as false positive',
        };

        const mockCase: FraudCaseResponse = {
          id: 'fraud_123',
          userId: 'user_123',
          accountNumber: '1234567890',
          transactionId: 'tx_123',
          transactionType: 'TRANSFER',
          amount: 100000,
          fraudType: 'UNUSUAL_TRANSACTION',
          riskLevel: FraudRiskLevel.LOW,
          status: FraudCaseStatus.RESOLVED,
          description: 'Unusual transaction pattern',
          evidence: '',
          notes: 'Confirmed as false positive',
          assignedTo: 'analyst_1',
          resolvedBy: 'admin_1',
          resolvedAt: '2024-01-01T15:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockCase });

        const result = await service.resolveFraudCase('fraud_123', mockDecision);

        expect(api.post).toHaveBeenCalledWith('/backoffice/fraud-cases/fraud_123/resolve', mockDecision);
        expect(result.status).toBe(FraudCaseStatus.RESOLVED);
      });

      it('should mark fraud case as false positive', async () => {
        const mockDecision: FraudCaseDecisionRequest = {
          status: FraudCaseStatus.FALSE_POSITIVE,
          notes: 'Legitimate transaction',
        };

        const mockCase: FraudCaseResponse = {
          id: 'fraud_456',
          userId: 'user_456',
          accountNumber: '0987654321',
          transactionId: 'tx_456',
          transactionType: 'QRIS_PAYMENT',
          amount: 50000,
          fraudType: 'UNUSUAL_LOCATION',
          riskLevel: FraudRiskLevel.LOW,
          status: FraudCaseStatus.FALSE_POSITIVE,
          description: 'Transaction from new location',
          evidence: '',
          notes: 'Legitimate transaction - user traveling',
          assignedTo: 'analyst_3',
          resolvedBy: 'admin_2',
          resolvedAt: '2024-01-01T16:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockCase });

        const result = await service.resolveFraudCase('fraud_456', mockDecision);

        expect(result.status).toBe(FraudCaseStatus.FALSE_POSITIVE);
      });
    });

    describe('assignFraudCase', () => {
      it('should assign fraud case to analyst', async () => {
        const mockCase: FraudCaseResponse = {
          id: 'fraud_789',
          userId: 'user_789',
          accountNumber: '1111111111',
          transactionId: 'tx_789',
          transactionType: 'TRANSFER',
          amount: 1000000,
          fraudType: 'ACCOUNT_TAKEOVER',
          riskLevel: FraudRiskLevel.HIGH,
          status: FraudCaseStatus.OPEN,
          description: 'Suspicious activity',
          evidence: '',
          notes: '',
          assignedTo: 'analyst_new',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockCase });

        const result = await service.assignFraudCase('fraud_789', 'analyst_new');

        expect(api.post).toHaveBeenCalledWith('/backoffice/fraud-cases/fraud_789/assign', expect.any(URLSearchParams));
        expect(result.assignedTo).toBe('analyst_new');
      });
    });
  });

  describe('Customer Cases', () => {
    describe('getCustomerCases', () => {
      it('should fetch customer cases with default pagination', async () => {
        const mockCases: CustomerCaseResponse[] = [
          {
            id: 'case_1',
            userId: 'user_1',
            accountNumber: '1234567890',
            caseNumber: 'CS-2024-001',
            caseType: CustomerCaseType.COMPLAINT,
            priority: CustomerCasePriority.HIGH,
            subject: 'Transaction not received',
            description: 'Transfer showing as pending',
            status: CustomerCaseStatus.OPEN,
            notes: '',
            assignedTo: 'agent_1',
            resolvedBy: '',
            resolvedAt: '',
            createdAt: '2024-01-01T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockCases });

        const result = await service.getCustomerCases();

        expect(api.get).toHaveBeenCalledWith('/backoffice/customer-cases?page=0&size=20');
        expect(result).toEqual(mockCases);
      });

      it('should fetch customer cases with status and priority filters', async () => {
        const mockCases: CustomerCaseResponse[] = [];

        vi.mocked(api.get).mockResolvedValue({ data: mockCases });

        await service.getCustomerCases(CustomerCaseStatus.IN_PROGRESS, CustomerCasePriority.URGENT, 0, 10);

        expect(api.get).toHaveBeenCalledWith('/backoffice/customer-cases?status=IN_PROGRESS&priority=URGENT&page=0&size=10');
      });

      it('should fetch customer cases with all priorities', async () => {
        const priorities = [
          CustomerCasePriority.LOW,
          CustomerCasePriority.MEDIUM,
          CustomerCasePriority.HIGH,
          CustomerCasePriority.URGENT,
        ];

        for (const priority of priorities) {
          const mockCases: CustomerCaseResponse[] = [];
          vi.mocked(api.get).mockResolvedValue({ data: mockCases });

          await service.getCustomerCases(undefined, priority);

          expect(api.get).toHaveBeenCalledWith(expect.stringContaining(`priority=${priority}`));
        }
      });
    });

    describe('getCustomerCase', () => {
      it('should fetch single customer case by ID', async () => {
        const mockCase: CustomerCaseResponse = {
          id: 'case_123',
          userId: 'user_123',
          accountNumber: '1234567890',
          caseNumber: 'CS-2024-123',
          caseType: CustomerCaseType.DISPUTE,
          priority: CustomerCasePriority.MEDIUM,
          subject: 'Unauthorized transaction',
          description: 'Transaction I did not make',
          status: CustomerCaseStatus.IN_PROGRESS,
          notes: 'Investigating with fraud team',
          assignedTo: 'agent_2',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockCase });

        const result = await service.getCustomerCase('case_123');

        expect(api.get).toHaveBeenCalledWith('/backoffice/customer-cases/case_123');
        expect(result).toEqual(mockCase);
      });
    });

    describe('updateCustomerCase', () => {
      it('should update customer case', async () => {
        const mockUpdate: CustomerCaseUpdateRequest = {
          status: CustomerCaseStatus.WAITING_FOR_CUSTOMER,
          priority: CustomerCasePriority.LOW,
          notes: 'Waiting for customer response',
        };

        const mockCase: CustomerCaseResponse = {
          id: 'case_456',
          userId: 'user_456',
          accountNumber: '0987654321',
          caseNumber: 'CS-2024-456',
          caseType: CustomerCaseType.INQUIRY,
          priority: CustomerCasePriority.LOW,
          subject: 'Account balance question',
          description: 'Balance not updating',
          status: CustomerCaseStatus.WAITING_FOR_CUSTOMER,
          notes: 'Waiting for customer response',
          assignedTo: 'agent_3',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.put).mockResolvedValue({ data: mockCase });

        const result = await service.updateCustomerCase('case_456', mockUpdate);

        expect(api.put).toHaveBeenCalledWith('/backoffice/customer-cases/case_456', mockUpdate);
        expect(result.status).toBe(CustomerCaseStatus.WAITING_FOR_CUSTOMER);
      });

      it('should update customer case with partial data', async () => {
        const mockUpdate: CustomerCaseUpdateRequest = {
          notes: 'Customer contacted',
        };

        const mockCase: CustomerCaseResponse = {
          id: 'case_789',
          userId: 'user_789',
          accountNumber: '1111111111',
          caseNumber: 'CS-2024-789',
          caseType: CustomerCaseType.REQUEST,
          priority: CustomerCasePriority.MEDIUM,
          subject: 'Feature request',
          description: 'Request new feature',
          status: CustomerCaseStatus.OPEN,
          notes: 'Customer contacted - feature requested',
          assignedTo: 'agent_4',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.put).mockResolvedValue({ data: mockCase });

        const result = await service.updateCustomerCase('case_789', mockUpdate);

        expect(result.notes).toContain('Customer contacted');
      });
    });

    describe('assignCustomerCase', () => {
      it('should assign customer case to agent', async () => {
        const mockCase: CustomerCaseResponse = {
          id: 'case_999',
          userId: 'user_999',
          accountNumber: '9999999999',
          caseNumber: 'CS-2024-999',
          caseType: CustomerCaseType.COMPLAINT,
          priority: CustomerCasePriority.HIGH,
          subject: 'High priority issue',
          description: 'Urgent issue',
          status: CustomerCaseStatus.IN_PROGRESS,
          notes: '',
          assignedTo: 'agent_new',
          resolvedBy: '',
          resolvedAt: '',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockCase });

        const result = await service.assignCustomerCase('case_999', 'agent_new');

        expect(api.post).toHaveBeenCalledWith('/backoffice/customer-cases/case_999/assign', expect.any(URLSearchParams));
        expect(result.assignedTo).toBe('agent_new');
      });
    });
  });

  describe('Error handling', () => {
    it('should handle API errors for KYC reviews', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'));

      await expect(service.getKycReviews()).rejects.toThrow('Network error');
    });

    it('should handle API errors for fraud cases', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Service unavailable'));

      await expect(service.getFraudCases()).rejects.toThrow('Service unavailable');
    });

    it('should handle API errors for customer cases', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Database error'));

      await expect(service.getCustomerCases()).rejects.toThrow('Database error');
    });
  });
});
