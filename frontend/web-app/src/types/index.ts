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
  sharedMembers?: SharedMember[];
  isShared?: boolean;
  ownerAccountId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SharedMember {
  accountId: string;
  fullName: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
}

export interface BalanceResponse {
  accountId: string;
  balance: number;
  availableBalance: number;
  reservedBalance: number;
  currency: string;
}

export type TransactionType = 'INTERNAL_TRANSFER' | 'BIFAST_TRANSFER' | 'SKN_TRANSFER' | 'RTGS_TRANSFER' | 'QRIS_PAYMENT' | 'BILL_PAYMENT' | 'TOP_UP';
export type TransferType = 'INTERNAL_TRANSFER' | 'BIFAST_TRANSFER' | 'SKN_TRANSFER' | 'RTGS_TRANSFER';
export type TransactionStatus = 'PENDING' | 'VALIDATING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type TransferScheduleType = 'NOW' | 'SCHEDULED' | 'RECURRING';

export const transferSchema = z.object({
  fromAccountId: z.string().min(1, 'Source account is required'),
  toAccountId: z.string().min(1, 'Destination account is required'),
  amount: z.number().positive('Amount must be positive'),
  description: z.string().optional(),
  transferType: z.enum(['INTERNAL_TRANSFER', 'BIFAST_TRANSFER', 'SKN_TRANSFER', 'RTGS_TRANSFER'] as const).optional().default('INTERNAL_TRANSFER'),
  scheduleType: z.enum(['NOW', 'SCHEDULED', 'RECURRING'] as const).optional().default('NOW'),
  scheduledAt: z.string().optional(),
  recurringDay: z.number().min(1).max(31).optional(),
  recurringMonth: z.number().min(1).max(12).optional(),
});

// Use the input type for form (all fields required for form validation)
export type TransferRequest = z.input<typeof transferSchema>;
// Use the output type for API calls (defaults applied)
export type TransferRequestOutput = z.output<typeof transferSchema>;

export interface InitiateTransferRequest {
  senderAccountId: string;
  recipientAccountNumber: string;
  amount: number;
  currency?: string;
  description: string;
  type: TransactionType;
  transactionPin?: string;
  deviceId?: string;
  scheduledAt?: string;
  recurringDay?: number;
  recurringMonth?: number;
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

export interface ProcessQrisPaymentRequest {
  qrCode: string;
  amount: number;
  accountId: string;
}

export interface AnalyticsData {
  totalIncome: number;
  totalExpenses: number;
  monthlySavings: number;
  investmentRoi: number;
  incomeChange: number;
  expenseChange: number;
  savingsChange: number;
  roiChange: number;
  spendingBreakdown: SpendingCategory[];
}

export interface SpendingCategory {
  label: string;
  amount: number;
  percentage: number;
  color: string;
}

export interface PortfolioUpdate {
  type: 'BALANCE_UPDATE' | 'TRANSACTION' | 'INVESTMENT';
  data: AnalyticsData;
  timestamp: string;
}

// A/B Testing Types (re-exported from ABTestingService)
export enum ExperimentStatus {
  DRAFT = 'DRAFT',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
}

export enum AllocationStrategy {
  MODULO = 'MODULO',
  MURMURHASH3 = 'MURMURHASH3',
}

export interface ExperimentVariant {
  id: string;
  experimentId: string;
  key: string;
  name: string;
  description: string;
  isControl: boolean;
  allocationWeight: number;
  config: Record<string, unknown>;
  createdAt: string;
}

export interface Experiment {
  id: string;
  key: string;
  name: string;
  description: string;
  status: ExperimentStatus;
  allocationStrategy: AllocationStrategy;
  trafficPercentage: number;
  targetAudience: Record<string, unknown>;
  variants: ExperimentVariant[];
  startDate: string;
  endDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VariantAssignment {
  id: string;
  experimentId: string;
  experimentKey: string;
  variantId: string;
  variantKey: string;
  userId: string;
  deviceId?: string;
  assignedAt: string;
  variant: ExperimentVariant;
}

// Customer Segmentation Types (re-exported from SegmentationService)
export type SegmentTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'VIP';
export type SegmentStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export interface CustomerSegment {
  id: string;
  name: string;
  description: string;
  tier: SegmentTier;
  minBalance: number;
  maxBalance?: number;
  benefits: string[];
  requirements: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SegmentMembership {
  id: string;
  userId: string;
  segmentId: string;
  segment: CustomerSegment;
  status: SegmentStatus;
  joinedAt: string;
  validUntil?: string;
  score: number;
}

export interface SegmentedOffer {
  id: string;
  title: string;
  description: string;
  segmentId: string;
  segmentTier: SegmentTier;
  offerType: 'CASHBACK' | 'DISCOUNT' | 'REWARD_POINTS' | 'FREE_TRANSFER' | 'BONUS_INTEREST';
  value: number;
  currency?: string;
  percentage?: number;
  validFrom: string;
  validUntil: string;
  terms: string[];
  imageUrl?: string;
  promoCode?: string;
  minTransaction?: number;
  maxReward?: number;
  isActive: boolean;
  createdAt: string;
}

export interface UserSegmentsResponse {
  memberships: SegmentMembership[];
  currentTier: SegmentTier;
  nextTier?: SegmentTier;
  progressToNext?: number;
  totalScore: number;
}
