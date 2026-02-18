'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import {
  Calendar,
  Clock,
  ArrowRightLeft,
  Pause,
  Play,
  Trash2,
  Edit3,
  Plus,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Repeat
} from 'lucide-react';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
  useScheduledTransfers,
  useUpdateScheduledTransfer,
  useCancelScheduledTransfer,
  usePauseScheduledTransfer,
  useResumeScheduledTransfer,
} from '@/hooks';
import { useAuth } from '@/hooks';
import type { ScheduledTransfer } from '@/services/TransactionService';

export default function ScheduledTransfersPage() {
  const { user } = useAuth();
  const accountId = user?.id || 'default';

  const { data: transfers, isLoading } = useScheduledTransfers(accountId);
  const updateTransfer = useUpdateScheduledTransfer();
  const cancelTransfer = useCancelScheduledTransfer();
  const pauseTransfer = usePauseScheduledTransfer();
  const resumeTransfer = useResumeScheduledTransfer();

  const [selectedTransfer, setSelectedTransfer] = useState<ScheduledTransfer | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

  const [editForm, setEditForm] = useState({
    amount: 0,
    description: '',
    scheduleType: 'ONE_TIME',
  });

  const handleOpenEditModal = (transfer: ScheduledTransfer) => {
    setSelectedTransfer(transfer);
    setEditForm({
      amount: transfer.amount,
      description: transfer.description || '',
      scheduleType: transfer.scheduleType,
    });
    setIsEditModalOpen(true);
  };

  const handleOpenCancelModal = (transfer: ScheduledTransfer) => {
    setSelectedTransfer(transfer);
    setIsCancelModalOpen(true);
  };

  const handleUpdate = async () => {
    if (!selectedTransfer) return;

    await updateTransfer.mutateAsync({
      id: selectedTransfer.id,
      data: {
        amount: editForm.amount,
        description: editForm.description,
        scheduleType: editForm.scheduleType as any,
      },
    });

    setIsEditModalOpen(false);
  };

  const handleCancel = async () => {
    if (!selectedTransfer) return;

    await cancelTransfer.mutateAsync(selectedTransfer.id);
    setIsCancelModalOpen(false);
  };

  const handlePause = async (transfer: ScheduledTransfer) => {
    await pauseTransfer.mutateAsync(transfer.id);
  };

  const handleResume = async (transfer: ScheduledTransfer) => {
    await resumeTransfer.mutateAsync(transfer.id);
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; className: string }> = {
      ACTIVE: { variant: 'default', className: 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' },
      PAUSED: { variant: 'secondary', className: 'bg-amber-500/10 text-amber-500 border-amber-500/20' },
      CANCELLED: { variant: 'destructive', className: 'bg-red-500/10 text-red-500 border-red-500/20' },
      COMPLETED: { variant: 'outline', className: 'bg-gray-500/10 text-gray-500 border-gray-500/20' },
    };

    const config = variants[status] || variants.ACTIVE;
    return (
      <Badge variant={config.variant} className={config.className}>
        {status}
      </Badge>
    );
  };

  const getScheduleTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      ONE_TIME: 'Sekali',
      RECURRING_DAILY: 'Harian',
      RECURRING_WEEKLY: 'Mingguan',
      RECURRING_MONTHLY: 'Bulanan',
      RECURRING_CUSTOM: 'Kustom',
    };
    return labels[type] || type;
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-8">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-bold text-foreground">Transfer Terjadwal</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">
                    Kelola dan pantau transfer berulang Anda.
                  </p>
                </div>
                <ButtonMotion>
                  <Button className="shadow-xl shadow-primary/20" asChild>
                    <a href="/transfer">
                      <Plus className="h-4 w-4 mr-2" /> Transfer Baru
                    </a>
                  </Button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              {[
                {
                  label: 'Total Transfer',
                  value: transfers?.length || 0,
                  icon: ArrowRightLeft,
                  color: 'bg-blue-500/10 text-blue-500',
                },
                {
                  label: 'Aktif',
                  value: transfers?.filter((t) => t.status === 'ACTIVE').length || 0,
                  icon: CheckCircle2,
                  color: 'bg-emerald-500/10 text-emerald-500',
                },
                {
                  label: 'Dijeda',
                  value: transfers?.filter((t) => t.status === 'PAUSED').length || 0,
                  icon: Pause,
                  color: 'bg-amber-500/10 text-amber-500',
                },
                {
                  label: 'Selesai',
                  value: transfers?.filter((t) => t.status === 'COMPLETED').length || 0,
                  icon: Calendar,
                  color: 'bg-gray-500/10 text-gray-500',
                },
              ].map((stat, i) => (
                <StaggerItem key={i}>
                  <div className="bg-card rounded-xl p-6 border border-border shadow-sm">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{stat.label}</p>
                        <p className="text-2xl font-bold mt-1">{stat.value}</p>
                      </div>
                      <div className={`h-12 w-12 rounded-xl flex items-center justify-center ${stat.color}`}>
                        <stat.icon className="h-6 w-6" />
                      </div>
                    </div>
                  </div>
                </StaggerItem>
              ))}
            </div>

            {/* Transfers List */}
            <StaggerItem>
              <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                <div className="p-6 border-b border-border">
                  <h3 className="text-lg font-bold">Daftar Transfer Terjadwal</h3>
                </div>

                {isLoading ? (
                  <div className="p-12 flex justify-center">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                  </div>
                ) : !transfers || transfers.length === 0 ? (
                  <div className="p-12 text-center">
                    <Calendar className="h-12 w-12 mx-auto text-muted-foreground/50 mb-4" />
                    <p className="text-muted-foreground">Belum ada transfer terjadwal</p>
                    <Button className="mt-4" asChild>
                      <a href="/transfer">Buat Transfer Terjadwal</a>
                    </Button>
                  </div>
                ) : (
                  <div className="divide-y divide-border">
                    {transfers.map((transfer) => (
                      <div
                        key={transfer.id}
                        className="p-6 hover:bg-muted/50 transition-colors"
                      >
                        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                          <div className="flex items-start gap-4">
                            <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center">
                              <Repeat className="h-6 w-6 text-primary" />
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <p className="font-bold">{transfer.description || 'Transfer Terjadwal'}</p>
                                {getStatusBadge(transfer.status)}
                              </div>
                              <div className="flex items-center gap-4 mt-1 text-sm text-muted-foreground">
                                <span className="flex items-center gap-1">
                                  <ArrowRightLeft className="h-3 w-3" /
                                  Rp {transfer.amount.toLocaleString('id-ID')}
                                </span>
                                <span className="flex items-center gap-1">
                                  <Calendar className="h-3 w-3" /
                                  {getScheduleTypeLabel(transfer.scheduleType)}
                                </span>
                                <span className="flex items-center gap-1">
                                  <Clock className="h-3 w-3" /
                                  {new Date(transfer.nextExecutionDate).toLocaleDateString('id-ID')}
                                </span>
                              </div>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            {transfer.status === 'ACTIVE' && (
                              <>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handlePause(transfer)}
                                  disabled={pauseTransfer.isPending}
                                >
                                  {pauseTransfer.isPending ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <Pause className="h-4 w-4" />
                                  )}
                                </Button>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleOpenEditModal(transfer)}
                                  disabled={updateTransfer.isPending}
                                >
                                  <Edit3 className="h-4 w-4" />
                                </Button>
                              </>
                            )}

                            {transfer.status === 'PAUSED' && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleResume(transfer)}
                                disabled={resumeTransfer.isPending}
                              >
                                {resumeTransfer.isPending ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Play className="h-4 w-4" />
                                )}
                              </Button>
                            )}

                            {(transfer.status === 'ACTIVE' || transfer.status === 'PAUSED') && (
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-red-500 hover:bg-red-500/10"
                                onClick={() => handleOpenCancelModal(transfer)}
                                disabled={cancelTransfer.isPending}
                              >
                                {cancelTransfer.isPending ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Trash2 className="h-4 w-4" />
                                )}
                              </Button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>

      {/* Edit Modal */}
      <Dialog open={isEditModalOpen} onOpenChange={setIsEditModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Edit Transfer Terjadwal</DialogTitle>
            <DialogDescription>
              Ubah detail transfer terjadwal Anda.
            </DialogDescription>
          </DialogHeader>

          {updateTransfer.isError && (
            <Alert className="bg-red-500/10 border-red-500/20">
              <AlertCircle className="h-4 w-4 text-red-500" />
              <AlertDescription className="text-red-500">
                Gagal memperbarui transfer. Silakan coba lagi.
              </AlertDescription>
            </Alert>
          )}

          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Jumlah (IDR)</label>
              <Input
                type="number"
                value={editForm.amount}
                onChange={(e) => setEditForm((prev) => ({ ...prev, amount: parseInt(e.target.value) || 0 }))}
                placeholder="100000"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">Deskripsi</label>
              <Input
                type="text"
                value={editForm.description}
                onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder="Deskripsi transfer"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">Frekuensi</label>
              <Select
                value={editForm.scheduleType}
                onValueChange={(value) => setEditForm((prev) => ({ ...prev, scheduleType: value }))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ONE_TIME">Sekali</SelectItem>
                  <SelectItem value="RECURRING_DAILY">Harian</SelectItem>
                  <SelectItem value="RECURRING_WEEKLY">Mingguan</SelectItem>
                  <SelectItem value="RECURRING_MONTHLY">Bulanan</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditModalOpen(false)}>
              Batal
            </Button>
            <Button onClick={handleUpdate} disabled={updateTransfer.isPending}>
              {updateTransfer.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : null}
              Simpan Perubahan
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel Confirmation Modal */}
      <Dialog open={isCancelModalOpen} onOpenChange={setIsCancelModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle className="text-destructive">Batalkan Transfer</DialogTitle>
            <DialogDescription>
              Apakah Anda yakin ingin membatalkan transfer terjadwal ini? Tindakan ini tidak dapat dibatalkan.
            </DialogDescription>
          </DialogHeader>

          {cancelTransfer.isError && (
            <Alert className="bg-red-500/10 border-red-500/20">
              <AlertCircle className="h-4 w-4 text-red-500" />
              <AlertDescription className="text-red-500">
                Gagal membatalkan transfer. Silakan coba lagi.
              </AlertDescription>
            </Alert>
          )}

          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setIsCancelModalOpen(false)}>
              Batal
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancel}
              disabled={cancelTransfer.isPending}
            >
              {cancelTransfer.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Trash2 className="h-4 w-4 mr-2" />
              )}
              Batalkan Transfer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}
