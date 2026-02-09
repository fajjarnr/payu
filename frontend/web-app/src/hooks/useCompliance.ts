'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import ComplianceService from '@/services/ComplianceService';
import type { CreateAuditReportRequest, CreateGdprAuditRequest, GdprSearchCriteria } from '@/services/ComplianceService';

// ── Audit Reports ──
export function useAuditReports() {
  return useQuery({
    queryKey: ['audit-reports'],
    queryFn: () => ComplianceService.listAuditReports(),
  });
}

export function useAuditReport(id: string) {
  return useQuery({
    queryKey: ['audit-report', id],
    queryFn: () => ComplianceService.getAuditReport(id),
    enabled: !!id,
  });
}

export function useCreateAuditReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAuditReportRequest) => ComplianceService.createAuditReport(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['audit-reports'] }); },
  });
}

// ── GDPR Audits ──
export function useUserGdprAudits(userId: string) {
  return useQuery({
    queryKey: ['gdpr-audits', userId],
    queryFn: () => ComplianceService.getUserGdprAudits(userId),
    enabled: !!userId,
  });
}

export function useUserGdprAuditCount(userId: string) {
  return useQuery({
    queryKey: ['gdpr-audit-count', userId],
    queryFn: () => ComplianceService.getUserGdprAuditCount(userId),
    enabled: !!userId,
  });
}

export function useFailedAccessAudits() {
  return useQuery({
    queryKey: ['gdpr-audits', 'failed-access'],
    queryFn: () => ComplianceService.getFailedAccess(),
  });
}

export function useCreateGdprAudit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateGdprAuditRequest) => ComplianceService.createGdprAudit(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['gdpr-audits'] }); },
  });
}

export function useSearchGdprAudits() {
  return useMutation({
    mutationFn: (criteria: GdprSearchCriteria) => ComplianceService.searchGdprAudits(criteria),
  });
}

export function useDeleteGdprAudit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (auditId: string) => ComplianceService.deleteGdprAudit(auditId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['gdpr-audits'] }); },
  });
}
