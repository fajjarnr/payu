export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

export interface CreatePaymentRequest {
  accountId: string;
  billerCode: string;
  customerId: string;
  amount: number;
}

export interface PaymentResponse {
  id: string;
  referenceNumber: string;
  accountId: string;
  billerCode: string;
  billerName: string;
  customerId: string;
  amount: number;
  adminFee: number;
  totalAmount: number;
  status: string;
  failureReason?: string;
  createdAt: string;
  completedAt?: string;
}

export interface InitiateTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency: string;
  description?: string;
  type?: string;
  transactionPin?: string;
  idempotencyKey?: string;
  bankCode?: string;
}

export interface TransactionResponse {
  id: string;
  referenceNumber: string;
  senderAccountId: string;
  recipientAccountId?: string;
  amount: number;
  currency: string;
  status: string;
  type: string;
  createdAt: string;
  completedAt?: string;
}

export interface BalanceResponse {
  accountId: string;
  balance: string;
  availableBalance: string;
  reservedBalance: string;
  currency: string;
}
