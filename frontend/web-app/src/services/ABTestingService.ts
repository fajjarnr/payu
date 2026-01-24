import api from '@/lib/api';

// Experiment Types
export enum ExperimentStatus {
  DRAFT = 'DRAFT',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
}

export enum AllocationStrategy {
  MODULO = 'MODULO',
  MURMURHASH3 = 'MURMURHASH3',
}

export interface Variant {
  id: string;
  experimentId: string;
  key: string;
  name: string;
  description: string;
  isControl: boolean;
  allocationWeight: number;
  config: Record<string, unknown>;
  createdAt: string;
}

export interface Experiment {
  id: string;
  key: string;
  name: string;
  description: string;
  status: ExperimentStatus;
  allocationStrategy: AllocationStrategy;
  trafficPercentage: number;
  targetAudience: Record<string, unknown>;
  variants: Variant[];
  startDate: string;
  endDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VariantAssignment {
  id: string;
  experimentId: string;
  experimentKey: string;
  variantId: string;
  variantKey: string;
  userId: string;
  deviceId?: string;
  assignedAt: string;
  variant: Variant;
}

export interface ConversionEvent {
  eventType: string;
  userId: string;
  deviceId?: string;
  timestamp: string;
  properties?: Record<string, unknown>;
}

export interface TrackConversionRequest {
  experimentId: string;
  userId: string;
  deviceId?: string;
  eventType: string;
  properties?: Record<string, unknown>;
}

export interface AssignVariantRequest {
  userId: string;
  deviceId?: string;
  forceVariantKey?: string;
}

export interface CachedVariantAssignment {
  variantKey: string;
  experimentKey: string;
  assignedAt: string;
  expiresAt: string;
}

export class ABTestingService {
  private static instance: ABTestingService;
  private readonly CACHE_PREFIX = 'ab_test_';
  private readonly CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

  private constructor() {}

  static getInstance(): ABTestingService {
    if (!ABTestingService.instance) {
      ABTestingService.instance = new ABTestingService();
    }
    return ABTestingService.instance;
  }

  /**
   * Fetch experiment by key
   */
  async getExperimentByKey(key: string): Promise<Experiment> {
    const response = await api.get<Experiment>(`/experiments/key/${key}`);
    return response.data;
  }

  /**
   * Get or create variant assignment for a user
   */
  async assignVariant(
    experimentKey: string,
    userId: string,
    deviceId?: string,
    forceVariantKey?: string
  ): Promise<VariantAssignment> {
    const payload: AssignVariantRequest = {
      userId,
      deviceId,
      forceVariantKey,
    };

    const response = await api.post<VariantAssignment>(
      `/experiments/${experimentKey}/assign`,
      payload
    );

    // Cache the assignment
    this.cacheVariantAssignment(experimentKey, response.data);

    return response.data;
  }

  /**
   * Track conversion event for an experiment
   */
  async trackConversion(
    experimentId: string,
    userId: string,
    eventType: string,
    deviceId?: string,
    properties?: Record<string, unknown>
  ): Promise<void> {
    const payload: TrackConversionRequest = {
      experimentId,
      userId,
      deviceId,
      eventType,
      properties,
    };

    await api.post(`/experiments/${experimentId}/track`, payload);
  }

  /**
   * Get cached variant assignment if exists and not expired
   */
  getCachedVariant(experimentKey: string): string | null {
    if (typeof window === 'undefined') {
      return null;
    }

    try {
      const cacheKey = this.CACHE_PREFIX + experimentKey;
      const cached = localStorage.getItem(cacheKey);

      if (!cached) {
        return null;
      }

      const assignment: CachedVariantAssignment = JSON.parse(cached);

      // Check if expired
      if (new Date(assignment.expiresAt) < new Date()) {
        localStorage.removeItem(cacheKey);
        return null;
      }

      return assignment.variantKey;
    } catch {
      return null;
    }
  }

  /**
   * Cache variant assignment with TTL
   */
  private cacheVariantAssignment(
    experimentKey: string,
    assignment: VariantAssignment
  ): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const cacheKey = this.CACHE_PREFIX + experimentKey;
      const expiresAt = new Date(
        Date.now() + this.CACHE_TTL_MS
      ).toISOString();

      const cached: CachedVariantAssignment = {
        variantKey: assignment.variantKey,
        experimentKey: assignment.experimentKey,
        assignedAt: assignment.assignedAt,
        expiresAt,
      };

      localStorage.setItem(cacheKey, JSON.stringify(cached));
    } catch (error) {
      console.error('Failed to cache variant assignment:', error);
    }
  }

  /**
   * Clear cached variant assignment
   */
  clearCachedVariant(experimentKey: string): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const cacheKey = this.CACHE_PREFIX + experimentKey;
      localStorage.removeItem(cacheKey);
    } catch (error) {
      console.error('Failed to clear cached variant:', error);
    }
  }

  /**
   * Clear all experiment caches
   */
  clearAllCache(): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const keys = Object.keys(localStorage);
      keys.forEach((key) => {
        if (key.startsWith(this.CACHE_PREFIX)) {
          localStorage.removeItem(key);
        }
      });
    } catch (error) {
      console.error('Failed to clear experiment cache:', error);
    }
  }
}

export default ABTestingService.getInstance();
