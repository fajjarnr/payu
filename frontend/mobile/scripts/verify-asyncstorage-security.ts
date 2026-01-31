#!/usr/bin/env tsx

/**
 * AsyncStorage Security Verification Script
 * ===========================================
 *
 * This script audits AsyncStorage to ensure no sensitive financial or PII data
 * is being persisted (violating PCI-DSS and OJK compliance).
 *
 * Usage:
 *   npx tsx scripts/verify-asyncstorage-security.ts
 *
 * Exit codes:
 *   0 - No security violations found
 *   1 - Security violations detected
 *   2 - Script error
 *
 * @version 2.0.0
 * @compliance PCI-DSS, OJK, PayU Security Policy P2-C2
 */

import AsyncStorage from '@react-native-async-storage/async-storage';

// Sensitive query keys that must NOT be in AsyncStorage
const SENSITIVE_QUERY_KEYS = [
  'wallet',
  'wallets',
  'transactions',
  'transaction',
  'cards',
  'user',
  'profile',
  'auth',
  'login',
  'register',
  'transfer',
  'topup',
  'qris',
] as const;

interface SecurityReport {
  isSecure: boolean;
  violations: string[];
  persistedKeys: string[];
  summary: {
    totalKeys: number;
    sensitiveKeys: number;
    nonSensitiveKeys: number;
  };
}

async function auditAsyncStorage(): Promise<SecurityReport> {
  const violations: string[] = [];
  const persistedKeys: string[] = [];
  let sensitiveKeys = 0;

  try {
    console.log('Auditing AsyncStorage for security compliance...\n');

    // Get all keys from AsyncStorage
    const keys = await AsyncStorage.getAllKeys();
    console.log(`Total keys found: ${keys.length}`);

    // Check React Query cache specifically
    const queryCacheKey = 'payu-query-cache';
    if (keys.includes(queryCacheKey)) {
      const cacheData = await AsyncStorage.getItem(queryCacheKey);

      if (cacheData) {
        try {
          const parsedCache = JSON.parse(cacheData);
          const queries = parsedCache?.clientState?.queries || [];

          console.log(`\nQuery cache contains ${queries.length} queries:`);

          queries.forEach((query: { queryKey: string[] }) => {
            const queryKey = query?.queryKey?.[0]?.toString().toLowerCase() || '';
            persistedKeys.push(queryKey);

            // Check if sensitive data is persisted
            const isSensitive = SENSITIVE_QUERY_KEYS.some((key) =>
              queryKey.includes(key.toLowerCase())
            );

            if (isSensitive) {
              sensitiveKeys++;
              violations.push(
                `SECURITY VIOLATION: Sensitive query "${queryKey}" found in AsyncStorage!`
              );
            }
          });
        } catch (e) {
          violations.push('Failed to parse query cache for security check');
        }
      }
    }

    // Check for any other suspicious keys
    const suspiciousKeys = keys.filter((key) =>
      SENSITIVE_QUERY_KEYS.some((sensitive) =>
        key.toLowerCase().includes(sensitive.toLowerCase())
      )
    );

    suspiciousKeys.forEach((key) => {
      if (key !== queryCacheKey) {
        violations.push(`Suspicious key in AsyncStorage: ${key}`);
      }
    });

    const report: SecurityReport = {
      isSecure: violations.length === 0,
      violations,
      persistedKeys,
      summary: {
        totalKeys: keys.length,
        sensitiveKeys,
        nonSensitiveKeys: persistedKeys.length - sensitiveKeys,
      },
    };

    return report;
  } catch (error) {
    violations.push(`Security check failed: ${error}`);
    return {
      isSecure: false,
      violations,
      persistedKeys,
      summary: {
        totalKeys: 0,
        sensitiveKeys: 0,
        nonSensitiveKeys: 0,
      },
    };
  }
}

function printReport(report: SecurityReport): void {
  console.log('\n' + '='.repeat(60));
  console.log('SECURITY AUDIT REPORT');
  console.log('='.repeat(60));

  console.log('\nSummary:');
  console.log(`  Status: ${report.isSecure ? '\u2714 SECURE' : '\u2718 NOT SECURE'}`);
  console.log(`  Total AsyncStorage keys: ${report.summary.totalKeys}`);
  console.log(`  Cached queries: ${report.persistedKeys.length}`);
  console.log(`  Sensitive queries: ${report.summary.sensitiveKeys}`);
  console.log(`  Non-sensitive queries: ${report.summary.nonSensitiveKeys}`);

  if (report.persistedKeys.length > 0) {
    console.log('\nPersisted Query Keys:');
    report.persistedKeys.forEach((key) => {
      const isSensitive = SENSITIVE_QUERY_KEYS.some((s) =>
        key.includes(s.toLowerCase())
      );
      const status = isSensitive ? ' [SENSITIVE]' : '';
      console.log(`  - ${key}${status}`);
    });
  }

  if (report.violations.length > 0) {
    console.log('\n' + '='.repeat(60));
    console.log('SECURITY VIOLATIONS:');
    console.log('='.repeat(60));
    report.violations.forEach((violation, index) => {
      console.log(`\n${index + 1}. ${violation}`);
    });
  }

  console.log('\n' + '='.repeat(60));

  if (!report.isSecure) {
    console.log('\nACTION REQUIRED:');
    console.log(
      '  Sensitive data (financial, PII, auth) MUST NOT be stored in AsyncStorage.'
    );
    console.log('  Use SecureStore for sensitive data instead.');
    console.log('  Update QueryProvider.tsx shouldDehydrateQuery function.');
    console.log('\nCompliance References:');
    console.log('  - PCI-DSS Requirement 3: Protect stored cardholder data');
    console.log('  - OJK Regulation: Financial data encryption at rest');
    console.log('  - PayU Security Policy P2-C2: Secure token storage');
  }

  console.log('\n' + '='.repeat(60));
}

async function main() {
  const report = await auditAsyncStorage();
  printReport(report);

  process.exit(report.isSecure ? 0 : 1);
}

// Only run if executed directly
if (require.main === module) {
  main().catch((error) => {
    console.error('Script error:', error);
    process.exit(2);
  });
}

export { auditAsyncStorage };
export type { SecurityReport };
