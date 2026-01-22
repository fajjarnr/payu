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

export interface User {
  id: string;
  externalId: string;
  username: string;
  email: string;
  fullName: string;
  kycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
}

export interface LoginResponse {
  token: string;
  user: User;
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

export const transferSchema = z.object({
  fromAccountId: z.string().min(1, 'Source account is required'),
  toAccountId: z.string().min(1, 'Destination account is required'),
  amount: z.number().positive('Amount must be positive'),
  description: z.string().optional(),
});

export type TransferRequest = z.infer<typeof transferSchema>;

export interface TransferResponse {
  transactionId: string;
  status: string;
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  currency: string;
  createdAt: string;
}

export interface Transaction {
  id: string;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  currency: string;
  description: string;
  createdAt: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
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
