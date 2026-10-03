'use client';

import { useQuery } from '@tanstack/react-query';
import CMSService, { type ContentType } from '@/services/CMSService';

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

export const useBanners = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('BANNER', options);
};

export const usePromos = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('PROMO', options);
};

export const useEmergencyAlerts = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('ALERT', options);
};

export const usePopups = (options?: {
  segment?: string;
  location?: string;
  device?: string;
  enabled?: boolean;
}) => {
  return useActiveContent('POPUP', options);
};
