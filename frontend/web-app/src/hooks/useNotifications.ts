'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import NotificationService from '@/services/NotificationService';

export function useNotifications(userId: string, size = 20) {
  return useQuery({
    queryKey: ['notifications', userId, size],
    queryFn: () => NotificationService.getUserNotifications(userId, size),
    enabled: !!userId,
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
  });
}

export function useNotification(id: string) {
  return useQuery({
    queryKey: ['notification', id],
    queryFn: () => NotificationService.getNotification(id),
    enabled: !!id,
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
  });
}

export function useMarkNotificationRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: string) => NotificationService.markAsRead(notificationId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }); },
  });
}
