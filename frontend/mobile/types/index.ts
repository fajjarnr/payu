// User & Auth Types
export interface User {
  id: string;
  email: string;
  phoneNumber: string;
  fullName: string;
  avatar?: string;
  kycVerified: boolean;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface LoginCredentials {
  identifier: string; // email or phone
  password: string;
}

export interface RegisterData {
  email: string;
  phoneNumber: string;
  fullName: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  user: User;
  tokens: AuthTokens;
}

// Wallet Types
export interface Wallet {
  id: string;
  userId: string;
  balance: number;
  currency: string;
  pocketType: 'primary' | 'savings' | 'goals';
  createdAt: string;
}

export interface Pocket {
  id: string;
  name: string;
  balance: number;
  type: 'savings' | 'goals' | 'primary';
  color: string;
  icon: string;
}

// Transaction Types
export interface Transaction {
  id: string;
  userId: string;
  type: 'transfer' | 'payment' | 'topup' | 'qris' | 'withdrawal';
  amount: number;
  description: string;
  category?: string;
  status: 'pending' | 'completed' | 'failed' | 'cancelled';
  fromPocket?: string;
  toPocket?: string;
  recipientName?: string;
  recipientAccount?: string;
  createdAt: string;
  processedAt?: string;
}

export interface TransferData {
  amount: number;
  recipientAccount: string;
  recipientBank: string;
  description: string;
  fromPocket: string;
  scheduleDate?: string;
}

// Card Types
export interface VirtualCard {
  id: string;
  lastFour: string;
  cardHolder: string;
  expiryDate: string;
  cvv: string;
  status: 'active' | 'frozen' | 'cancelled';
  balance: number;
  limit: number;
  spendingLimit: number;
  isPhysical: boolean;
  createdAt: string;
}

// QRIS Types
export interface QRISData {
  merchantName: string;
  amount: number;
  merchantId: string;
  terminalId: string;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

// Navigation Types
export type RootStackParamList = {
  auth: undefined;
  tabs: undefined;
  qris: undefined;
  feedback: undefined;
  transfer: undefined;
  'transfer-confirm': { data: TransferData };
  'transfer-success': { transactionId: string };
};

export type AuthStackParamList = {
  login: undefined;
  register: undefined;
  'forgot-password': undefined;
};

export type TabsParamList = {
  index: undefined;
  transfers: undefined;
  cards: undefined;
  history: undefined;
  profile: undefined;
};

// Component Props Types
export interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  icon?: React.ReactNode;
  fullWidth?: boolean;
}

export interface InputProps {
  label?: string;
  value: string;
  onChangeText: (text: string) => void;
  placeholder?: string;
  secureTextEntry?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric' | 'phone-pad';
  error?: string;
  disabled?: boolean;
  icon?: React.ReactNode;
  multiline?: boolean;
  numberOfLines?: number;
}

export interface CardProps {
  children: React.ReactNode;
  variant?: 'elevated' | 'outlined' | 'flat';
  padding?: 'none' | 'sm' | 'md' | 'lg';
  className?: string;
}

export interface ModalProps {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}

// Notification Types
export interface PushNotification {
  id: string;
  title: string;
  body: string;
  data?: Record<string, any>;
  readAt?: string;
  createdAt: string;
}

// Feedback Types
export interface FeedbackData {
  category: 'bug' | 'feature' | 'ui' | 'performance' | 'other';
  rating: number;
  message: string;
  screenshots?: string[];
  deviceInfo: {
    appVersion: string;
    os: string;
    osVersion: string;
    device: string;
  };
}
