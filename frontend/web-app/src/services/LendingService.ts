import api from '@/lib/api';
import { getFinancialMutationHeaders } from '@/lib/utils';

export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISBURSED' | 'REPAID' | 'DEFAULTED';
export type PayLaterStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

// BUG-CROSS-053: Backend LoanApplicationCommand: externalId, loanType, principalAmount, tenureMonths, purpose
export interface LoanApplicationRequest {
  externalId: string;
  loanType: 'PERSONAL' | 'BUSINESS' | 'MORTGAGE' | 'AUTO' | 'EDUCATION';
  principalAmount: number;
  tenureMonths: number;
  purpose: string;
}

export interface Loan {
  id: string;
  userId: string;
  amount: number;
  interestRate: number;
  tenureMonths: number;
  purpose: string;
  status: LoanStatus;
  monthlyPayment: number;
  totalPayment: number;
  createdAt: string;
  approvedAt?: string;
  disbursedAt?: string;
}

export interface RepaymentSchedule {
  id: string;
  loanId: string;
  installmentNumber: number;
  dueDate: string;
  amount: number;
  principalAmount: number;
  interestAmount: number;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
  paidAt?: string;
}

export interface PayLater {
  id: string;
  userId: string;
  creditLimit: number;
  usedLimit: number;
  availableLimit: number;
  status: PayLaterStatus;
  dueDate?: string;
  minimumPayment?: number;
  createdAt: string;
}

export interface PayLaterTransaction {
  id: string;
  userId: string;
  type: 'PURCHASE' | 'PAYMENT';
  merchantName?: string;
  amount: number;
  balanceAfter: number;
  description?: string;
  createdAt: string;
}

export interface CreditScore {
  id: string;
  userId: string;
  score: number;
  grade: 'A' | 'B' | 'C' | 'D' | 'E';
  factors: string[];
  lastUpdated: string;
}

export interface PayLaterLimitRequest {
  monthlyIncome: number;
  employmentType: string;
  employmentDurationMonths: number;
}

export interface PayLaterPurchaseRequest {
  merchantName: string;
  amount: number;
  description?: string;
}

export interface PayLaterPaymentRequest {
  amount: number;
}

export class LendingService {
  private static instance: LendingService;

  private constructor() {}

  static getInstance(): LendingService {
    if (!LendingService.instance) {
      LendingService.instance = new LendingService();
    }
    return LendingService.instance;
  }

  async applyLoan(request: LoanApplicationRequest): Promise<Loan> {
    const response = await api.post<Loan>('/lending/loans', request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async getLoan(loanId: string): Promise<Loan> {
    const response = await api.get<Loan>(`/lending/loans/${loanId}`);
    return response.data;
  }

  async createRepaymentSchedule(loanId: string): Promise<RepaymentSchedule[]> {
    const response = await api.post<RepaymentSchedule[]>(`/lending/loans/${loanId}/repayment-schedule`);
    return response.data;
  }

  async getRepaymentSchedule(loanId: string): Promise<RepaymentSchedule[]> {
    const response = await api.get<RepaymentSchedule[]>(`/lending/loans/${loanId}/repayment-schedule`);
    return response.data;
  }

  // IMP-015 Fix: Moved 'amount' from query param to request body
  // Query params are logged in access logs and browser history (security risk)
  async processRepayment(scheduleId: string, amount: number): Promise<RepaymentSchedule> {
    const response = await api.post<RepaymentSchedule>(`/lending/repayment-schedules/${scheduleId}/pay`, {
      amount
    }, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  // BUG-CROSS-054: Backend activatePayLater takes userId as @RequestParam, body is PayLaterLimitRequest only
  // IMP-015 Fix: Moved 'userId' from query param to request body
  // Query params are logged in access logs and browser history (security risk)
  async activatePayLater(userId: string, request: PayLaterLimitRequest): Promise<PayLater> {
    const response = await api.post<PayLater>(`/lending/paylater/activate`, request, {
      params: { userId }
    });
    return response.data;
  }

  async getPayLater(userId: string): Promise<PayLater> {
    const response = await api.get<PayLater>(`/lending/paylater/${userId}`);
    return response.data;
  }

  // BUG-CROSS-055: Backend recordPurchase reads merchantName, amount, description from JSON body
  async recordPurchase(userId: string, merchantName: string, amount: number, description?: string): Promise<PayLaterTransaction> {
    const request: PayLaterPurchaseRequest = {
      merchantName,
      amount,
      ...(description === undefined ? {} : { description }),
    };
    const response = await api.post<PayLaterTransaction>(`/lending/paylater/${userId}/purchase`, request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  // BUG-CROSS-056: Backend recordPayment reads amount from JSON body
  async recordPayment(userId: string, amount: number): Promise<PayLaterTransaction> {
    const request: PayLaterPaymentRequest = { amount };
    const response = await api.post<PayLaterTransaction>(`/lending/paylater/${userId}/payment`, request, {
      headers: getFinancialMutationHeaders(),
    });
    return response.data;
  }

  async getTransactionHistory(userId: string): Promise<PayLaterTransaction[]> {
    const response = await api.get<PayLaterTransaction[]>(`/lending/paylater/${userId}/transactions`);
    return response.data;
  }

  async calculateCreditScore(userId: string): Promise<CreditScore> {
    const response = await api.post<CreditScore>('/lending/credit-score/calculate', null, {
      params: { userId }
    });
    return response.data;
  }

  async getCreditScore(userId: string): Promise<CreditScore> {
    const response = await api.get<CreditScore>(`/lending/credit-score/${userId}`);
    return response.data;
  }

  // === Pre-Approval (FE-GAP-013) ===

  /** POST /lending/pre-approval/check — Check pre-approval eligibility */
  async checkPreApproval(request: PreApprovalCheckRequest): Promise<PreApproval> {
    const response = await api.post<PreApproval>('/lending/pre-approval/check', request);
    return response.data;
  }

  /** GET /lending/pre-approval/{preApprovalId} — Get pre-approval details */
  async getPreApproval(preApprovalId: string): Promise<PreApproval> {
    const response = await api.get<PreApproval>(`/lending/pre-approval/${preApprovalId}`);
    return response.data;
  }

  /** GET /lending/pre-approval/user/{userId}/active — Get active pre-approvals */
  async getActivePreApprovals(userId: string): Promise<PreApproval[]> {
    const response = await api.get<PreApproval[]>(`/lending/pre-approval/user/${userId}/active`);
    return response.data;
  }
}

// === Pre-Approval Types ===

export interface PreApprovalCheckRequest {
  userId: string;
  requestedAmount?: number;
  purpose?: string;
}

export interface PreApproval {
  id: string;
  userId: string;
  maxAmount: number;
  interestRate: number;
  maxTenureMonths: number;
  status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'EXPIRED';
  validUntil: string;
  createdAt: string;
}

export default LendingService.getInstance();
