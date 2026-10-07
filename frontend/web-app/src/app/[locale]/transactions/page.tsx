'use client';

import React, { useState } from 'react';
import { useLocale } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { useAuthStore } from '@/stores';
import { useTransactions, useCancelTransaction } from '@/hooks';
import {
  ArrowLeftRight,
  ArrowUpRight,
  ArrowDownLeft,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  MoreHorizontal,
  X,
  RotateCcw,
  Filter
} from '@/components/icons';
import { Button, Card, Dropdown, Modal, Skeleton, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { notify as toast } from '@/lib/notify';
import clsx from 'clsx';
import { addCurrency, formatCurrency } from '@/lib/currency';
import type { Transaction, TransactionFilters, TransactionStatus, TransactionType } from '@/types';

const statusConfig: Record<string, { label: string; color: string; icon: typeof Clock }> = {
  PENDING: { label: 'Menunggu', color: 'bg-warning/10 text-warning border-warning/20', icon: Clock },
  VALIDATING: { label: 'Divalidasi', color: 'bg-warning/10 text-warning border-warning/20', icon: Clock },
  PROCESSING: { label: 'Diproses', color: 'bg-primary/10 text-secondary border-primary/20', icon: RotateCcw },
  COMPLETED: { label: 'Selesai', color: 'bg-primary/10 text-primary-dark border-primary/20', icon: CheckCircle2 },
  FAILED: { label: 'Gagal', color: 'bg-error/10 text-error border-error/20', icon: XCircle },
  CANCELLED: { label: 'Dibatalkan', color: 'bg-text-secondary/10 text-text-secondary border-text-secondary/20', icon: X },
  // RELAY-014: enum gained these (ADR-0028/0030) — missing entries crashed the
  // whole page (reading 'icon' of undefined) on any held/step-up row.
  PENDING_COMPLIANCE_REVIEW: { label: 'Tinjauan AML', color: 'bg-accent/10 text-accent border-accent/20', icon: AlertCircle },
  PENDING_STEP_UP: { label: 'Butuh Verifikasi', color: 'bg-accent/10 text-accent border-accent/20', icon: AlertCircle },
};

const typeConfig: Record<string, { label: string; icon: typeof ArrowLeftRight }> = {
  INTERNAL_TRANSFER: { label: 'Transfer', icon: ArrowLeftRight },
  BIFAST_TRANSFER: { label: 'BI-FAST', icon: ArrowUpRight },
  SKN_TRANSFER: { label: 'SKN', icon: ArrowUpRight },
  RTGS_TRANSFER: { label: 'RTGS', icon: ArrowUpRight },
  QRIS_PAYMENT: { label: 'QRIS', icon: ArrowUpRight },
  BILL_PAYMENT: { label: 'Pembayaran', icon: ArrowUpRight },
  TOP_UP: { label: 'Top Up', icon: ArrowDownLeft },
};

// Helper to check if transaction is a credit (income) for current account
const isCreditType = (type: string, t?: Transaction, currentAccountId?: string | null): boolean => {
  if (type === 'TOP_UP') return true;
  if (t && currentAccountId && t.recipientAccountId === currentAccountId) return true;
  return false;
};

export default function TransactionsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  const [page, setPage] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';

  const { data: transactions, isLoading } = useTransactions(accountId || undefined, page, 20, filters);
  const cancelTransaction = useCancelTransaction();

  // Compute stats from actual transaction data
  const totalIn = transactions?.filter((t: Transaction) => isCreditType(t.type, t, accountId)).reduce((sum: string, t: Transaction) => addCurrency(sum, t.amount), '0') ?? '0';
  const totalOut = transactions?.filter((t: Transaction) => !isCreditType(t.type, t, accountId)).reduce((sum: string, t: Transaction) => addCurrency(sum, t.amount), '0') ?? '0';
  const pendingCount = transactions?.filter((t: Transaction) => t.status === 'PENDING' || t.status === 'PROCESSING').length ?? 0;
  const completedCount = transactions?.filter((t: Transaction) => t.status === 'COMPLETED').length ?? 0;

  const handleCancelClick = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
    setIsCancelDialogOpen(true);
  };

  const handleConfirmCancel = async () => {
    if (!selectedTransaction) return;

    try {
      await cancelTransaction.mutateAsync(selectedTransaction.id);
      toast.success('Transaksi berhasil dibatalkan');
      setIsCancelDialogOpen(false);
      setSelectedTransaction(null);
    } catch {
      toast.error('Gagal membatalkan transaksi');
    }
  };

  const formatAmount = (amount: string, currency: string) => {
    return formatCurrency(amount, { symbol: currency || 'Rp', locale: bcp47Locale });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(bcp47Locale, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const canCancel = (status: string) => {
    return status === 'PENDING' || status === 'PROCESSING';
  };
  const columns: TableColumnsType<Transaction> = [
    {
      key: 'date',
      title: 'Tanggal',
      render: (_, transaction) => (
        <div>
          <div className="text-sm font-bold text-foreground">{formatDate(transaction.createdAt)}</div>
          <div className="text-xs text-muted-foreground font-mono mt-1">{transaction.referenceNumber}</div>
        </div>
      ),
    },
    {
      key: 'type',
      title: 'Tipe',
      render: (_, transaction) => {
        const type = typeConfig[transaction.type] || typeConfig.INTERNAL_TRANSFER;
        const TypeIcon = type.icon;
        const credit = isCreditType(transaction.type, transaction, accountId);
        return (
          <div className="flex items-center gap-3">
            <div className={clsx(
              "h-10 w-10 rounded-xl flex items-center justify-center border",
              credit ? "bg-primary/10 border-primary/10" : "bg-muted/50 border-border/50"
            )}>
              <TypeIcon className={clsx("h-5 w-5", credit ? "text-primary" : "text-muted-foreground")} />
            </div>
            <span className="text-sm font-bold text-foreground">{type.label}</span>
          </div>
        );
      },
    },
    {
      key: 'description',
      title: 'Deskripsi',
      render: (_, transaction) => (
        <p className="text-sm font-medium text-foreground max-w-[200px] truncate">{transaction.description}</p>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      align: 'center',
      render: (_, transaction) => {
        const status = statusConfig[transaction.status];
        const StatusIcon = status.icon;
        return (
          <Tag bordered={false} className={clsx("font-bold text-xs", status.color)}>
            <StatusIcon className="h-3 w-3 mr-1" />
            {status.label}
          </Tag>
        );
      },
    },
    {
      key: 'amount',
      title: 'Jumlah',
      align: 'right',
      render: (_, transaction) => {
        const credit = isCreditType(transaction.type, transaction, accountId);
        return (
          <span className={clsx("font-bold tabular-nums", credit ? "text-primary" : "text-foreground")}>
            {credit ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
          </span>
        );
      },
    },
    {
      key: 'actions',
      title: '',
      align: 'right',
      render: (_, transaction) => (
        <Dropdown
          trigger={['click']}
          placement="bottomRight"
          menu={{
            className: 'w-48',
            items: [
              { key: 'detail', label: 'Lihat Detail' },
              ...(canCancel(transaction.status)
                ? [{ key: 'cancel', danger: true, label: 'Batalkan Transaksi', onClick: () => handleCancelClick(transaction) }]
                : []),
            ],
          }}
        >
          <Button type="text" shape="circle" className="min-h-[44px] min-w-[44px] rounded-xl" icon={<MoreHorizontal className="h-4 w-4" />} aria-label="Opsi transaksi" />
        </Dropdown>
      ),
    },
  ];

  return (
    <DashboardLayout>
      <>
        <div className="space-y-8">
          {/* Header */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h1 className="text-3xl font-bold text-foreground">Riwayat Transaksi</h1>
              <p className="text-sm text-muted-foreground font-medium mt-1">
                Kelola dan pantau semua aktivitas transaksi Anda
              </p>
            </div>
            <div className="flex gap-3">
              {(filters.status || filters.type) && (
                <Button
                  type="text"
                  size="small"
                  onClick={() => setFilters({})}
                  className="text-xs font-bold text-muted-foreground hover:text-primary"
                >
                  Hapus Filter
                </Button>
              )}
              <Dropdown
                trigger={['click']}
                placement="bottomRight"
                menu={{
                  className: 'w-48',
                  selectedKeys: filters.status ? [filters.status] : [],
                  items: [
                    { key: 'all', label: 'Semua Status', onClick: () => setFilters({ ...filters, status: undefined }) },
                    ...Object.keys(statusConfig).map((status) => ({
                      key: status,
                      label: statusConfig[status].label,
                      onClick: () => setFilters({ ...filters, status: status as TransactionStatus }),
                    })),
                  ],
                }}
              >
                <Button className="gap-2">
                  <Filter className="h-4 w-4" />
                  {filters.status ? statusConfig[filters.status].label : 'Status'}
                </Button>
              </Dropdown>

              <Dropdown
                trigger={['click']}
                placement="bottomRight"
                menu={{
                  className: 'w-56',
                  selectedKeys: filters.type ? [filters.type] : [],
                  items: [
                    { key: 'all', label: 'Semua Tipe', onClick: () => setFilters({ ...filters, type: undefined }) },
                    ...Object.keys(typeConfig).map((type) => ({
                      key: type,
                      label: typeConfig[type].label,
                      onClick: () => setFilters({ ...filters, type: type as TransactionType }),
                    })),
                  ],
                }}
              >
                <Button className="gap-2">
                  <ArrowLeftRight className="h-4 w-4" />
                  {filters.type ? typeConfig[filters.type]?.label : 'Tipe'}
                </Button>
              </Dropdown>
            </div>
          </div>

          {/* Stats Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <>
              <Card className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center border border-primary/10">
                      <ArrowDownLeft className="h-6 w-6 text-primary" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Total Masuk</p>
                       <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton.Input active size="small" className="!h-7 !w-24" /> : formatAmount(totalIn, 'IDR')}
                      </p>
                    </div>
                  </div>
              </Card>
            </>
            <>
              <Card className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-error/10 flex items-center justify-center">
                      <ArrowUpRight className="h-6 w-6 text-error" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Total Keluar</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton.Input active size="small" className="!h-7 !w-24" /> : formatAmount(totalOut, 'IDR')}
                      </p>
                    </div>
                  </div>
              </Card>
            </>
            <>
              <Card className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                      <Clock className="h-6 w-6 text-primary" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Menunggu</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton.Input active size="small" className="!h-7 !w-24" /> : String(pendingCount)}
                      </p>
                    </div>
                  </div>
              </Card>
            </>
            <>
              <Card className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-accent/10 flex items-center justify-center">
                      <CheckCircle2 className="h-6 w-6 text-accent" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Selesai</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton.Input active size="small" className="!h-7 !w-24" /> : String(completedCount)}
                      </p>
                    </div>
                  </div>
              </Card>
            </>
          </div>

          {/* Transactions Table */}
          <Card
            title={<span className="text-lg font-bold tracking-widest uppercase">Daftar Transaksi</span>}
            extra={<Tag bordered className="font-mono">Halaman {page + 1}</Tag>}
          >
            <div>
              {isLoading ? (
                <div className="space-y-4">
                  {[1, 2, 3, 4, 5].map((i) => (
                    <Skeleton.Input key={i} active className="!h-16 !w-full" />
                  ))}
                </div>
              ) : (
                <>
                  <div className="hidden md:block">
                    <Table<Transaction>
                      columns={columns}
                      dataSource={transactions ?? []}
                      rowKey="id"
                      pagination={false}
                      loading={isLoading}
                      locale={{ emptyText: 'Tidak ada transaksi' }}
                    />
                  </div>

                  {/* Mobile Layout */}
                  <div className="md:hidden space-y-4">
                    {transactions?.map((transaction: Transaction) => {
                      const status = statusConfig[transaction.status];
                      const type = typeConfig[transaction.type] || typeConfig.INTERNAL_TRANSFER;
                      const StatusIcon = status.icon;
                      const TypeIcon = type.icon;

                      return (
                        <div
                          key={transaction.id}
                          className="bg-muted/30 p-4 rounded-xl border border-border/50"
                        >
                          <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-3">
                              <div className={clsx(
                                "h-10 w-10 rounded-xl flex items-center justify-center border",
                                isCreditType(transaction.type) ? "bg-primary/10 border-primary/10" : "bg-muted/50 border-border/50"
                              )}>
                                <TypeIcon className={clsx(
                                  "h-5 w-5",
                                    isCreditType(transaction.type) ? "text-primary" : "text-muted-foreground"
                                )} />
                              </div>
                              <div>
                                <p className="text-sm font-bold text-foreground">{type.label}</p>
                                <p className="text-xs text-muted-foreground">{formatDate(transaction.createdAt)}</p>
                              </div>
                            </div>
                            <Tag bordered={false} className={clsx("font-bold text-xs", status.color)}>
                              <StatusIcon className="h-3 w-3 mr-1" />
                              {status.label}
                            </Tag>
                          </div>
                          <div className="flex items-center justify-between pt-3 border-t border-border/50">
                            <p className="text-xs text-muted-foreground truncate max-w-[150px]">
                              {transaction.description}
                            </p>
                            <p className={clsx(
                              "text-sm font-bold tabular-nums",
                              isCreditType(transaction.type) ? "text-primary" : "text-foreground"
                            )}>
                              {isCreditType(transaction.type) ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
                            </p>
                          </div>
                          {canCancel(transaction.status) && (
                            <div className="mt-3 pt-3 border-t border-border/50">
                              <Button
                                type="text"
                                danger
                                size="small"
                                className="w-full text-error hover:text-white hover:bg-error"
                                onClick={() => handleCancelClick(transaction)}
                              >
                                <X className="h-4 w-4 mr-2" />
                                Batalkan Transaksi
                              </Button>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {!transactions || transactions.length === 0 ? (
                    <div className="text-center py-8">
                      <div className="h-16 w-16 bg-muted/50 rounded-full flex items-center justify-center mx-auto mb-4">
                        <AlertCircle className="h-8 w-8 text-muted-foreground" />
                      </div>
                      <h3 className="text-lg font-bold text-foreground mb-2">Tidak Ada Transaksi</h3>
                      <p className="text-sm text-muted-foreground">
                        Anda belum memiliki transaksi. Mulai lakukan transfer atau pembayaran.
                      </p>
                    </div>
                  ) : null}

                  {/* Pagination */}
                  {transactions && transactions.length > 0 && (
                    <div className="flex items-center justify-between mt-6 pt-6 border-t border-border/50">
                      <Button
                        size="small"
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                      >
                        Sebelumnya
                      </Button>
                      <span className="text-sm text-muted-foreground">
                        Halaman {page + 1}
                      </span>
                      <Button
                        size="small"
                        onClick={() => setPage((p) => p + 1)}
                        disabled={!transactions || transactions.length < 20}
                      >
                        Selanjutnya
                      </Button>
                    </div>
                  )}
                </>
              )}
            </div>
          </Card>
        </div>
      </>

      {/* Cancel Confirmation Dialog */}
      <Modal open={isCancelDialogOpen} onCancel={() => setIsCancelDialogOpen(false)} footer={null} centered width={512} title={<Typography.Title level={4} className="!mb-1 flex items-center gap-2"><AlertCircle className="h-5 w-5 text-error" />Batalkan Transaksi?</Typography.Title>}>
        <div>
          <Typography.Text type="secondary">
            Apakah Anda yakin ingin membatalkan transaksi ini? Tindakan ini tidak dapat dibatalkan.
          </Typography.Text>
          {selectedTransaction && (
            <div className="bg-muted/50 p-4 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">Referensi</span>
                <span className="text-sm font-mono font-medium">{selectedTransaction.referenceNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">Deskripsi</span>
                <span className="text-sm font-medium truncate max-w-[150px]">{selectedTransaction.description}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-muted-foreground">Jumlah</span>
                <span className="text-sm font-bold">
                  {formatAmount(selectedTransaction.amount, selectedTransaction.currency)}
                </span>
              </div>
            </div>
          )}
          <div className="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2 gap-2">
            <Button onClick={() => setIsCancelDialogOpen(false)}>
              Batal
            </Button>
            <Button
              danger
              type="primary"
              onClick={handleConfirmCancel}
              disabled={cancelTransaction.isPending}
            >
              {cancelTransaction.isPending ? 'Membatalkan...' : 'Ya, Batalkan'}
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
