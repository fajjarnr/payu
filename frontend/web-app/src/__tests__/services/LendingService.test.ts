import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  LendingService,
  type LoanApplicationRequest,
  type Loan,
  type RepaymentSchedule,
  type PayLater,
  type PayLaterTransaction,
  type CreditScore,
  type PayLaterLimitRequest,
  type LoanStatus,
  type PayLaterStatus,
} from '@/services/LendingService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('LendingService', () => {
  let service: LendingService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = LendingService.getInstance();
  });

  it('should be a singleton', () => {
    const instance1 = LendingService.getInstance();
    const instance2 = LendingService.getInstance();
    expect(instance1).toBe(instance2);
  });

  describe('Loan Management', () => {
    describe('applyLoan', () => {
      it('should apply for a loan successfully', async () => {
        const mockRequest: LoanApplicationRequest = {
          userId: 'user_123',
          amount: 10000000,
          tenureMonths: 12,
          purpose: 'Home renovation',
          interestRate: 12.5,
        };

        const mockLoan: Loan = {
          id: 'loan_123',
          userId: 'user_123',
          amount: 10000000,
          interestRate: 12.5,
          tenureMonths: 12,
          purpose: 'Home renovation',
          status: 'PENDING',
          monthlyPayment: 888888.89,
          totalPayment: 10666666.67,
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockLoan });

        const result = await service.applyLoan(mockRequest);

        expect(api.post).toHaveBeenCalledWith('/lending/loans', mockRequest);
        expect(result).toEqual(mockLoan);
        expect(result.status).toBe('PENDING');
      });

      it('should apply loan without optional interest rate', async () => {
        const mockRequest: LoanApplicationRequest = {
          userId: 'user_456',
          amount: 5000000,
          tenureMonths: 6,
          purpose: 'Emergency fund',
        };

        const mockLoan: Loan = {
          id: 'loan_456',
          userId: 'user_456',
          amount: 5000000,
          interestRate: 15.0,
          tenureMonths: 6,
          purpose: 'Emergency fund',
          status: 'PENDING',
          monthlyPayment: 901775.32,
          totalPayment: 5410651.92,
          createdAt: '2024-01-01T11:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockLoan });

        const result = await service.applyLoan(mockRequest);

        expect(result.interestRate).toBe(15.0);
      });
    });

    describe('getLoan', () => {
      it('should fetch loan by ID', async () => {
        const mockLoan: Loan = {
          id: 'loan_789',
          userId: 'user_789',
          amount: 20000000,
          interestRate: 10.0,
          tenureMonths: 24,
          purpose: 'Business expansion',
          status: 'APPROVED',
          monthlyPayment: 917496.36,
          totalPayment: 22019912.64,
          createdAt: '2024-01-01T10:00:00Z',
          approvedAt: '2024-01-02T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockLoan });

        const result = await service.getLoan('loan_789');

        expect(api.get).toHaveBeenCalledWith('/lending/loans/loan_789');
        expect(result).toEqual(mockLoan);
        expect(result.status).toBe('APPROVED');
      });

      it('should handle different loan statuses', async () => {
        const statuses: LoanStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'DISBURSED', 'REPAID', 'DEFAULTED'];

        for (const status of statuses) {
          const mockLoan: Loan = {
            id: `loan_${status}`,
            userId: `user_${status}`,
            amount: 1000000,
            interestRate: 12.0,
            tenureMonths: 12,
            purpose: 'Test',
            status: status,
            monthlyPayment: 88888.89,
            totalPayment: 1066666.67,
            createdAt: '2024-01-01T10:00:00Z',
          };

          vi.mocked(api.get).mockResolvedValue({ data: mockLoan });

          const result = await service.getLoan(`loan_${status}`);

          expect(result.status).toBe(status);
        }
      });
    });

    describe('createRepaymentSchedule', () => {
      it('should create repayment schedule for loan', async () => {
        const mockSchedule: RepaymentSchedule[] = [
          {
            id: 'schedule_1',
            loanId: 'loan_123',
            installmentNumber: 1,
            dueDate: '2024-02-01T00:00:00Z',
            amount: 888888.89,
            principalAmount: 783888.89,
            interestAmount: 105000.00,
            status: 'PENDING',
          },
          {
            id: 'schedule_2',
            loanId: 'loan_123',
            installmentNumber: 2,
            dueDate: '2024-03-01T00:00:00Z',
            amount: 888888.89,
            principalAmount: 791688.89,
            interestAmount: 97200.00,
            status: 'PENDING',
          },
        ];

        vi.mocked(api.post).mockResolvedValue({ data: mockSchedule });

        const result = await service.createRepaymentSchedule('loan_123');

        expect(api.post).toHaveBeenCalledWith('/lending/loans/loan_123/repayment-schedule');
        expect(result).toHaveLength(2);
        expect(result[0].installmentNumber).toBe(1);
      });
    });

    describe('getRepaymentSchedule', () => {
      it('should fetch repayment schedule for loan', async () => {
        const mockSchedule: RepaymentSchedule[] = [
          {
            id: 'schedule_1',
            loanId: 'loan_123',
            installmentNumber: 1,
            dueDate: '2024-02-01T00:00:00Z',
            amount: 888888.89,
            principalAmount: 783888.89,
            interestAmount: 105000.00,
            status: 'PAID',
            paidAt: '2024-02-01T10:00:00Z',
          },
          {
            id: 'schedule_2',
            loanId: 'loan_123',
            installmentNumber: 2,
            dueDate: '2024-03-01T00:00:00Z',
            amount: 888888.89,
            principalAmount: 791688.89,
            interestAmount: 97200.00,
            status: 'PENDING',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockSchedule });

        const result = await service.getRepaymentSchedule('loan_123');

        expect(api.get).toHaveBeenCalledWith('/lending/loans/loan_123/repayment-schedule');
        expect(result).toHaveLength(2);
        expect(result[0].status).toBe('PAID');
        expect(result[1].status).toBe('PENDING');
      });

      it('should handle overdue payment status', async () => {
        const mockSchedule: RepaymentSchedule[] = [
          {
            id: 'schedule_overdue',
            loanId: 'loan_456',
            installmentNumber: 1,
            dueDate: '2024-01-01T00:00:00Z',
            amount: 500000,
            principalAmount: 450000,
            interestAmount: 50000,
            status: 'OVERDUE',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockSchedule });

        const result = await service.getRepaymentSchedule('loan_456');

        expect(result[0].status).toBe('OVERDUE');
      });
    });

    describe('processRepayment', () => {
      it('should process loan repayment', async () => {
        const mockSchedule: RepaymentSchedule = {
          id: 'schedule_1',
          loanId: 'loan_123',
          installmentNumber: 1,
          dueDate: '2024-02-01T00:00:00Z',
          amount: 888888.89,
          principalAmount: 783888.89,
          interestAmount: 105000.00,
          status: 'PAID',
          paidAt: '2024-02-01T10:30:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockSchedule });

        const result = await service.processRepayment('schedule_1', 888888.89);

        expect(api.post).toHaveBeenCalledWith('/lending/repayment-schedules/schedule_1/pay', null, {
          params: { amount: 888888.89 },
        });
        expect(result.status).toBe('PAID');
      });
    });
  });

  describe('PayLater Management', () => {
    describe('activatePayLater', () => {
      it('should activate PayLater account', async () => {
        const mockRequest: PayLaterLimitRequest = {
          monthlyIncome: 15000000,
          employmentType: 'FULL_TIME',
          employmentDurationMonths: 24,
        };

        const mockPayLater: PayLater = {
          id: 'paylater_123',
          userId: 'user_123',
          creditLimit: 5000000,
          usedLimit: 0,
          availableLimit: 5000000,
          status: 'ACTIVE',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockPayLater });

        const result = await service.activatePayLater('user_123', mockRequest);

        expect(api.post).toHaveBeenCalledWith('/lending/paylater/activate', mockRequest, {
          params: { userId: 'user_123' },
        });
        expect(result.status).toBe('ACTIVE');
        expect(result.availableLimit).toBe(5000000);
      });

      it('should handle different employment types', async () => {
        const employmentTypes = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'SELF_EMPLOYED'];

        for (const empType of employmentTypes) {
          const mockRequest: PayLaterLimitRequest = {
            monthlyIncome: 10000000,
            employmentType: empType,
            employmentDurationMonths: 12,
          };

          const mockPayLater: PayLater = {
            id: `paylater_${empType}`,
            userId: `user_${empType}`,
            creditLimit: 3000000,
            usedLimit: 0,
            availableLimit: 3000000,
            status: 'ACTIVE',
            createdAt: '2024-01-01T10:00:00Z',
          };

          vi.mocked(api.post).mockResolvedValue({ data: mockPayLater });

          const result = await service.activatePayLater(`user_${empType}`, mockRequest);

          expect(result.status).toBe('ACTIVE');
        }
      });
    });

    describe('getPayLater', () => {
      it('should fetch PayLater account', async () => {
        const mockPayLater: PayLater = {
          id: 'paylater_456',
          userId: 'user_456',
          creditLimit: 10000000,
          usedLimit: 2500000,
          availableLimit: 7500000,
          status: 'ACTIVE',
          dueDate: '2024-02-15T00:00:00Z',
          minimumPayment: 500000,
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockPayLater });

        const result = await service.getPayLater('user_456');

        expect(api.get).toHaveBeenCalledWith('/lending/paylater/user_456');
        expect(result.availableLimit).toBe(7500000);
        expect(result.minimumPayment).toBe(500000);
      });

      it('should handle different PayLater statuses', async () => {
        const statuses: PayLaterStatus[] = ['ACTIVE', 'SUSPENDED', 'CLOSED'];

        for (const status of statuses) {
          const mockPayLater: PayLater = {
            id: `paylater_${status}`,
            userId: `user_${status}`,
            creditLimit: 5000000,
            usedLimit: 1000000,
            availableLimit: 4000000,
            status: status,
            createdAt: '2024-01-01T10:00:00Z',
          };

          vi.mocked(api.get).mockResolvedValue({ data: mockPayLater });

          const result = await service.getPayLater(`user_${status}`);

          expect(result.status).toBe(status);
        }
      });
    });

    describe('recordPurchase', () => {
      it('should record PayLater purchase', async () => {
        const mockTransaction: PayLaterTransaction = {
          id: 'txn_123',
          userId: 'user_123',
          type: 'PURCHASE',
          merchantName: 'Tokopedia',
          amount: 500000,
          balanceAfter: 500000,
          description: 'Online shopping',
          createdAt: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockTransaction });

        const result = await service.recordPurchase('user_123', 'Tokopedia', 500000, 'Online shopping');

        expect(api.post).toHaveBeenCalledWith('/lending/paylater/user_123/purchase', null, {
          params: {
            merchantName: 'Tokopedia',
            amount: 500000,
            description: 'Online shopping',
          },
        });
        expect(result.type).toBe('PURCHASE');
        expect(result.balanceAfter).toBe(500000);
      });

      it('should record purchase without description', async () => {
        const mockTransaction: PayLaterTransaction = {
          id: 'txn_456',
          userId: 'user_456',
          type: 'PURCHASE',
          merchantName: 'Shopee',
          amount: 1000000,
          balanceAfter: 1500000,
          createdAt: '2024-01-01T11:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockTransaction });

        const result = await service.recordPurchase('user_456', 'Shopee', 1000000);

        expect(result.merchantName).toBe('Shopee');
      });
    });

    describe('recordPayment', () => {
      it('should record PayLater payment', async () => {
        const mockTransaction: PayLaterTransaction = {
          id: 'txn_789',
          userId: 'user_789',
          type: 'PAYMENT',
          amount: 500000,
          balanceAfter: 0,
          createdAt: '2024-01-01T12:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockTransaction });

        const result = await service.recordPayment('user_789', 500000);

        expect(api.post).toHaveBeenCalledWith('/lending/paylater/user_789/payment', null, {
          params: { amount: 500000 },
        });
        expect(result.type).toBe('PAYMENT');
        expect(result.balanceAfter).toBe(0);
      });
    });

    describe('getTransactionHistory', () => {
      it('should fetch PayLater transaction history', async () => {
        const mockTransactions: PayLaterTransaction[] = [
          {
            id: 'txn_1',
            userId: 'user_123',
            type: 'PURCHASE',
            merchantName: 'Merchant 1',
            amount: 100000,
            balanceAfter: 100000,
            createdAt: '2024-01-01T10:00:00Z',
          },
          {
            id: 'txn_2',
            userId: 'user_123',
            type: 'PAYMENT',
            amount: 50000,
            balanceAfter: 50000,
            createdAt: '2024-01-02T10:00:00Z',
          },
          {
            id: 'txn_3',
            userId: 'user_123',
            type: 'PURCHASE',
            merchantName: 'Merchant 2',
            amount: 200000,
            balanceAfter: 250000,
            createdAt: '2024-01-03T10:00:00Z',
          },
        ];

        vi.mocked(api.get).mockResolvedValue({ data: mockTransactions });

        const result = await service.getTransactionHistory('user_123');

        expect(api.get).toHaveBeenCalledWith('/lending/paylater/user_123/transactions');
        expect(result).toHaveLength(3);
        expect(result[0].type).toBe('PURCHASE');
        expect(result[1].type).toBe('PAYMENT');
      });
    });
  });

  describe('Credit Score Management', () => {
    describe('calculateCreditScore', () => {
      it('should calculate credit score', async () => {
        const mockScore: CreditScore = {
          id: 'score_123',
          userId: 'user_123',
          score: 750,
          grade: 'A',
          factors: ['Payment history', 'Credit utilization', 'Account age'],
          lastUpdated: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.post).mockResolvedValue({ data: mockScore });

        const result = await service.calculateCreditScore('user_123');

        expect(api.post).toHaveBeenCalledWith('/lending/credit-score/calculate', null, {
          params: { userId: 'user_123' },
        });
        expect(result.score).toBe(750);
        expect(result.grade).toBe('A');
      });

      it('should handle different credit grades', async () => {
        const grades: Array<'A' | 'B' | 'C' | 'D' | 'E'> = ['A', 'B', 'C', 'D', 'E'];
        const scores = [800, 700, 600, 500, 400];

        for (let i = 0; i < grades.length; i++) {
          const mockScore: CreditScore = {
            id: `score_${grades[i]}`,
            userId: `user_${grades[i]}`,
            score: scores[i],
            grade: grades[i],
            factors: ['Test factor'],
            lastUpdated: '2024-01-01T10:00:00Z',
          };

          vi.mocked(api.post).mockResolvedValue({ data: mockScore });

          const result = await service.calculateCreditScore(`user_${grades[i]}`);

          expect(result.grade).toBe(grades[i]);
          expect(result.score).toBe(scores[i]);
        }
      });
    });

    describe('getCreditScore', () => {
      it('should fetch existing credit score', async () => {
        const mockScore: CreditScore = {
          id: 'score_456',
          userId: 'user_456',
          score: 680,
          grade: 'B',
          factors: ['Payment history', 'Credit mix'],
          lastUpdated: '2024-01-01T10:00:00Z',
        };

        vi.mocked(api.get).mockResolvedValue({ data: mockScore });

        const result = await service.getCreditScore('user_456');

        expect(api.get).toHaveBeenCalledWith('/lending/credit-score/user_456');
        expect(result.score).toBe(680);
        expect(result.factors).toContain('Payment history');
      });
    });
  });

  describe('Data transformation', () => {
    it('should correctly calculate loan payment details', async () => {
      const mockRequest: LoanApplicationRequest = {
        userId: 'user_calc',
        amount: 12000000,
        tenureMonths: 12,
        purpose: 'Test calculation',
        interestRate: 12.0,
      };

      const mockLoan: Loan = {
        id: 'loan_calc',
        userId: 'user_calc',
        amount: 12000000,
        interestRate: 12.0,
        tenureMonths: 12,
        purpose: 'Test calculation',
        status: 'PENDING',
        monthlyPayment: 1066666.67,
        totalPayment: 12800000.04,
        createdAt: '2024-01-01T10:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockLoan });

      const result = await service.applyLoan(mockRequest);

      expect(result.monthlyPayment).toBeGreaterThan(0);
      expect(result.totalPayment).toBeGreaterThan(result.amount);
    });

    it('should transform API response to PayLater format', async () => {
      const mockPayLater: PayLater = {
        id: 'paylater_transform',
        userId: 'user_transform',
        creditLimit: 10000000,
        usedLimit: 3000000,
        availableLimit: 7000000,
        status: 'ACTIVE',
        createdAt: '2024-01-01T10:00:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPayLater });

      const result = await service.getPayLater('user_transform');

      expect(result.availableLimit).toBe(result.creditLimit - result.usedLimit);
    });
  });

  describe('Error handling', () => {
    it('should handle loan application errors', async () => {
      const mockRequest: LoanApplicationRequest = {
        userId: 'user_error',
        amount: 1000000,
        tenureMonths: 12,
        purpose: 'Test error',
      };

      vi.mocked(api.post).mockRejectedValue(new Error('Credit check failed'));

      await expect(service.applyLoan(mockRequest)).rejects.toThrow('Credit check failed');
    });

    it('should handle PayLater activation errors', async () => {
      const mockRequest: PayLaterLimitRequest = {
        monthlyIncome: 5000000,
        employmentType: 'PART_TIME',
        employmentDurationMonths: 6,
      };

      vi.mocked(api.post).mockRejectedValue(new Error('Insufficient employment duration'));

      await expect(service.activatePayLater('user_error', mockRequest)).rejects.toThrow(
        'Insufficient employment duration'
      );
    });

    it('should handle credit score calculation errors', async () => {
      vi.mocked(api.post).mockRejectedValue(new Error('Unable to calculate score'));

      await expect(service.calculateCreditScore('user_error')).rejects.toThrow('Unable to calculate score');
    });
  });
});
