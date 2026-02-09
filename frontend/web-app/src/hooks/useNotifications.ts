'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import NotificationService from '@/services/NotificationService';

export function useNotifications(userId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['notifications', userId, page, size],
    queryFn: () => NotificationService.getUserNotifications(userId, page, size),
    enabled: !!userId,
  });
}

export function useNotification(id: string) {
  return useQuery({
    queryKey: ['notification', id],
    queryFn: () => NotificationService.getNotification(id),
    enabled: !!id,
  });
}

export function useMarkNotificationRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: string) => NotificationService.markAsRead(notificationId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }); },
  });
}
