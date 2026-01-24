import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useExperiment } from '@/hooks/useExperiment';
import { ExperimentProvider } from '@/contexts/ExperimentContext';
import ABTestingService from '@/services/ABTestingService';
import { ExperimentStatus, AllocationStrategy } from '@/services/ABTestingService';

// Mock ABTestingService
vi.mock('@/services/ABTestingService');

// Mock useAuth
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: 'test-user-id' },
  }),
}));

describe('useExperiment', () => {
  const mockExperiment = {
    id: 'exp-1',
    key: 'test_experiment',
    name: 'Test Experiment',
    description: 'A test experiment',
    status: ExperimentStatus.RUNNING,
    allocationStrategy: AllocationStrategy.MODULO,
    trafficPercentage: 100,
    targetAudience: {},
    variants: [
      {
        id: 'var-1',
        experimentId: 'exp-1',
        key: 'control',
        name: 'Control',
        description: 'Control variant',
        isControl: true,
        allocationWeight: 50,
        config: {},
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'var-2',
        experimentId: 'exp-1',
        key: 'variant_b',
        name: 'Variant B',
        description: 'Test variant B',
        isControl: false,
        allocationWeight: 50,
        config: {},
        createdAt: '2024-01-01T00:00:00Z',
      },
    ],
    startDate: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  const mockAssignment = {
    id: 'assign-1',
    experimentId: 'exp-1',
    experimentKey: 'test_experiment',
    variantId: 'var-1',
    variantKey: 'control',
    userId: 'test-user-id',
    assignedAt: '2024-01-01T00:00:00Z',
    variant: mockExperiment.variants[0],
  };

  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <ExperimentProvider>{children}</ExperimentProvider>
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should fetch experiment and assign variant', async () => {
    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);
    vi.mocked(ABTestingService.assignVariant).mockResolvedValue(mockAssignment);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);
    vi.mocked(ABTestingService.trackConversion).mockResolvedValue(undefined);

    const { result } = renderHook(() => useExperiment('test_experiment'), {
      wrapper,
    });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.variantKey).toBe('control');
    expect(result.current.assignment).toEqual(mockAssignment);
    expect(ABTestingService.getExperimentByKey).toHaveBeenCalledWith('test_experiment');
    expect(ABTestingService.assignVariant).toHaveBeenCalledWith(
      'test_experiment',
      'test-user-id',
      undefined
    );
  });

  it('should use cached variant if available', async () => {
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue('variant_b');
    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);

    const { result } = renderHook(() => useExperiment('test_experiment'), {
      wrapper,
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.variantKey).toBe('variant_b');
    expect(ABTestingService.assignVariant).not.toHaveBeenCalled();
  });

  it('should track impression on mount', async () => {
    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);
    vi.mocked(ABTestingService.assignVariant).mockResolvedValue(mockAssignment);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);
    vi.mocked(ABTestingService.trackConversion).mockResolvedValue(undefined);

    renderHook(() => useExperiment('test_experiment', { trackImpression: true }), {
      wrapper,
    });

    await waitFor(() => {
      expect(ABTestingService.trackConversion).toHaveBeenCalledWith(
        'exp-1',
        'test-user-id',
        'impression',
        undefined,
        { variantKey: 'control', experimentKey: 'test_experiment' }
      );
    });
  });

  it('should not track impression when disabled', async () => {
    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);
    vi.mocked(ABTestingService.assignVariant).mockResolvedValue(mockAssignment);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);
    vi.mocked(ABTestingService.trackConversion).mockResolvedValue(undefined);

    renderHook(() => useExperiment('test_experiment', { trackImpression: false }), {
      wrapper,
    });

    await waitFor(() => {
      expect(ABTestingService.assignVariant).toHaveBeenCalled();
    });

    expect(ABTestingService.trackConversion).not.toHaveBeenCalled();
  });

  it('should track conversions', async () => {
    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);
    vi.mocked(ABTestingService.assignVariant).mockResolvedValue(mockAssignment);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);
    vi.mocked(ABTestingService.trackConversion).mockResolvedValue(undefined);

    const { result } = renderHook(
      () => useExperiment('test_experiment', { trackImpression: false }),
      { wrapper }
    );

    await waitFor(() => {
      expect(result.current.assignment).not.toBeNull();
    });

    await result.current.trackConversion('purchase_completed', { amount: 100000 });

    expect(ABTestingService.trackConversion).toHaveBeenCalledWith(
      'exp-1',
      'test-user-id',
      'purchase_completed',
      undefined,
      { amount: 100000 }
    );
  });

  it('should call onVariantAssigned callback', async () => {
    const onVariantAssigned = vi.fn();

    vi.mocked(ABTestingService.getExperimentByKey).mockResolvedValue(mockExperiment);
    vi.mocked(ABTestingService.assignVariant).mockResolvedValue(mockAssignment);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);
    vi.mocked(ABTestingService.trackConversion).mockResolvedValue(undefined);

    renderHook(
      () =>
        useExperiment('test_experiment', {
          trackImpression: false,
          onVariantAssigned,
        }),
      { wrapper }
    );

    await waitFor(() => {
      expect(onVariantAssigned).toHaveBeenCalledWith('control');
    });
  });

  it('should handle errors', async () => {
    const error = new Error('Network error');
    vi.mocked(ABTestingService.getExperimentByKey).mockRejectedValue(error);
    vi.mocked(ABTestingService.getCachedVariant).mockReturnValue(null);

    const onError = vi.fn();

    const { result } = renderHook(
      () => useExperiment('test_experiment', { onError }),
      { wrapper }
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toEqual(error);
    expect(onError).toHaveBeenCalledWith(error);
  });
});
