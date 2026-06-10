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
      // Update the auth store with new user data
      if (response.user) {
        setAuth(response.user, response.user.id);
      }
      // Invalidate user queries to refetch
      queryClient.invalidateQueries({ queryKey: ['user'] });
      queryClient.invalidateQueries({ queryKey: ['auth'] });
    },
    onError: (error) => {
      console.error('Update user failed:', error);
    },
  });
};
