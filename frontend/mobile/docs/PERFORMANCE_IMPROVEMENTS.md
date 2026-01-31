# Request Cancellation & Memoization Implementation

## Overview

This document describes the implementation of request cancellation and memoization patterns in the PayU mobile app to prevent memory leaks and improve performance.

## Problem Statement

Prior to these improvements:
- API requests continued after component unmount
- No cleanup for pending async operations
- Functions were recreated on every render
- Expensive computations ran repeatedly
- Memory leaks from unclosed resources

## Solutions Implemented

### 1. Custom Cancellation Hooks (`/hooks/useCancellableEffect.ts`)

#### `useCancellableEffect`
Prevents state updates on unmounted components.

```typescript
useCancellableEffect(async (isCancelled) => {
  const data = await fetchData();
  if (!isCancelled()) {
    setState(data);
  }
}, [dependency]);
```

#### `useAbortController`
Manages AbortController for fetch requests with automatic cleanup.

```typescript
const { abortController, signal, abort } = useAbortController();

useEffect(() => {
  const fetchData = async () => {
    try {
      const response = await fetch(url, { signal });
      // Handle response
    } catch (error) {
      if (error.name !== 'AbortError') {
        // Handle error
      }
    }
  };

  fetchData();
}, []);
```

#### `useCancellablePromise`
Hook for cancellable promises with timeout support.

```typescript
const { data, error, loading, execute, retry, cancel } = useCancellablePromise(
  (signal) => apiService.getData(signal),
  10000
);
```

### 2. API Client Improvements (`/services/api.ts`)

#### Request Deduplication
Same requests are deduplicated using AbortController:

```typescript
private pendingRequests: Map<string, AbortController> = new Map();

// Generate unique key for each request
private getRequestKey(config): string {
  return `${config.method}:${config.url}:${JSON.stringify(config.params)}`;
}
```

#### Global Cancellation
Cancel all pending requests at once:

```typescript
public cancelAllRequests() {
  this.pendingRequests.forEach((controller) => {
    controller.abort();
  });
  this.pendingRequests.clear();
}
```

### 3. Hook Updates

#### `useWallet` (`/hooks/useWallet.ts`)
- Added `isMountedRef` for cleanup tracking
- Memoized `refresh` function
- Proper async cleanup in useEffect

#### `useAuth` (`/hooks/useAuth.ts`)
- Token check interval cleanup
- Cancel intervals on logout
- Proper cleanup of token timeout references

#### `useTransactions` (`/hooks/useTransactions.ts`)
- Memoized `performTransfer` function
- Added `loadMore` with mount check
- Added `useTopUp` and `useQRISPayment` hooks

#### `useCards` (`/hooks/useCards.ts`)
- Added `refresh` function
- Mount check for async operations
- Proper cleanup on unmount

### 4. Component Improvements

#### Home Screen (`/app/(tabs)/index.tsx`)
- Memoized balance calculation
- Memoized transaction list flattening
- Memoized display transactions
- All callbacks use `useCallback`
- Mount checks for async operations
- Cleanup on unmount

```typescript
// Memoize expensive calculations
const balance = useMemo(() => wallet?.balance ?? 0, [wallet?.balance]);
const transactions = useMemo(
  () => transactionsData?.pages.flatMap(page => page.items) ?? [],
  [transactionsData]
);
const displayTransactions = useMemo(
  () => transactions.slice(0, 5),
  [transactions]
);

// Memoize all callbacks
const handleToggleBalance = useCallback(() => {
  setShowBalance(prev => !prev);
}, []);

const handleNotificationPress = useCallback(() => {
  router.push('/notifications');
}, [router]);
```

#### History Screen (`/app/(tabs)/history.tsx`)
- Uses FlashList for efficient rendering
- Memoized list data transformation
- Memoized render callbacks
- Proper cleanup of scroll handlers

#### Login Screen (`/app/(auth)/login.tsx`)
- Fixed timer cleanup for countdown
- Proper null check before clearInterval

```typescript
React.useEffect(() => {
  let interval: NodeJS.Timeout | null = null;
  if (countdown > 0) {
    interval = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);
  }
  return () => {
    if (interval) clearInterval(interval);
  };
}, [countdown]);
```

#### Profile Screen (`/app/profile.tsx`)
- Memoized settings sections
- Memoized render item callback
- Mount checks for async operations
- Proper cleanup of refs

### 5. Utility Functions (`/utils/requestCleanup.ts`)

#### Global Request Tracking
Track and cancel all pending requests:

```typescript
import { cancelAllPendingRequests } from '@/utils/requestCleanup';

// Cancel on app background
AppState.addEventListener('change', (nextAppState) => {
  if (nextAppState === 'background') {
    cancelAllPendingRequests();
  }
});
```

#### Mount Tracking
Helper for tracking component mount status:

```typescript
const { isMounted, mount, unmount } = createMountTracker();

useEffect(() => {
  mount();

  const fetchData = async () => {
    const data = await api.fetch();
    if (!isMounted()) return;
    setState(data);
  };

  fetchData();
  return unmount;
}, []);
```

#### Debounce/Throttle with Cleanup
Debounced and throttled functions that auto-cancel:

```typescript
const debouncedSearch = useDebouncedCallback(
  (query) => searchAPI(query),
  500,
  [searchAPI]
);

// Auto-cancels on unmount
```

## Performance Benefits

### Memory Leak Prevention
- No more "Can't perform a React state update on an unmounted component" warnings
- All async operations properly cancelled on unmount
- Timers and intervals properly cleaned up

### Reduced Re-renders
- Callbacks memoized with `useCallback`
- Expensive computations memoized with `useMemo`
- Component re-renders minimized

### Network Efficiency
- Duplicate requests automatically cancelled
- Request deduplication
- Global cancellation capability

### Better UX
- No stale data after navigation
- Faster screen transitions
- Reduced jank from unnecessary renders

## Best Practices

### 1. Always Cleanup Effects
```typescript
useEffect(() => {
  const controller = new AbortController();

  fetchData(controller.signal);

  return () => controller.abort();
}, []);
```

### 2. Use Mount Checks for Async
```typescript
const isMountedRef = useRef(true);

useEffect(() => {
  return () => { isMountedRef.current = false; };
}, []);

const fetchData = async () => {
  const data = await api.fetch();
  if (!isMountedRef.current) return;
  setState(data);
};
```

### 3. Memoize Expensive Computations
```typescript
const result = useMemo(() => {
  return expensiveCalculation(data);
}, [data]);
```

### 4. Memoize Callbacks
```typescript
const handleClick = useCallback(() => {
  doSomething(dependency);
}, [dependency]);
```

### 5. Cancel Pending Requests on Logout
```typescript
const handleLogout = async () => {
  cancelAllPendingRequests();
  await logout();
  router.replace('/login');
};
```

## Files Modified

### Hooks
- `/hooks/useCancellableEffect.ts` (NEW)
- `/hooks/useWallet.ts`
- `/hooks/useAuth.ts`
- `/hooks/useTransactions.ts`
- `/hooks/useCards.ts`

### Services
- `/services/api.ts`

### Screens
- `/app/(tabs)/index.tsx`
- `/app/(tabs)/history.tsx`
- `/app/(auth)/login.tsx`
- `/app/profile.tsx`
- `/app/feedback.tsx`

### Utils
- `/utils/requestCleanup.ts` (NEW)

## Testing Checklist

- [ ] Navigate between screens rapidly - no errors
- [ ] Logout while requests pending - requests cancelled
- [ ] Background app while loading - requests cancelled
- [ ] Refresh on home screen - proper cleanup
- [ ] Infinite scroll on history - proper cleanup
- [ ] Form submissions - cancellable
- [ ] No memory leaks after extended use

## Future Improvements

1. **React Query Integration**: Leverage React Query's built-in cancellation
2. **Request Queue**: Implement request queuing for better control
3. **Request Retry**: Add automatic retry with exponential backoff
4. **Offline Support**: Queue requests when offline
5. **Request Caching**: Implement aggressive caching strategies
