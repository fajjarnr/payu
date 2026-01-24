import { useQuery } from '@tanstack/react-query';
import CMSService, { type ContentType } from '@/services/CMSService';

/**
 * Fetch active content by type
 * @param type - Content type (BANNER, PROMO, ALERT, POPUP)
 * @param options - Optional targeting parameters
 */
export const useActiveContent = (
  type: ContentType,
  options?: {
    segment?: string;
    location?: string;
    device?: string;
    enabled?: boolean;
  }
) => {
  return useQuery({
    queryKey: ['cms-content', type, options?.segment, options?.location, options?.device],
    queryFn: () => CMSService.getActiveContentByType(type, options),
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    refetchOnWindowFocus: false,
  });
};

/**
 * Fetch banner content for carousel
 */
export const useBanners = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('BANNER', options);
};

/**
 * Fetch promotional content
 */
export const usePromos = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('PROMO', options);
};

/**
 * Fetch emergency alerts
 */
export const useEmergencyAlerts = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('ALERT', options);
};

/**
 * Fetch popup content (for modals)
 */
export const usePopups = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('POPUP', options);
};
