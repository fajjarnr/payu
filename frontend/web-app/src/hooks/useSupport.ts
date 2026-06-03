'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MutationPresets } from '@/lib/mutation-config';
import SupportService from '@/services/SupportService';
import type { CreateAgentRequest, CreateModuleRequest, AssignTrainingRequest, AgentStatus, TrainingStatus, CreateTicketRequest, TicketCategory, TicketPriority } from '@/services/SupportService';

// ── Training Dashboard ──
export function useTrainingStatus() {
  return useQuery({
    queryKey: ['support-training-status'],
    queryFn: () => SupportService.getTrainingStatus(),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useSupportAgents() {
  return useQuery({
    queryKey: ['support-agents'],
    queryFn: () => SupportService.listAgents(),
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

export function useSupportAgent(id: string) {
  return useQuery({
    queryKey: ['support-agent', id],
    queryFn: () => SupportService.getAgent(id),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

// ── Agents ──
export function useSupportAgents() {
  return useQuery({
    queryKey: ['support-agents'],
    queryFn: () => SupportService.listAgents(),
  });
}

export function useSupportAgent(id: string) {
  return useQuery({
    queryKey: ['support-agent', id],
    queryFn: () => SupportService.getAgent(id),
    enabled: !!id,
  });
}

export function useCreateAgent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAgentRequest) => SupportService.createAgent(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['support-agents'] }); },
  });
}

export function useUpdateAgentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      SupportService.updateAgentStatus(id, active),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['support-agents'] }); },
  });
}

// ── Training Modules ──
export function useTrainingModules() {
  return useQuery({
    queryKey: ['training-modules'],
    queryFn: () => SupportService.listModules(),
    staleTime: 10 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
  });
}

export function useMandatoryModules() {
  return useQuery({
    queryKey: ['training-modules', 'mandatory'],
    queryFn: () => SupportService.getMandatoryModules(),
    staleTime: 10 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
  });
}

export function useCreateModule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModuleRequest) => SupportService.createModule(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['training-modules'] }); },
  });
}

export function useUpdateModuleStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TrainingStatus }) =>
      SupportService.updateModuleStatus(id, status),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['training-modules'] }); },
  });
}

// ── Training Assignments ──
export function useAgentTrainings(agentId: number) {
  return useQuery({
    queryKey: ['agent-trainings', agentId],
    queryFn: () => SupportService.getAgentTrainings(agentId),
    enabled: !!agentId,
  });
}

export function useAgentTrainingStatus(agentId: number) {
  return useQuery({
    queryKey: ['agent-training-status', agentId],
    queryFn: () => SupportService.getAgentTrainingStatus(agentId),
    enabled: !!agentId,
  });
}

export function useAssignTraining() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: AssignTrainingRequest) => SupportService.assignTraining(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['agent-trainings'] });
      qc.invalidateQueries({ queryKey: ['support-training-status'] });
    },
  });
}

// ── Tickets (frontend-only for now) ──
export function useTickets() {
  return useQuery({
    queryKey: ['support-tickets'],
    queryFn: () => SupportService.getTickets(),
  });
}

export function useCreateTicket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTicketRequest) =>
      SupportService.createTicket(data),
    ...MutationPresets.nonFinancial,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['support-tickets'] }); },
  });
}
