import { z } from 'zod';

export const registerUserSchema = z.object({
  externalId: z.string().min(1, 'External ID is required'),
  username: z.string().min(3, 'Username must be at least 3 characters'),
  email: z.string().email('Invalid email address'),
  phoneNumber: z.string().min(10, 'Phone number is too short').optional(),
  fullName: z.string().min(1, 'Full name is required'),
  nik: z.string().length(16, 'NIK must be exactly 16 digits'),
});

export type RegisterUserRequest = z.infer<typeof registerUserSchema>;

export const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

export type LoginRequest = z.infer<typeof loginSchema>;

export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface User {
  id: string;
  externalId: string;
  username: string;
  email: string;
  phoneNumber?: string;
  fullName: string;
  nik: string;
  kycStatus: KycStatus;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

export interface Pocket {
  id: string;
  name: string;
  balance: number;
  target: number;
  type: 'MAIN' | 'SAVING' | 'SHARED';
}

export interface BalanceResponse {
  accountId: string;
  balance: number;
  availableBalance: number;
  reservedBalance: number;
  currency: string;
}

export type TransactionType = 'INTERNAL_TRANSFER' | 'BIFAST_TRANSFER' | 'QRIS_PAYMENT' | 'BILL_PAYMENT' | 'TOP_UP';
export type TransactionStatus = 'PENDING' | 'VALIDATING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export const transferSchema = z.object({
  fromAccountId: z.string().min(1, 'Source account is required'),
  toAccountId: z.string().min(1, 'Destination account is required'),
  amount: z.number().positive('Amount must be positive'),
  description: z.string().optional(),
});

export type TransferRequest = z.infer<typeof transferSchema>;

export interface InitiateTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency?: string;
  description: string;
  type?: TransactionType;
  transactionPin?: string;
  deviceId?: string;
}

export interface InitiateTransferResponse {
  transactionId: string;
  referenceNumber: string;
  status: string;
  fee: number;
  estimatedCompletionTime: string;
}

export interface Transaction {
  id: string;
  referenceNumber: string;
  senderAccountId: string;
  recipientAccountId: string;
  type: TransactionType;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
  failureReason?: string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface WalletTransaction {
  id: string;
  walletId: string;
  referenceId: string;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface Biller {
  code: string;
  name: string;
  category: string;
  logo: string;
}

export interface CreatePaymentRequest {
  billerCode: string;
  customerId: string;
  amount: number;
  referenceNumber?: string;
}

export interface PaymentResponse {
  id: string;
  billerCode: string;
  customerId: string;
  amount: number;
  currency: string;
  referenceNumber: string;
  status: string;
  createdAt: string;
}
