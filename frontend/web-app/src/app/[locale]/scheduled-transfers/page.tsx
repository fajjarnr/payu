'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

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
} from '@/components/icons';
import { useLocale } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Alert, Button, Input, Modal, Select, Tag, Typography } from 'antd';
import {
  useScheduledTransfers,
  useUpdateScheduledTransfer,
  useCancelScheduledTransfer,
  usePauseScheduledTransfer,
  useResumeScheduledTransfer,
} from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import type { ScheduledTransfer } from '@/services/TransactionService';
import { parseCurrencyExact } from '@/lib/currency';

export default function ScheduledTransfersPage() {
  const { accountId: storeAccountId } = useAuthStore();
  const accountId = storeAccountId ?? '';
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';

  const { data: rawTransfers, isLoading } = useScheduledTransfers(accountId);
  const transfers = Array.isArray(rawTransfers) ? rawTransfers : [];
  const updateTransfer = useUpdateScheduledTransfer();
  const cancelTransfer = useCancelScheduledTransfer();
  const pauseTransfer = usePauseScheduledTransfer();
  const resumeTransfer = useResumeScheduledTransfer();

  const [selectedTransfer, setSelectedTransfer] = useState<ScheduledTransfer | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

  const [editForm, setEditForm] = useState<{
    amount: string;
    description: string;
    scheduleType: 'ONE_TIME' | 'RECURRING_DAILY' | 'RECURRING_WEEKLY' | 'RECURRING_MONTHLY' | 'RECURRING_CUSTOM';
  }>({
    amount: '0.0000',
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
        amount: parseCurrencyExact(editForm.amount),
        description: editForm.description,
        scheduleType: editForm.scheduleType,
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

  const STATUS_TAG: Record<string, { color: 'green' | 'orange' | 'red' | 'default'; className: string }> = {
    ACTIVE: { color: 'green', className: 'bg-primary/10 text-primary border-primary/20' },
    PAUSED: { color: 'orange', className: 'bg-warning/10 text-warning border-warning/20' },
    CANCELLED: { color: 'red', className: 'bg-error/10 text-error border-error/20' },
    COMPLETED: { color: 'default', className: 'bg-text-secondary/10 text-text-secondary border-text-secondary/20' },
  };

  const getStatusBadge = (status: string) => {
    const config = STATUS_TAG[status] || STATUS_TAG.ACTIVE;
    return (
      <Tag bordered={false} color={config.color} className={config.className}>
        {status}
      </Tag>
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
      <div className="space-y-6 lg:space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
          <div>
            <h2 className="text-3xl font-bold text-foreground tracking-tight">Transfer Terjadwal</h2>
            <p className="text-sm text-muted-foreground font-medium mt-1">
              Kelola dan pantau transfer berulang Anda.
            </p>
          </div>
          <Link href="/transfer" className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-primary px-6 py-2 h-12 text-xs font-bold uppercase tracking-[0.15em] text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:bg-primary/90 active:scale-95">
            <Plus className="h-4 w-4 mr-2" /> Transfer Baru
          </Link>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          {[
            {
              label: 'Total Transfer',
              value: transfers?.length || 0,
              icon: ArrowRightLeft,
              color: 'bg-primary/10 text-primary',
            },
            {
              label: 'Aktif',
              value: transfers?.filter((t) => t.status === 'ACTIVE').length || 0,
              icon: CheckCircle2,
              color: 'bg-primary/10 text-primary',
            },
            {
              label: 'Dijeda',
              value: transfers?.filter((t) => t.status === 'PAUSED').length || 0,
              icon: Pause,
              color: 'bg-warning/10 text-warning',
            },
            {
              label: 'Selesai',
              value: transfers?.filter((t) => t.status === 'COMPLETED').length || 0,
              icon: Calendar,
              color: 'bg-text-secondary/10 text-text-secondary',
            },
          ].map((stat, i) => (
            <div key={i} className="bg-card rounded-xl p-6 border border-border shadow-sm">
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
          ))}
        </div>

        {/* Transfers List */}
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
              <Link href="/transfer" className="mt-4 inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-primary px-6 py-2 h-12 text-xs font-bold uppercase tracking-[0.15em] text-primary-foreground shadow-lg transition-all hover:bg-primary/90 active:scale-95">Buat Transfer Terjadwal</Link>
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
                            <ArrowRightLeft className="h-3 w-3" />
                            Rp {Number(transfer.amount).toLocaleString(bcp47Locale)}
                          </span>
                          <span className="flex items-center gap-1">
                            <Calendar className="h-3 w-3" />
                            {getScheduleTypeLabel(transfer.scheduleType)}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="h-3 w-3" />
                            {new Date(transfer.nextExecutionDate).toLocaleDateString(bcp47Locale)}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {transfer.status === 'ACTIVE' && (
                        <>
                          <Button
                            size="small"
                            onClick={() => handlePause(transfer)}
                            disabled={pauseTransfer.isPending}
                            aria-label="Jeda transfer terjadwal"
                          >
                            {pauseTransfer.isPending ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <Pause className="h-4 w-4" />
                            )}
                          </Button>
                          <Button
                            size="small"
                            onClick={() => handleOpenEditModal(transfer)}
                            disabled={updateTransfer.isPending}
                            aria-label="Edit transfer terjadwal"
                          >
                            <Edit3 className="h-4 w-4" />
                          </Button>
                        </>
                      )}

                      {transfer.status === 'PAUSED' && (
                        <Button
                          size="small"
                          onClick={() => handleResume(transfer)}
                          disabled={resumeTransfer.isPending}
                          aria-label="Lanjutkan transfer terjadwal"
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
                          danger
                          size="small"
                          className="text-error hover:bg-error/10"
                          onClick={() => handleOpenCancelModal(transfer)}
                          disabled={cancelTransfer.isPending}
                          aria-label="Hapus transfer terjadwal"
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
      </div>

      {/* Edit Modal */}
      <Modal open={isEditModalOpen} onCancel={() => setIsEditModalOpen(false)} footer={null} centered width={512} title={<Typography.Title level={4} className="!mb-1">Edit Transfer Terjadwal</Typography.Title>}>
        <div>
          <Typography.Text type="secondary">
            Ubah detail transfer terjadwal Anda.
          </Typography.Text>

          {updateTransfer.isError && (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle className="h-4 w-4 text-error" />}
              className="bg-error/10 border-error/20 p-4"
              description={<span className="text-error">Gagal memperbarui transfer. Silakan coba lagi.</span>}
            />
          )}

          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <label htmlFor="scheduled-amount" className="text-sm font-medium">Jumlah (IDR)</label>
              <Input id="scheduled-amount"
                type="number"
                step="any"
                value={editForm.amount}
                onChange={(e) => setEditForm((prev) => ({ ...prev, amount: e.target.value }))}
                placeholder="100000"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="scheduled-description" className="text-sm font-medium">Deskripsi</label>
              <Input id="scheduled-description"
                type="text"
                value={editForm.description}
                onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder="Deskripsi transfer"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="scheduled-type" className="text-sm font-medium">Tipe Jadwal</label>
              <Select id="scheduled-type"
                className="w-full"
                value={editForm.scheduleType}
                onChange={(value: string) => setEditForm((prev) => ({ ...prev, scheduleType: value as typeof prev.scheduleType }))}
                options={[
                  { value: 'ONE_TIME', label: 'Sekali' },
                  { value: 'RECURRING_DAILY', label: 'Harian' },
                  { value: 'RECURRING_WEEKLY', label: 'Mingguan' },
                  { value: 'RECURRING_MONTHLY', label: 'Bulanan' },
                ]}
              />
            </div>
          </div>

          <div className="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2">
            <Button onClick={() => setIsEditModalOpen(false)}>
              Batal
            </Button>
            <Button type="primary" onClick={handleUpdate} disabled={updateTransfer.isPending}>
              {updateTransfer.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : null}
              Simpan Perubahan
            </Button>
          </div>
        </div>
      </Modal>

      {/* Cancel Confirmation Modal */}
      <Modal open={isCancelModalOpen} onCancel={() => setIsCancelModalOpen(false)} footer={null} centered width={512} title={<Typography.Title level={4} className="!mb-1 text-destructive">Batalkan Transfer</Typography.Title>}>
        <div>
          <Typography.Text type="secondary">
            Apakah Anda yakin ingin membatalkan transfer terjadwal ini? Tindakan ini tidak dapat dibatalkan.
          </Typography.Text>

          {cancelTransfer.isError && (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle className="h-4 w-4 text-error" />}
              className="bg-error/10 border-error/20 p-4"
              description={<span className="text-error">Gagal membatalkan transfer. Silakan coba lagi.</span>}
            />
          )}

          <div className="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2 mt-4">
            <Button onClick={() => setIsCancelModalOpen(false)}>
              Batal
            </Button>
            <Button
              danger
              type="primary"
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
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
