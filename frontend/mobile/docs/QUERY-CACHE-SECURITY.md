# React Query Cache Security - PayU Mobile App

## Security Issue: Sensitive Data in AsyncStorage

### Severity: **CRITICAL**

### Summary

The React Query cache was configured to persist sensitive financial and PII (Personally Identifiable Information) data to AsyncStorage, which is **unencrypted storage**. This violated PCI-DSS, OJK regulations, and PayU security policies.

### Affected Data (Previously Vulnerable)

| Data Type | Query Keys | Risk Level |
|-----------|------------|------------|
| **Wallet Balances** | `['wallet']`, `['wallets']` | **HIGH** - Financial data |
| **Transaction History** | `['transactions']`, `['transaction']` | **HIGH** - Financial data |
| **User Profile** | `['user']`, `['profile']` | **HIGH** - PII |
| **Card Details** | `['cards']` | **CRITICAL** - PCI-DSS violation |
| **Auth Tokens** | `['auth']`, `['login']` | **CRITICAL** - Credentials |
| **Transfer Data** | `['transfer']`, `['topup']` | **HIGH** - Financial data |
| **QRIS Payments** | `['qris']` | **HIGH** - Financial data |

### Root Cause

In `src/providers/QueryProvider.tsx` (before fix):

```typescript
// OLD VULNERABLE CODE (DO NOT USE)
const persistableQueries = [
  'wallet',
  'wallets',
  'transactions',
  'cards',
  'user',
  'profile',
];
return persistableQueries.some((key) =>
  query.queryKey[0]?.toString().startsWith(key)
);
```

This code **explicitly persisted** sensitive queries to AsyncStorage.

### Why This Is a Security Issue

1. **AsyncStorage is NOT Encrypted**
   - Data is stored in plaintext on the device file system
   - Easily accessible on rooted/jailbroken devices
   - Can be extracted via USB debugging

2. **Compliance Violations**
   - **PCI-DSS Requirement 3**: Protect stored cardholder data
   - **OJK Regulation**: Financial data must be encrypted at rest
   - **PayU Security Policy P2-C2**: Use SecureStore for sensitive data

3. **Real-World Risk**
   - Malicious apps can read AsyncStorage
   - Physical device access exposes financial data
   - Backup files contain unencrypted sensitive data

---

## Security Fix Implementation

### Solution: Exclude Sensitive Queries from Persistence

**File**: `/home/ubuntu/payu/frontend/mobile/src/providers/QueryProvider.tsx`

```typescript
// NEW SECURE CODE (v2.0.0)
const SENSITIVE_QUERY_KEYS = [
  'wallet', 'wallets', 'transactions', 'transaction',
  'cards', 'user', 'profile', 'auth', 'login', 'register',
  'transfer', 'topup', 'qris',
] as const;

const NON_SENSITIVE_QUERY_KEYS = [
  'banks', 'promo', 'features', 'settings',
  'currencies', 'categories',
] as const;

dehydrateOptions: {
  shouldDehydrateQuery: (query: any) => {
    const queryKeyString = query.queryKey[0]?.toString().toLowerCase() || '';

    // SECURITY: Exclude sensitive queries from AsyncStorage persistence
    const isSensitive = SENSITIVE_QUERY_KEYS.some((key) =>
      queryKeyString.includes(key.toLowerCase())
    );
    if (isSensitive) {
      return false; // Do NOT persist to AsyncStorage
    }

    // Only persist non-sensitive queries
    const isNonSensitive = NON_SENSITIVE_QUERY_KEYS.some((key) =>
      queryKeyString.includes(key.toLowerCase())
    );

    return isNonSensitive;
  },
}
```

### Key Changes

1. **Inverted Logic**: Changed from "allowlist" to "denylist" approach
2. **Sensitive Data Excluded**: All financial/PII queries return `false`
3. **Cache Buster**: Incremented to `v2` to invalidate old vulnerable caches
4. **Comprehensive Coverage**: Added all sensitive query patterns

---

## Storage Security Architecture

### Three-Tier Storage Model

| Tier | Storage Type | Encryption | Data Types | Examples |
|------|--------------|------------|------------|----------|
| **Tier 1** | **SecureStore** | ✅ Encrypted | Credentials, Tokens | Auth tokens, User data, PIN |
| **Tier 2** | **Memory-only** | ❌ No persistence | Financial, PII | Wallets, Transactions, Cards |
| **Tier 3** | **AsyncStorage** | ❌ Plaintext | Reference only | Bank lists, Feature flags |

### Data Classification Rules

```typescript
// Tier 1: SecureStore (encrypted)
await SecureStore.setItemAsync('payu_auth_tokens', JSON.stringify(tokens));

// Tier 2: Memory-only (no persistence)
useQuery({
  queryKey: ['wallet', 'primary'],  // NOT persisted
  queryFn: () => walletService.getPrimaryWallet(),
});

// Tier 3: AsyncStorage (non-sensitive only)
useQuery({
  queryKey: ['banks', 'list'],  // CAN be persisted
  queryFn: () => bankService.getBanks(),
});
```

---

## Verification & Testing

### Automated Security Test

Run the security test suite:

```bash
cd /home/ubuntu/payu/frontend/mobile
npm test -- QueryProvider.security.test.ts
```

### Manual Verification Script

```bash
npx tsx scripts/verify-asyncstorage-security.ts
```

Expected output (secure):
```
Status: ✓ SECURE
Sensitive queries: 0
Non-sensitive queries: 3
```

If violations found:
```
Status: ✗ NOT SECURE
SECURITY VIOLATIONS:
1. SECURITY VIOLATION: Sensitive query "wallet" found in AsyncStorage!
```

### Development Utility

In development, audit AsyncStorage contents:

```typescript
import { devLogAsyncStorageContents } from '@/providers/QueryProvider';

if (__DEV__) {
  devLogAsyncStorageContents();
}
```

---

## Query Key Guidelines for Developers

### When Adding New Queries

1. **Classify your data**:
   - Financial? → Memory-only (do NOT persist)
   - PII? → Memory-only (do NOT persist)
   - Auth-related? → SecureStore (do NOT persist)
   - Reference data? → AsyncStorage (CAN persist)

2. **Use appropriate query keys**:
   ```typescript
   // ❌ WRONG - Financial data should NOT be persistable
   export const sensitiveKeys = {
     all: ['account_balance'] as const,  // Financial!
   };

   // ✅ CORRECT - Reference data CAN be persisted
   export const referenceKeys = {
     all: ['bank_list'] as const,  // Public data!
   };
   ```

3. **Update security lists** if needed:
   - Add new sensitive patterns to `SENSITIVE_QUERY_KEYS`
   - Add new non-sensitive patterns to `NON_SENSITIVE_QUERY_KEYS`
   - Run tests to verify

### Examples by Domain

| Domain | Query Key Pattern | Persist? | Reason |
|--------|-------------------|----------|--------|
| **Wallet** | `['wallet', ...]` | ❌ No | Balances are sensitive |
| **Transaction** | `['transactions', ...]` | ❌ No | Amounts/recipients are sensitive |
| **Auth** | `['auth', ...]` | ❌ No | Already in SecureStore |
| **Banks** | `['banks', ...]` | ✅ Yes | Public reference data |
| **Features** | `['features', ...]` | ✅ Yes | Feature flags are public |
| **Promos** | `['promo', ...]` | ✅ Yes | Public content |

---

## Compliance References

### PCI-DSS

- **Requirement 3**: Protect stored cardholder data
- **Requirement 3.1**: Keep cardholder data storage to a minimum
- **Requirement 3.4**: Render PAN unreadable anywhere it's stored

### OJK Regulations

- **POJK 18/2020**: Electronic wallet transactions
- **Data encryption at rest** for financial information
- **Access control** for sensitive customer data

### PayU Security Policies

- **P2-C2**: Secure token storage using SecureStore
- **P2-D1**: Data classification (Sensitive vs Non-sensitive)
- **P2-E3**: AsyncStorage for non-sensitive data only

---

## Migration Notes

### For Existing Apps

1. **Cache Invalidation**: The `buster: 'v2'` will clear old persisted data
2. **User Impact**: Users will need to re-fetch data (no data loss)
3. **Backward Compatibility**: No breaking changes to API

### Testing Checklist

- [ ] Run `QueryProvider.security.test.ts` - all tests pass
- [ ] Run `verify-asyncstorage-security.ts` - no violations
- [ ] Manual testing: Open app, check wallet data loads
- [ ] Manual testing: Close/reopen app, verify AsyncStorage is clean
- [ ] Manual testing: Verify non-sensitive data (banks, promos) persist

---

## FAQ

### Q: Why not encrypt AsyncStorage?

**A**: AsyncStorage doesn't support encryption. Use SecureStore for sensitive data, which uses iOS Keychain and Android Keystore.

### Q: Won't this break offline functionality?

**A**: No. Sensitive data stays in memory while the app is open. Only non-sensitive data (bank lists, etc.) persists for offline use.

### Q: How do I check if my query is being persisted?

**A**: Run the verification script or check the `shouldDehydrateQuery` function in `QueryProvider.tsx`.

### Q: Can I add custom encrypted storage?

**A**: Yes, but use SecureStore directly. Don't implement custom encryption - use platform-provided secure storage.

---

## Contact

For questions or security concerns:
- **Security Team**: security@payu.fajjjar.my.id
- **Architecture**: arch@payu.fajjjar.my.id
- **Documentation**: `/home/ubuntu/payu/frontend/mobile/docs/`

---

**Version**: 2.0.0
**Last Updated**: 2026-01-31
**Status**: ✅ Implemented
