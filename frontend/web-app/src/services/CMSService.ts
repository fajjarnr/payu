import api from "@/lib/api";

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

export type ContentType = "BANNER" | "PROMO" | "ALERT" | "POPUP";
export type ActionType = "LINK" | "DEEP_LINK" | "DISMISS";

/** CMSService handles content retrieval from the public CMS API. No authentication required. */
export class CMSService {
  private static instance: CMSService;
  private baseURL = "/public/contents";

  private constructor() {}

  static getInstance(): CMSService {
    if (!CMSService.instance) {
      CMSService.instance = new CMSService();
    }
    return CMSService.instance;
  }

  /** Fetch active content by type */
  async getActiveContentByType(
    type: ContentType,
    options?: {
      segment?: string;
      location?: string;
      device?: string;
    },
  ): Promise<Content[]> {
    const response = await api.get<Content[]>(`${this.baseURL}/type/${type}`, {
      params: options,
      // Public endpoint doesn't require auth, but api interceptor adds it
      // We can override by using a separate axios instance for public calls if needed
      // For now, the token will be sent but ignored by the backend
    });
    // Sort by priority (higher first)
    return response.data.sort((a, b) => b.priority - a.priority);
  }

  async getBanners(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType("BANNER", options);
  }

  async getPromos(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType("PROMO", options);
  }

  async getAlerts(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType("ALERT", options);
  }

  async getPopups(options?: {
    segment?: string;
    location?: string;
    device?: string;
  }): Promise<Content[]> {
    return this.getActiveContentByType("POPUP", options);
  }

  /** POST /contents — Create new content */
  async createContent(data: {
    title: string;
    contentType: ContentType;
    description: string;
    imageUrl?: string;
    actionUrl?: string;
    actionType?: ActionType;
    startDate?: string;
    endDate?: string;
    priority?: number;
  }): Promise<Content> {
    const response = await api.post<Content>("/contents", data);
    return response.data;
  }

  /** PUT /contents/{id} — Update content */
  async updateContent(
    id: string,
    data: Partial<
      Omit<
        Content,
        "id" | "createdAt" | "updatedAt" | "createdBy" | "updatedBy"
      >
    >,
  ): Promise<Content> {
    const response = await api.put<Content>(`/contents/${id}`, data);
    return response.data;
  }

  /** PATCH /contents/{id}/status — Update content status */
  async updateContentStatus(
    id: string,
    status: "ACTIVE" | "PAUSED" | "ARCHIVED",
  ): Promise<Content> {
    const response = await api.patch<Content>(`/contents/${id}/status`, {
      status,
    });
    return response.data;
  }

  /** DELETE /contents/{id} — Delete content */
  async deleteContent(id: string): Promise<void> {
    await api.delete(`/contents/${id}`);
  }
}

export default CMSService.getInstance();
