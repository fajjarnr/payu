# State Management Architecture

> **TD-MOB-001 Resolution**: Unified State Management Pattern

## Overview

This document describes the unified state management architecture for PayU Mobile App, resolving the technical debt of duplicate state management between Zustand and TanStack Query.

## Architecture Principles

### 1. Clear Separation of Concerns

| State Type | Technology | Responsibility |
|------------|------------|----------------|
| **Server State** | TanStack Query | API data, caching, synchronization |
| **UI State** | Zustand | Theme, language, selections, view preferences |
| **Token Storage** | SecureStore | Encrypted token storage only |

### 2. No Duplication Rule

- **NEVER** store the same data in both Zustand and TanStack Query
- Server data belongs exclusively in TanStack Query cache
- UI preferences belong exclusively in Zustand

### 3. Security First

- Tokens are **NEVER** stored in React state, Zustand, or React Query cache
- Tokens are stored **ONLY** in SecureStore (encrypted)
- Sensitive data is automatically excluded from persistence

## Directory Structure

```
frontend/mobile/
├── store/                    # Zustand stores (UI state only)
│   ├── index.ts             # Centralized exports
│   ├── uiStore.ts           # Theme, language, UI preferences
│   ├── cardUIStore.ts       # Card UI state (selection, view mode)
│   └── authStore.ts         # Auth UI state (deprecated, minimal)
│
├── src/hooks/               # TanStack Query hooks (server state)
│   ├── index.ts             # Centralized exports
│   ├── useAuthQuery.ts      # Auth mutations & queries
│   ├── useWalletQuery.ts    # Wallet data & mutations
│   ├── useTransactionQuery.ts # Transaction data & mutations
│   └── useCardQuery.ts      # Card data & mutations
│
├── hooks/                   # Custom hooks (composition layer)
│   ├── useAuth.ts           # Unified auth hook
│   ├── useCards.ts          # Unified cards hook
│   └── ...
│
└── context/                 # React Context (minimal)
    └── AuthContext.tsx      # Auth provider & route protection
```

## Usage Patterns

### Server State (TanStack Query)

```typescript
// Fetching data
import { usePrimaryWallet, useTransactions } from '@/src/hooks';

function WalletScreen() {
  const { data: wallet, isLoading } = usePrimaryWallet();
  const { data: transactions } = useTransactions();
}

// Mutations
import { useCreateTransfer } from '@/src/hooks';

function TransferScreen() {
  const transfer = useCreateTransfer();

  const handleTransfer = async (data) => {
    await transfer.mutateAsync(data);
  };
}
```

### UI State (Zustand)

```typescript
// UI preferences
import { useUIStore, selectShowBalance } from '@/store';

function BalanceCard() {
  const showBalance = useUIStore(selectShowBalance);
  const setShowBalance = useUIStore((state) => state.setShowBalance);
}

// Card UI state
import { useCardUIStore } from '@/store';

function CardsScreen() {
  const { selectedCardId, selectCard } = useCardUIStore();
}
```

### Unified Hooks (Composition)

```typescript
// Use unified hooks for complex interactions
import { useAuth } from '@/hooks/useAuth';
import { useCards } from '@/hooks/useCards';

function App() {
  const { user, isAuthenticated, login, logout } = useAuth();
  const { cards, selectedCard, selectCard } = useCards();
}
```

## Migration Guide

### From Old Pattern (Zustand for everything)

```typescript
// OLD: Using Zustand for server state (DEPRECATED)
import { useAuthStore } from '@/store/authStore';

function Component() {
  const { user, login, isLoading } = useAuthStore();
}
```

### To New Pattern (TanStack Query for server state)

```typescript
// NEW: Using TanStack Query for server state
import { useAuth } from '@/hooks/useAuth';

function Component() {
  const { user, login, isLoading } = useAuth();
}
```

## Store Details

### UI Store (`store/uiStore.ts`)

**Purpose**: Client-side UI preferences

**State**:
- `colorScheme`: 'light' | 'dark' | 'system'
- `language`: 'en' | 'id'
- `showBalance`: boolean
- `notificationsEnabled`: boolean
- `biometricsEnabled`: boolean
- `autoLockEnabled`: boolean
- `autoLockTimeout`: number

**Persistence**: AsyncStorage (non-sensitive data only)

### Card UI Store (`store/cardUIStore.ts`)

**Purpose**: Card-related UI state

**State**:
- `selectedCardId`: string | null
- `cardViewMode`: 'grid' | 'list'
- `showCardDetails`: boolean

**Persistence**: AsyncStorage (selectedCardId not persisted)

### Auth UI Store (`store/authStore.ts`)

**Purpose**: Auth-related UI preferences only (DEPRECATED for auth state)

**State**:
- `lastLoginAttempt`: number | null (rate limiting UI)
- `biometricPromptEnabled`: boolean

**Note**: Auth state (user, isAuthenticated) is now in TanStack Query

## Query Hooks Details

### Auth Queries (`src/hooks/useAuthQuery.ts`)

**Queries**:
- `useAuthState()`: Get auth state from cache
- `useInitializeAuth()`: Initialize auth from SecureStore

**Mutations**:
- `useLogin()`: Login with credentials
- `useRegister()`: Register new user
- `useLogout()`: Logout and clear cache
- `useRefreshToken()`: Refresh access token
- `useChangePassword()`: Change password
- `useRequestPasswordReset()`: Request password reset
- `useResetPassword()`: Reset password with token
- `useVerifyEmail()`: Verify email address

### Wallet Queries (`src/hooks/useWalletQuery.ts`)

**Queries**:
- `useWallets()`: Get all wallets
- `usePrimaryWallet()`: Get primary wallet
- `useWallet(id)`: Get specific wallet

**Mutations**:
- `useCreatePocket()`: Create new pocket
- `useTransferToPocket()`: Transfer between pockets

### Transaction Queries (`src/hooks/useTransactionQuery.ts`)

**Queries**:
- `useTransactions()`: Get paginated transactions
- `useInfiniteTransactions()`: Get transactions with infinite scroll
- `useTransaction(id)`: Get specific transaction
- `useTransactionSummary()`: Get transaction summary

**Mutations**:
- `useCreateTransfer()`: Create bank transfer
- `useTopUp()`: Top up wallet
- `usePayQRIS()`: Pay with QRIS

### Card Queries (`hooks/useCardQuery.ts`)

**Queries**:
- `useCards()`: Get all cards

**Mutations**:
- `useCreateCard()`: Create new card
- `useCardActions()`: Freeze/unfreeze cards

## Security Considerations

### Token Storage

```typescript
// Tokens are NEVER in React state
// They are stored ONLY in SecureStore

// API layer reads tokens directly from SecureStore
api.interceptors.request.use(async (config) => {
  const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);
  if (tokens?.accessToken) {
    config.headers.Authorization = `Bearer ${tokens.accessToken}`;
  }
  return config;
});
```

### Cache Configuration

```typescript
// Sensitive data is never cached
export const authKeys = {
  all: ['auth'] as const,
  user: () => [...authKeys.all, 'user'] as const,
  // SECURITY: No 'tokens' key - tokens are NEVER in React Query cache
};
```

## Testing

### Testing UI State (Zustand)

```typescript
import { useUIStore } from '@/store';

beforeEach(() => {
  useUIStore.setState({
    showBalance: true,
    colorScheme: 'system',
  });
});
```

### Testing Server State (TanStack Query)

```typescript
import { renderHook, waitFor } from '@testing-library/react-native';
import { usePrimaryWallet } from '@/src/hooks';
import { QueryProvider } from '@/src/providers/QueryProvider';

const wrapper = ({ children }) => (
  <QueryProvider>{children}</QueryProvider>
);

const { result } = renderHook(() => usePrimaryWallet(), { wrapper });
await waitFor(() => expect(result.current.isSuccess).toBe(true));
```

## Best Practices

1. **Always use selectors** for Zustand to prevent unnecessary re-renders
2. **Use query keys consistently** for TanStack Query invalidation
3. **Keep server state in React Query** - don't duplicate in Zustand
4. **Keep UI state in Zustand** - don't put in React Query cache
5. **Use unified hooks** for complex compositions
6. **Never store tokens** in any state management solution

## Troubleshooting

### Issue: Data not syncing between components

**Solution**: Ensure you're using the same query keys for TanStack Query

### Issue: UI state not persisting

**Solution**: Check that the Zustand store has the persist middleware configured

### Issue: Too many re-renders

**Solution**: Use selectors for Zustand and proper dependency arrays for React Query

## References

- [TanStack Query Documentation](https://tanstack.com/query/latest)
- [Zustand Documentation](https://docs.pmnd.rs/zustand)
- [Expo SecureStore Documentation](https://docs.expo.dev/versions/latest/sdk/securestore/)
