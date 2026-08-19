'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import { PartnerService } from '@/services/PartnerService';
import type { Partner } from '@/services/PartnerService';

// ── Partner CRUD ──
export function usePartners() {
  return useQuery({
    queryKey: ['partners'],
    queryFn: () => PartnerService.listPartners(),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function usePartner(id: number) {
  return useQuery({
    queryKey: ['partner', id],
    queryFn: () => PartnerService.getProfile(id),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useMyPartner() {
  return useQuery({
    queryKey: ['partner', 'me'],
    queryFn: () => PartnerService.getMyPartner(),
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useRegisterPartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: { name: string; email: string; type: string; phone: string; publicKey?: string }) =>
      PartnerService.register(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['partners'] }); },
  });
}

export function useUpdatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Partner> }) =>
      PartnerService.updatePartner(id, data),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['partner', vars.id] });
      qc.invalidateQueries({ queryKey: ['partners'] });
    },
  });
}

export function useRegeneratePartnerKeys() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => PartnerService.regenerateKeys(id),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, id) => { qc.invalidateQueries({ queryKey: ['partner', id] }); },
  });
}

export function useDeletePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => PartnerService.deletePartner(id),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['partners'] }); },
  });
}

// ── Certificates ──
export function usePartnerCertificates(partnerId: number) {
  return useQuery({
    queryKey: ['partner-certificates', partnerId],
    queryFn: () => PartnerService.getCertificates(partnerId),
    enabled: !!partnerId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useExpiringCertificates(partnerId: number) {
  return useQuery({
    queryKey: ['partner-certificates', partnerId, 'expiring'],
    queryFn: () => PartnerService.getExpiringCertificates(partnerId),
    enabled: !!partnerId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useUploadCertificate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ partnerId, certData }: { partnerId: number; certData: string }) =>
      PartnerService.uploadCertificate(partnerId, certData),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['partner-certificates', vars.partnerId] }); },
  });
}

export function useGenerateCertificate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (partnerId: number) => PartnerService.generateCertificate(partnerId),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, partnerId) => { qc.invalidateQueries({ queryKey: ['partner-certificates', partnerId] }); },
  });
}

export function useRotateCertificate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ partnerId, certificateId }: { partnerId: number; certificateId: string }) =>
      PartnerService.rotateCertificate(partnerId, certificateId),
    ...MutationPresets.nonFinancial,
    onSuccess: (_, vars) => { qc.invalidateQueries({ queryKey: ['partner-certificates', vars.partnerId] }); },
  });
}

// ── SNAP-BI ──
// BUG-FE-033: useSnapBiAuthToken removed — SNAP-BI token exchange must happen server-side only.

export function useSnapBiPayment() {
  return useMutation({
    mutationFn: (data: { amount: number; currency: string; referenceId: string; description?: string }) =>
      PartnerService.createSnapBiPayment(data),
    ...MutationPresets.financial,
  });
}
