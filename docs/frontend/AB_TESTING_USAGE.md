# A/B Testing Frontend Integration Guide

This guide explains how to use the A/B testing infrastructure in the PayU web application.

## Overview

The A/B testing system consists of:

1. **ABTestingService** - Service for communicating with the backend
2. **useExperiment hook** - React hook for fetching and using variants
3. **ExperimentContext** - Global context for managing experiment state
4. **ExperimentVariant component** - Component for rendering variant-specific UI
5. **FeatureFlag component** - Component for feature flagging

## Setup

The `ExperimentProvider` is already included in `/home/ubuntu/payu/frontend/web-app/src/app/providers.tsx`. No additional setup is required.

## Basic Usage

### Using the `useExperiment` Hook

```tsx
import { useExperiment } from '@/hooks';

function CheckoutButton() {
  const { variantKey, isLoading } = useExperiment('checkout_flow_v2');

  if (isLoading) {
    return <SkeletonButton />;
  }

  return (
    <button className={variantKey === 'variant_b' ? 'btn-green' : 'btn-blue'}>
      Checkout
    </button>
  );
}
```

### Using the `ExperimentVariant` Component

```tsx
import { ExperimentVariant } from '@/components/experiments';

function CheckoutPage() {
  return (
    <ExperimentVariant experimentKey="checkout_flow_v2" fallback={<OldCheckout />}>
      {{
        control: <OldCheckout />,
        variant_b: <NewCheckout />,
        variant_c: <SimplifiedCheckout />,
      }}
    </ExperimentVariant>
  );
}
```

### Using the `FeatureFlag` Component

```tsx
import { FeatureFlag } from '@/components/experiments';

function Dashboard() {
  return (
    <FeatureFlag
      experimentKey="new_dashboard_enabled"
      enabledVariant="enabled"
      fallback={<OldDashboard />}
    >
      <NewDashboard />
    </FeatureFlag>
  );
}
```

## Advanced Usage

### Tracking Conversions

```tsx
function PurchaseComplete() {
  const { variantKey, trackConversion } = useExperiment('checkout_flow_v2');

  const handlePurchase = async () => {
    // Complete purchase logic...

    // Track conversion
    await trackConversion('purchase_completed', {
      amount: 100000,
      currency: 'IDR',
    });
  };

  return <button onClick={handlePurchase}>Complete Purchase</button>;
}
```

### Custom User ID

```tsx
function MyComponent() {
  const { variantKey } = useExperiment('experiment_key', {
    userId: 'custom-user-id',
    deviceId: 'device-123',
  });

  // ...
}
```

### Callbacks

```tsx
function MyComponent() {
  const { variantKey } = useExperiment('experiment_key', {
    onVariantAssigned: (variantKey) => {
      console.log(`Assigned to variant: ${variantKey}`);
    },
    onError: (error) => {
      console.error('Failed to load experiment:', error);
    },
  });

  // ...
}
```

## API Reference

### `useExperiment`

Hook for fetching and using A/B test variant assignments.

**Parameters:**
- `experimentKey: string` - The experiment key
- `options?: UseExperimentOptions` - Configuration options

**Options:**
- `trackImpression?: boolean` - Whether to track impression on mount (default: true)
- `userId?: string` - Custom user ID
- `deviceId?: string` - Custom device ID
- `refetchOnMount?: boolean` - Whether to re-fetch on mount (default: false)
- `onVariantAssigned?: (variantKey: string) => void` - Callback when variant is assigned
- `onError?: (error: Error) => void` - Callback when experiment fails

**Returns:**
```typescript
{
  variantKey: string | null;
  assignment: VariantAssignment | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  trackConversion: (eventType: string, properties?: Record<string, unknown>) => Promise<void>;
  refetch: () => void;
}
```

### `ExperimentVariant` Component

**Props:**
```typescript
{
  experimentKey: string;
  children: Record<string, ReactNode>;
  fallback?: ReactNode;
  errorFallback?: ReactNode;
  defaultVariant?: string;
  experimentOptions?: UseExperimentOptions;
}
```

### `FeatureFlag` Component

**Props:**
```typescript
{
  experimentKey: string;
  enabledVariant: string;
  children: ReactNode;
  fallback?: ReactNode;
  trackImpression?: boolean;
  onAccess?: (enabled: boolean, variantKey: string | null) => void;
}
```

## Backend Integration

The frontend communicates with the `ab-testing-service` backend via these endpoints:

- `GET /api/v1/experiments/key/{key}` - Fetch experiment by key
- `POST /api/v1/experiments/{key}/assign` - Get variant assignment
- `POST /api/v1/experiments/{id}/track` - Track conversion event

## Caching

Variant assignments are cached in `localStorage` with a 30-minute TTL. This ensures:
- Consistent user experience across page refreshes
- Reduced API calls
- Fast variant resolution

## Testing

### Example Test

```tsx
import { renderHook, act } from '@testing-library/react';
import { useExperiment } from '@/hooks/useExperiment';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock the ABTestingService
jest.mock('@/services/ABTestingService');

test('assigns variant to user', async () => {
  const queryClient = new QueryClient();
  const wrapper = ({ children }) => (
    <QueryClientProvider client={queryClient}>
      <ExperimentProvider>{children}</ExperimentProvider>
    </QueryClientProvider>
  );

  const { result } = renderHook(() => useExperiment('test_experiment'), {
    wrapper,
  });

  expect(result.current.isLoading).toBe(true);

  await act(async () => {
    await waitFor(() => expect(result.current.isLoading).toBe(false));
  });

  expect(result.current.variantKey).toBe('variant_a');
});
```

## Best Practices

1. **Always provide a fallback** - Handle loading and error states gracefully
2. **Use meaningful variant keys** - e.g., `control`, `variant_b`, `simplified_flow`
3. **Track conversions** - Use `trackConversion` for meaningful events
4. **Don't rely on experiments for critical features** - Always have a working default
5. **Test locally** - Use `forceVariantKey` in backend for testing
6. **Clean up experiments** - Remove old experiments after completion

## Migration from Old Code

If you have existing A/B testing logic:

```tsx
// Old code
const isNewFlow = Math.random() > 0.5;

// New code
const { variantKey } = useExperiment('new_flow_experiment');
const isNewFlow = variantKey === 'variant_b';
```

## Troubleshooting

**Variant not loading:**
- Check if the experiment key is correct
- Verify the backend service is running
- Check browser console for errors

**Inconsistent variant across refreshes:**
- Check localStorage caching
- Verify user ID is consistent
- Check backend allocation strategy

**Conversion not tracking:**
- Ensure `trackConversion` is called with correct event type
- Check network tab for failed requests
- Verify experiment ID is correct
