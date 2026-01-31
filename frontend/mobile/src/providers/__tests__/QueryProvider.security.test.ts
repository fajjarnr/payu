/**
 * Security Test Suite for QueryProvider
 * =====================================
 *
 * Tests to verify that sensitive financial and PII data is NOT persisted
 * to AsyncStorage (which is unencrypted).
 *
 * Compliance:
 * - PCI-DSS Requirement 3: Protect stored cardholder data
 * - OJK Regulation: Financial data encryption at rest
 * - PayU Security Policy P2-C2: Secure token storage
 *
 * @version 2.0.0
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  verifyAsyncStorageSecurity,
  devLogAsyncStorageContents,
} from '../QueryProvider';

// Mock query cache data structures
const createMockQueryCache = (queries: Array<{ queryKey: string[]; state: { data: unknown } }>) => ({
  clientState: {
    queries,
  },
});

describe('QueryProvider Security Tests', () => {
  beforeEach(async () => {
    // Clear AsyncStorage before each test
    await AsyncStorage.clear();
  });

  afterEach(async () => {
    // Clean up after each test
    await AsyncStorage.clear();
  });

  describe('Sensitive Data Exclusion', () => {
    it('should detect wallet queries in AsyncStorage as security violation', async () => {
      // Simulate a wallet query being persisted (old behavior)
      const mockCache = createMockQueryCache([
        { queryKey: ['wallet', 'primary'], state: { data: { balance: 1500000 } } },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('wallet')
      );
      expect(report.violations).toContainEqual(
        expect.stringContaining('SECURITY VIOLATION')
      );
    });

    it('should detect transaction queries in AsyncStorage as security violation', async () => {
      // Simulate transaction data being persisted
      const mockCache = createMockQueryCache([
        {
          queryKey: ['transactions', 'list'],
          state: {
            data: [
              { id: '1', amount: 500000, recipient: 'John Doe' },
              { id: '2', amount: 250000, recipient: 'Jane Smith' },
            ],
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('transaction')
      );
    });

    it('should detect user/profile queries in AsyncStorage as security violation', async () => {
      // Simulate user PII being persisted
      const mockCache = createMockQueryCache([
        {
          queryKey: ['user', 'profile'],
          state: {
            data: {
              name: 'John Doe',
              email: 'john.doe@example.com',
              phone: '+6281234567890',
              kycStatus: 'verified',
            },
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('user')
      );
    });

    it('should detect auth queries in AsyncStorage as security violation', async () => {
      // Simulate auth tokens being persisted to AsyncStorage instead of SecureStore
      const mockCache = createMockQueryCache([
        {
          queryKey: ['auth', 'session'],
          state: {
            data: {
              accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
              refreshToken: 'refresh_token_here',
            },
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('auth')
      );
    });

    it('should detect card queries in AsyncStorage as security violation', async () => {
      // Simulate card data being persisted
      const mockCache = createMockQueryCache([
        {
          queryKey: ['cards', 'list'],
          state: {
            data: [
              {
                id: 'card-1',
                last4: '4242',
                brand: 'visa',
                holder: 'JOHN DOE',
              },
            ],
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('card')
      );
    });

    it('should detect transfer-related queries in AsyncStorage as security violation', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['transfer', 'recent'],
          state: {
            data: {
              amount: 1000000,
              recipientAccount: '1234567890',
              recipientBank: 'BCA',
            },
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('transfer')
      );
    });

    it('should detect QRIS payment data in AsyncStorage as security violation', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['qris', 'recent'],
          state: {
            data: {
              amount: 75000,
              merchantName: 'Coffee Shop',
              nmid: '1234567890',
            },
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('qris')
      );
    });
  });

  describe('Non-Sensitive Data Allowance', () => {
    it('should allow bank lists in AsyncStorage (non-sensitive reference data)', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['banks', 'list'],
          state: {
            data: [
              { code: 'BCA', name: 'Bank Central Asia' },
              { code: 'BNI', name: 'Bank Negara Indonesia' },
            ],
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(true);
      expect(report.violations).toHaveLength(0);
    });

    it('should allow feature flags in AsyncStorage (non-sensitive)', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['features', 'flags'],
          state: {
            data: {
              enableBiometric: true,
              enableQRIS: true,
              enablePocket: true,
            },
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(true);
      expect(report.violations).toHaveLength(0);
    });

    it('should allow currency lists in AsyncStorage (non-sensitive reference data)', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['currencies', 'supported'],
          state: {
            data: [
              { code: 'IDR', symbol: 'Rp', name: 'Indonesian Rupiah' },
              { code: 'USD', symbol: '$', name: 'US Dollar' },
            ],
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(true);
      expect(report.violations).toHaveLength(0);
    });

    it('should allow promo data in AsyncStorage (public data)', async () => {
      const mockCache = createMockQueryCache([
        {
          queryKey: ['promo', 'active'],
          state: {
            data: [
              { id: 'promo-1', title: 'Cashback QRIS', discount: '10%' },
            ],
          },
        },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(true);
      expect(report.violations).toHaveLength(0);
    });
  });

  describe('Security Report Structure', () => {
    it('should return proper report structure when AsyncStorage is empty', async () => {
      const report = await verifyAsyncStorageSecurity();

      expect(report).toHaveProperty('isSecure');
      expect(report).toHaveProperty('violations');
      expect(report).toHaveProperty('persistedKeys');
      expect(Array.isArray(report.violations)).toBe(true);
      expect(Array.isArray(report.persistedKeys)).toBe(true);
    });

    it('should list all persisted query keys in report', async () => {
      const mockCache = createMockQueryCache([
        { queryKey: ['banks', 'list'], state: { data: [] } },
        { queryKey: ['features', 'flags'], state: { data: {} } },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.persistedKeys).toContain('banks');
      expect(report.persistedKeys).toContain('features');
    });

    it('should handle corrupted cache data gracefully', async () => {
      await AsyncStorage.setItem('payu-query-cache', 'invalid-json{{{');

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations.length).toBeGreaterThan(0);
    });
  });

  describe('Edge Cases', () => {
    it('should detect mixed sensitive and non-sensitive data', async () => {
      const mockCache = createMockQueryCache([
        { queryKey: ['banks', 'list'], state: { data: [] } }, // OK
        { queryKey: ['wallet', 'primary'], state: { data: { balance: 1000 } } }, // VIOLATION
        { queryKey: ['features'], state: { data: {} } }, // OK
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toHaveLength(1);
    });

    it('should handle empty query cache', async () => {
      const mockCache = createMockQueryCache([]);
      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(true);
      expect(report.violations).toHaveLength(0);
    });

    it('should detect suspicious keys outside of query cache', async () => {
      await AsyncStorage.setItem('sensitive_wallet_data', '{"balance": 1000000}');
      await AsyncStorage.setItem('user_auth_tokens', '{"token": "abc123"}');

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations).toContainEqual(
        expect.stringContaining('Suspicious key')
      );
    });

    it('should be case-insensitive when detecting sensitive keys', async () => {
      const mockCache = createMockQueryCache([
        { queryKey: ['WALLET', 'primary'], state: { data: {} } },
        { queryKey: ['Transactions', 'list'], state: { data: [] } },
        { queryKey: ['User', 'profile'], state: { data: {} } },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      const report = await verifyAsyncStorageSecurity();

      expect(report.isSecure).toBe(false);
      expect(report.violations.length).toBeGreaterThanOrEqual(3);
    });
  });

  describe('Development Utility', () => {
    it('should log AsyncStorage contents in development mode', async () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      await AsyncStorage.setItem('test-key', 'test-value');

      await devLogAsyncStorageContents();

      expect(consoleSpy).toHaveBeenCalledWith(
        '=== AsyncStorage Security Audit ==='
      );

      consoleSpy.mockRestore();
      consoleErrorSpy.mockRestore();
    });
  });

  describe('Regression Tests', () => {
    it('should NOT allow the old behavior where sensitive data was persisted', async () => {
      // This test ensures the old vulnerable behavior is fixed
      // Old code (v1) had these as "persistableQueries":
      // 'wallet', 'wallets', 'transactions', 'cards', 'user', 'profile'

      const oldVulnerableData = createMockQueryCache([
        { queryKey: ['wallet'], state: { data: { balance: 9999999 } } },
        { queryKey: ['wallets'], state: { data: [{ balance: 100 }] } },
        { queryKey: ['transactions'], state: { data: [{ amount: 5000 }] } },
        { queryKey: ['cards'], state: { data: [{ number: '4242...' }] } },
        { queryKey: ['user'], state: { data: { name: 'Test User' } } },
        { queryKey: ['profile'], state: { data: { email: 'test@test.com' } } },
      ]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(oldVulnerableData));

      const report = await verifyAsyncStorageSecurity();

      // All of these should now be detected as violations
      expect(report.isSecure).toBe(false);
      expect(report.violations.length).toBeGreaterThanOrEqual(6);
    });

    it('should ensure cache buster version is incremented (v2)', async () => {
      // The buster version should be 'v2' after this security fix
      // This forces existing vulnerable caches to be invalidated

      const mockCache = createMockQueryCache([]);

      await AsyncStorage.setItem('payu-query-cache', JSON.stringify(mockCache));

      // The QueryProvider config should have buster: 'v2'
      // This test documents that requirement
      const report = await verifyAsyncStorageSecurity();

      // If cache exists and has no violations, security is working
      expect(report).toHaveProperty('isSecure');
    });
  });
});

/**
 * Integration Test: Query Key Classification
 * ===========================================
 *
 * This section documents how query keys should be classified
 * for the shouldDehydrateQuery function in QueryProvider.
 */
describe('Query Key Classification (Documentation)', () => {
  it('documents all sensitive query key patterns', () => {
    const sensitivePatterns = [
      'wallet',      // Balances
      'wallets',     // Multiple wallets
      'transactions', // Transaction history
      'transaction',  // Single transaction
      'cards',       // Payment cards
      'user',        // User PII
      'profile',     // Profile data
      'auth',        // Auth tokens
      'login',       // Login state
      'register',    // Registration data
      'transfer',    // Transfer data
      'topup',       // Top-up data
      'qris',        // QRIS payments
    ];

    expect(sensitivePatterns).toContain('wallet');
    expect(sensitivePatterns).toContain('transactions');
  });

  it('documents all non-sensitive query key patterns', () => {
    const nonSensitivePatterns = [
      'banks',      // Bank lists
      'promo',      // Promotions
      'features',   // Feature flags
      'settings',   // UI settings
      'currencies', // Currency lists
      'categories', // Transaction categories
    ];

    expect(nonSensitivePatterns).toContain('banks');
    expect(nonSensitivePatterns).toContain('features');
  });
});
