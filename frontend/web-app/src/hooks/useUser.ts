'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import UserService from '@/services/UserService';
import type { UpdateUserRequest } from '@/services/UserService';
import { useAuthStore } from '@/stores';

export const useUser = (userId: string | undefined) => {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: () => {
      if (!userId) throw new Error('User ID is required');
      return UserService.getUser(userId);
    },
    enabled: !!userId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useUpdateUser = () => {
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: UpdateUserRequest }) =>
      UserService.updateUser(userId, data),
    ...MutationPresets.nonFinancial,
    onSuccess: (response) => {
      // WEB-AUTH-001: Update user data while preserving active accountId
      if (response.user) {
        const currentAccountId = useAuthStore.getState().accountId || response.user.id;
        setAuth(response.user, currentAccountId);
      }
      // Invalidate user queries to refetch
      queryClient.invalidateQueries({ queryKey: ['user'] });
      queryClient.invalidateQueries({ queryKey: ['auth'] });
    },
    onError: (error) => {
      console.error('Update user failed:', error instanceof Error ? error.message : 'Unknown error');
    },
  });
};
