"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import { MutationPresets } from "@/lib/mutation-config";
import ComplianceService from "@/services/ComplianceService";
import type {
  CreateAuditReportRequest,
  CreateGdprAuditRequest,
  GdprSearchCriteria,
  ComplianceStandard,
} from "@/services/ComplianceService";

// Audit Reports
export function useAuditReports(params?: {
  transactionId?: string;
  merchantId?: string;
  standard?: ComplianceStandard;
}) {
  // BACKOFFICE-RBAC-001: the controller rejects a parameterless search with
  // 400 ("At least one search parameter is required") — that is a contract,
  // not an outage, so the query must stay parked until a real filter exists.
  const hasCriteria = Boolean(
    params &&
    Object.values(params).some(
      (value) => typeof value === "string" && value.trim().length > 0,
    ),
  );

  return useQuery({
    queryKey: ["audit-reports", params],
    queryFn: () => ComplianceService.searchAuditReports(params ?? {}),
    enabled: hasCriteria,
  });
}

export function useAuditReport(id: string) {
  return useQuery({
    queryKey: ["audit-report", id],
    queryFn: () => ComplianceService.getAuditReport(id),
    enabled: !!id,
  });
}

export function useCreateAuditReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAuditReportRequest) =>
      ComplianceService.createAuditReport(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["audit-reports"] });
    },
  });
}

// GDPR Audits
export function useUserGdprAudits(userId: string) {
  return useQuery({
    queryKey: ["gdpr-audits", userId],
    queryFn: () => ComplianceService.getUserGdprAudits(userId),
    enabled: !!userId,
  });
}

export function useUserGdprAuditCount(userId: string) {
  return useQuery({
    queryKey: ["gdpr-audit-count", userId],
    queryFn: () => ComplianceService.getUserGdprAuditCount(userId),
    enabled: !!userId,
  });
}

export function useFailedAccessAudits(sinceDays = 7) {
  const since = useMemo(
    () => new Date(Date.now() - sinceDays * 86_400_000),
    [sinceDays],
  );
  return useQuery({
    queryKey: ["gdpr-audits", "failed-access", sinceDays],
    queryFn: () => ComplianceService.getFailedAccess(since),
  });
}

export function useCreateGdprAudit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateGdprAuditRequest) =>
      ComplianceService.createGdprAudit(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["gdpr-audits"] });
    },
  });
}

export function useSearchGdprAudits() {
  return useMutation({
    mutationFn: (criteria: GdprSearchCriteria) =>
      ComplianceService.searchGdprAudits(criteria),
    ...MutationPresets.readOnly,
  });
}

export function useDeleteGdprAudit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (auditId: string) => ComplianceService.deleteGdprAudit(auditId),
    ...MutationPresets.nonFinancial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["gdpr-audits"] });
    },
  });
}
