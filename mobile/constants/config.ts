export const API_CONFIG = {
  // Update this for your environment
  BASE_URL: __DEV__
    ? 'http://localhost:8080/api'
    : 'https://api.payu.id/api',
  TIMEOUT: 30000,
} as const;

export const AUTH_CONFIG = {
  TOKEN_KEY: 'payu_auth_tokens',
  USER_KEY: 'payu_user',
  REFRESH_THRESHOLD: 5 * 60 * 1000, // 5 minutes before expiry
} as const;

export const APP_CONFIG = {
  VERSION: '1.0.0',
  DEFAULT_CURRENCY: 'IDR',
  MIN_TRANSFER_AMOUNT: 10000,
  MAX_TRANSFER_AMOUNT: 50000000,
} as const;

export const TRANSACTION_CATEGORIES = [
  'Transfer',
  'Payment',
  'Top-up',
  'QRIS',
  'Withdrawal',
  'Bill Payment',
] as const;

export const BANKS = [
  { code: 'BCA', name: 'Bank Central Asia' },
  { code: 'BNI', name: 'Bank Negara Indonesia' },
  { code: 'BRI', name: 'Bank Rakyat Indonesia' },
  { code: 'MANDIRI', name: 'Bank Mandiri' },
  { code: 'CIMB', name: 'CIMB Niaga' },
  { code: 'PERMATA', name: 'Bank Permata' },
  { code: 'JENIUS', name: 'Jenius (BTPN)' },
  { code: 'DIGIBANK', name: 'Digibank (DBS)' },
  { code: 'JAGO', name: 'Bank Jago' },
  { code: 'ALLO', name: 'Bank Allo' },
] as const;

export const FEEDBACK_CATEGORIES = [
  { id: 'bug', label: 'Bug Report', icon: '🐛' },
  { id: 'feature', label: 'Feature Request', icon: '💡' },
  { id: 'ui', label: 'UI/UX', icon: '🎨' },
  { id: 'performance', label: 'Performance', icon: '⚡' },
  { id: 'other', label: 'Other', icon: '📝' },
] as const;

// Transfer Types
export const TRANSFER_TYPES = [
  { id: 'bifast', name: 'BI-FAST', fee: 0, minAmount: 10000, maxAmount: 25000000, processingTime: 'Real-time' },
  { id: 'skn', name: 'SKN', fee: 5000, minAmount: 10000, maxAmount: 100000000, processingTime: 'Same day' },
  { id: 'rtgs', name: 'RTGS', fee: 25000, minAmount: 10000001, maxAmount: 10000000000, processingTime: 'Real-time' },
] as const;

// Notification Types
export const NOTIFICATION_TYPES = {
  TRANSACTION: 'transaction',
  PROMO: 'promo',
  SECURITY: 'security',
  ACCOUNT: 'account',
} as const;

// Session Timeout Options (in minutes)
export const SESSION_TIMEOUTS = [5, 15, 30, 60] as const;

// App Lock Options
export const APP_LOCK_OPTIONS = {
  BIOMETRIC: 'biometric',
  PIN: 'pin',
  NONE: 'none',
} as const;
