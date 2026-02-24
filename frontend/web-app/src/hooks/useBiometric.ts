'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import AuthService from '@/services/AuthService';
import type { BiometricRegistration, BiometricAuthRequest } from '@/services/AuthService';

// BUG-FE-035: Changed from useQuery (enabled: false) to useMutation
export function useBiometricChallenge() {
  return useMutation({
    mutationFn: () => AuthService.getBiometricChallenge(),
    ...MutationPresets.nonFinancial,
  });
}

export function useBiometricRegistrations(username: string) {
  return useQuery({
    queryKey: ['biometric-registrations', username],
    queryFn: () => AuthService.getBiometricRegistrations(username),
    enabled: !!username,
  });
}

export function useRegisterBiometric() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (registration: BiometricRegistration) => AuthService.registerBiometric(registration),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['biometric-registrations'] }); },
  });
}

export function useAuthenticateBiometric() {
  return useMutation({
    mutationFn: (request: BiometricAuthRequest) => AuthService.authenticateBiometric(request),
    ...MutationPresets.nonFinancial,
  });
}

export function useRevokeBiometric() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (registrationId: string) => AuthService.revokeBiometricRegistration(registrationId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['biometric-registrations'] }); },
  });
}
