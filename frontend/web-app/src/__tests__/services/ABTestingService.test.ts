import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import ABTestingService, {
  ExperimentStatus,
  AllocationStrategy,
  type VariantAssignment,
} from '@/services/ABTestingService';

// Mock the api module
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import api from '@/lib/api';

describe('ABTestingService', () => {
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

  const mockAssignment: VariantAssignment = {
    id: 'assign-1',
    experimentId: 'exp-1',
    experimentKey: 'test_experiment',
    variantId: 'var-1',
    variantKey: 'control',
    userId: 'user-1',
    assignedAt: '2024-01-01T00:00:00Z',
    variant: mockExperiment.variants[0],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    // Clear localStorage
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('getExperimentByKey', () => {
    it('should fetch experiment by key', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockExperiment });

      const result = await ABTestingService.getExperimentByKey('test_experiment');

      expect(api.get).toHaveBeenCalledWith('/experiments/key/test_experiment');
      expect(result).toEqual(mockExperiment);
    });
  });

  describe('assignVariant', () => {
    it('should assign variant to user', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAssignment });

      const result = await ABTestingService.assignVariant(
        'test_experiment',
        'user-1'
      );

      expect(api.post).toHaveBeenCalledWith('/experiments/test_experiment/assign', {
        userId: 'user-1',
        deviceId: undefined,
        forceVariantKey: undefined,
      });
      expect(result).toEqual(mockAssignment);
    });

    it('should cache variant assignment', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAssignment });

      await ABTestingService.assignVariant('test_experiment', 'user-1');

      const cached = ABTestingService.getCachedVariant('test_experiment');
      expect(cached).toBe('control');
    });

    it('should support custom device ID and force variant', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAssignment });

      await ABTestingService.assignVariant('test_experiment', 'user-1', 'device-1', 'variant_b');

      expect(api.post).toHaveBeenCalledWith('/experiments/test_experiment/assign', {
        userId: 'user-1',
        deviceId: 'device-1',
        forceVariantKey: 'variant_b',
      });
    });
  });

  describe('trackConversion', () => {
    it('should track conversion event', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: {} });

      await ABTestingService.trackConversion(
        'exp-1',
        'user-1',
        'purchase_completed',
        'device-1',
        { amount: 100000 }
      );

      expect(api.post).toHaveBeenCalledWith('/experiments/exp-1/track', {
        experimentId: 'exp-1',
        userId: 'user-1',
        deviceId: 'device-1',
        eventType: 'purchase_completed',
        properties: { amount: 100000 },
      });
    });
  });

  describe('getCachedVariant', () => {
    it('should return cached variant if exists and not expired', () => {
      const cacheKey = 'ab_test_test_experiment';
      const expiresAt = new Date(Date.now() + 30 * 60 * 1000).toISOString();

      localStorage.setItem(
        cacheKey,
        JSON.stringify({
          variantKey: 'control',
          experimentKey: 'test_experiment',
          assignedAt: '2024-01-01T00:00:00Z',
          expiresAt,
        })
      );

      const result = ABTestingService.getCachedVariant('test_experiment');
      expect(result).toBe('control');
    });

    it('should return null if cache expired', () => {
      const cacheKey = 'ab_test_test_experiment';
      const expiresAt = new Date(Date.now() - 1000).toISOString();

      localStorage.setItem(
        cacheKey,
        JSON.stringify({
          variantKey: 'control',
          experimentKey: 'test_experiment',
          assignedAt: '2024-01-01T00:00:00Z',
          expiresAt,
        })
      );

      const result = ABTestingService.getCachedVariant('test_experiment');
      expect(result).toBeNull();
      expect(localStorage.getItem(cacheKey)).toBeNull();
    });

    it('should return null if no cache exists', () => {
      const result = ABTestingService.getCachedVariant('non_existent');
      expect(result).toBeNull();
    });
  });

  describe('clearCachedVariant', () => {
    it('should clear cached variant', () => {
      const cacheKey = 'ab_test_test_experiment';
      localStorage.setItem(cacheKey, JSON.stringify({ variantKey: 'control' }));

      ABTestingService.clearCachedVariant('test_experiment');

      expect(localStorage.getItem(cacheKey)).toBeNull();
    });
  });

  describe('clearAllCache', () => {
    it('should clear all experiment caches', () => {
      localStorage.setItem('ab_test_exp1', JSON.stringify({ variantKey: 'control' }));
      localStorage.setItem('ab_test_exp2', JSON.stringify({ variantKey: 'variant_b' }));
      localStorage.setItem('other_key', 'value');

      ABTestingService.clearAllCache();

      expect(localStorage.getItem('ab_test_exp1')).toBeNull();
      expect(localStorage.getItem('ab_test_exp2')).toBeNull();
      expect(localStorage.getItem('other_key')).toBe('value');
    });
  });
});
