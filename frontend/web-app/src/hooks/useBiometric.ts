'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import AuthService from '@/services/AuthService';
import type { BiometricRegistration, BiometricAuthRequest } from '@/services/AuthService';

export function useBiometricChallenge() {
  return useQuery({
    queryKey: ['biometric-challenge'],
    queryFn: () => AuthService.getBiometricChallenge(),
    enabled: false, // only fetch on demand
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['biometric-registrations'] }); },
  });
}

export function useAuthenticateBiometric() {
  return useMutation({
    mutationFn: (request: BiometricAuthRequest) => AuthService.authenticateBiometric(request),
  });
}

export function useRevokeBiometric() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (registrationId: string) => AuthService.revokeBiometricRegistration(registrationId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['biometric-registrations'] }); },
  });
}
