import api from '@/lib/api';

export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISBURSED' | 'REPAID' | 'DEFAULTED';
export type PayLaterStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export interface LoanApplicationRequest {
  userId: string;
  amount: number;
  tenureMonths: number;
  purpose: string;
  interestRate?: number;
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
    const response = await api.post<Loan>('/lending/loans', request);
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

  async processRepayment(scheduleId: string, amount: number): Promise<RepaymentSchedule> {
    const response = await api.post<RepaymentSchedule>(`/lending/repayment-schedules/${scheduleId}/pay`, null, {
      params: { amount }
    });
    return response.data;
  }

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

  async recordPurchase(userId: string, merchantName: string, amount: number, description?: string): Promise<PayLaterTransaction> {
    const response = await api.post<PayLaterTransaction>(`/lending/paylater/${userId}/purchase`, null, {
      params: { merchantName, amount, description }
    });
    return response.data;
  }

  async recordPayment(userId: string, amount: number): Promise<PayLaterTransaction> {
    const response = await api.post<PayLaterTransaction>(`/lending/paylater/${userId}/payment`, null, {
      params: { amount }
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
}

export default LendingService.getInstance();
