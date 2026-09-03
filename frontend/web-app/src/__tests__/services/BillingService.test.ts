import { describe, it, expect, vi, beforeEach } from 'vitest';
import BillingService, {
  type BillPayment,
  type BillerInfo,
  type CreatePaymentRequest,
  type TopUpRequest,
} from '@/services/BillingService';
import api from '@/lib/api';
import { asMoney } from '@/lib/currency';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('BillingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createPayment', () => {
    it('should create bill payment successfully', async () => {
      const request: CreatePaymentRequest = {
        accountId: 'acc_123',
        billerCode: 'PLN',
        customerId: 'customer_456',
        amount: asMoney('150000'),
      };

      const mockPayment: BillPayment = {
        id: 'pay_789',
        accountId: 'acc_123',
        billerCode: 'PLN',
        billerName: 'PLN Electricity',
        customerId: 'customer_456',
        amount: asMoney('150000'),
        adminFee: asMoney('2500'),
        totalAmount: asMoney('152500'),
        status: 'COMPLETED',
        referenceNumber: 'REF123',
        createdAt: '2026-02-18T10:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPayment });

      const result = await BillingService.createPayment(request);

      expect(api.post).toHaveBeenCalledWith('/payments', request, {
        headers: expect.any(Object),
      });
      expect(result).toEqual(mockPayment);
      expect(result.totalAmount).toBe('152500');
    });
  });

  describe('createTopUp', () => {
    it('should create top-up successfully', async () => {
      // BUG-CROSS-072: TopUpRequest now uses provider/walletNumber (not billerCode/customerId)
      const request: TopUpRequest = {
        accountId: 'acc_123',
        provider: 'TELKOMSEL',
        walletNumber: '08123456789',
        amount: asMoney('50000'),
      };

      const mockPayment: BillPayment = {
        id: 'topup_001',
        accountId: 'acc_123',
        billerCode: 'TELKOMSEL',
        billerName: 'Telkomsel Prepaid',
        customerId: '08123456789',
        amount: asMoney('50000'),
        adminFee: asMoney('0'),
        totalAmount: asMoney('50000'),
        status: 'COMPLETED',
        referenceNumber: 'TOP001',
        createdAt: '2026-02-18T10:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockPayment });

      const result = await BillingService.createTopUp(request);

      expect(api.post).toHaveBeenCalledWith('/topup', request, {
        headers: expect.any(Object),
      });
      expect(result.status).toBe('COMPLETED');
    });
  });

  describe('getPaymentHistory', () => {
    it('should fetch payment history with default pagination', async () => {
      const mockResponse = {
        content: [{ id: 'pay_1' }, { id: 'pay_2' }] as BillPayment[],
        totalElements: 2,
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      const result = await BillingService.getPaymentHistory();

      expect(api.get).toHaveBeenCalledWith('/payments', {
        params: { page: 0, size: 20 },
      });
      expect(result.content).toHaveLength(2);
      expect(result.totalElements).toBe(2);
    });

    it('should support custom pagination', async () => {
      const mockResponse = { content: [], totalElements: 50 };
      vi.mocked(api.get).mockResolvedValue({ data: mockResponse });

      await BillingService.getPaymentHistory(2, 10);

      expect(api.get).toHaveBeenCalledWith('/payments', {
        params: { page: 2, size: 10 },
      });
    });
  });

  describe('getPayment', () => {
    it('should fetch single payment by ID', async () => {
      const mockPayment: BillPayment = {
        id: 'pay_789',
        accountId: 'acc_123',
        billerCode: 'PDAM',
        billerName: 'PDAM Water',
        customerId: 'cust_001',
        amount: asMoney('85000'),
        adminFee: asMoney('2500'),
        totalAmount: asMoney('87500'),
        status: 'COMPLETED',
        referenceNumber: 'REF456',
        createdAt: '2026-02-18T10:00:00Z',
      };

      vi.mocked(api.get).mockResolvedValue({ data: mockPayment });

      const result = await BillingService.getPayment('pay_789');

      expect(api.get).toHaveBeenCalledWith('/payments/pay_789');
      expect(result.id).toBe('pay_789');
    });
  });

  describe('getBillers', () => {
    it('should fetch all billers without category filter', async () => {
      const mockBillers: BillerInfo[] = [
        { code: 'PLN', name: 'PLN Electricity', category: 'ELECTRICITY', isActive: true },
        { code: 'PDAM', name: 'PDAM Water', category: 'WATER', isActive: true },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockBillers });

      const result = await BillingService.getBillers();

      expect(api.get).toHaveBeenCalledWith('/billers', {
        params: { category: undefined },
      });
      expect(result).toHaveLength(2);
    });

    it('should filter billers by category', async () => {
      const mockBillers: BillerInfo[] = [
        { code: 'PLN', name: 'PLN Electricity', category: 'ELECTRICITY', isActive: true },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockBillers });

      const result = await BillingService.getBillers('ELECTRICITY');

      expect(api.get).toHaveBeenCalledWith('/billers', {
        params: { category: 'ELECTRICITY' },
      });
      expect(result).toHaveLength(1);
    });
  });
});
