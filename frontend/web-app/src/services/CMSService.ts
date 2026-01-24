import api from '@/lib/api';

export interface Content {
  id: string;
  contentType: string;
  title: string;
  description: string;
  imageUrl: string;
  actionUrl: string;
  actionType: string;
  startDate: string;
  endDate: string;
  priority: number;
  status: string;
  targetingRules: Record<string, unknown>;
  metadata: Record<string, unknown>;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  active: boolean;
}

export interface ContentResponse {
  contents: Content[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type ContentType = 'BANNER' | 'PROMO' | 'ALERT' | 'POPUP';
export type ActionType = 'LINK' | 'DEEP_LINK' | 'DISMISS';

/**
 * CMSService handles content retrieval from the public CMS API.
 * No authentication required for public content access.
 */
export class CMSService {
  private static instance: CMSService;
  private baseURL = '/public/contents';

  private constructor() {}

  static getInstance(): CMSService {
    if (!CMSService.instance) {
      CMSService.instance = new CMSService();
    }
    return CMSService.instance;
  }

  /**
   * Fetch active content by type
   * @param type - Content type (BANNER, PROMO, ALERT, POPUP)
   * @param options - Optional targeting parameters
   */
  async getActiveContentByType(
    type: ContentType,
    options?: {
      segment?: string;
      location?: string;
      device?: string;
    }
  ): Promise<Content[]> {
    const response = await api.get<Content[]>(
      `${this.baseURL}/type/${type}`,
      {
        params: options,
        // Public endpoint doesn't require auth, but api interceptor adds it
        // We can override by using a separate axios instance for public calls if needed
        // For now, the token will be sent but ignored by the backend
      }
    );
    // Sort by priority (higher first)
    return response.data.sort((a, b) => b.priority - a.priority);
  }

  /**
   * Fetch banner content
   */
  async getBanners(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType('BANNER', options);
  }

  /**
   * Fetch promotional content
   */
  async getPromos(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType('PROMO', options);
  }

  /**
   * Fetch alert content (emergency alerts)
   */
  async getAlerts(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType('ALERT', options);
  }

  /**
   * Fetch popup content
   */
  async getPopups(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType('POPUP', options);
  }
}

export default CMSService.getInstance();
