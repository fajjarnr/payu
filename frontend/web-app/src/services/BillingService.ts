import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';

export interface BillPayment {
  id: string;
  accountId: string;
  billerCode: string;
  billerName: string;
  customerId: string;
  amount: number;
  adminFee: number;
  totalAmount: number;
  status: PaymentStatus;
  referenceNumber: string;
  createdAt: string;
}

export interface BillerInfo {
  code: string;
  name: string;
  category: BillerCategory;
  iconUrl?: string;
  isActive: boolean;
}

export interface TopUpRequest {
  accountId: string;
  billerCode: string;
  customerId: string;
  amount: number;
}

export interface CreatePaymentRequest {
  accountId: string;
  billerCode: string;
  customerId: string;
  amount: number;
}

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REFUNDED' | 'EXPIRED';
export type BillerCategory = 'ELECTRICITY' | 'WATER' | 'MOBILE' | 'INTERNET' | 'INSURANCE' | 'EWALLET';

class BillingService {
  private static instance: BillingService;

  static getInstance(): BillingService {
    if (!BillingService.instance) {
      BillingService.instance = new BillingService();
    }
    return BillingService.instance;
  }

  async createPayment(request: CreatePaymentRequest): Promise<BillPayment> {
    const response = await api.post('/billing/payments', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async createTopUp(request: TopUpRequest): Promise<BillPayment> {
    const response = await api.post('/billing/topup', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async getPaymentHistory(page = 0, size = 20): Promise<{ content: BillPayment[]; totalElements: number }> {
    const response = await api.get('/billing/payments', { params: { page, size } });
    return response.data;
  }

  async getPayment(paymentId: string): Promise<BillPayment> {
    const response = await api.get(`/billing/payments/${paymentId}`);
    return response.data;
  }

  async getBillers(category?: BillerCategory): Promise<BillerInfo[]> {
    const response = await api.get('/billing/billers', { params: { category } });
    return response.data;
  }
}

export default BillingService.getInstance();
