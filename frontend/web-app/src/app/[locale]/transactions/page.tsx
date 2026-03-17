'use client';

import React, { useState } from 'react';
import { useTranslations } from 'next-intl';
import DashboardLayout from '@/components/DashboardLayout';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
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
  Calendar,
  Filter
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import clsx from 'clsx';
import type { Transaction } from '@/services/TransactionService';

const statusConfig: Record<string, { label: string; color: string; icon: typeof Clock }> = {
  PENDING: { label: 'Menunggu', color: 'bg-yellow-500/10 text-yellow-600 border-yellow-500/20', icon: Clock },
  PROCESSING: { label: 'Diproses', color: 'bg-blue-500/10 text-blue-600 border-blue-500/20', icon: RotateCcw },
  COMPLETED: { label: 'Selesai', color: 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20', icon: CheckCircle2 },
  FAILED: { label: 'Gagal', color: 'bg-red-500/10 text-red-600 border-red-500/20', icon: XCircle },
  CANCELLED: { label: 'Dibatalkan', color: 'bg-gray-500/10 text-gray-600 border-gray-500/20', icon: X },
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

// Helper to check if transaction type is a credit (income)
const isCreditType = (type: string): boolean => type === 'TOP_UP';

export default function TransactionsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  const [page, setPage] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false);

  const { data: transactions, isLoading } = useTransactions(accountId || undefined, page, 20);
  const cancelTransaction = useCancelTransaction();

  // Compute stats from actual transaction data
  const totalIn = transactions?.filter((t: Transaction) => isCreditType(t.type)).reduce((sum: number, t: Transaction) => sum + t.amount, 0) ?? 0;
  const totalOut = transactions?.filter((t: Transaction) => !isCreditType(t.type)).reduce((sum: number, t: Transaction) => sum + t.amount, 0) ?? 0;
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
    } catch (error) {
      toast.error('Gagal membatalkan transaksi');
    }
  };

  const formatAmount = (amount: number, currency: string) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: currency || 'IDR',
      minimumFractionDigits: 0,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('id-ID', {
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

  return (
    <DashboardLayout>
      <PageTransition>
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
              <Button variant="outline" className="gap-2">
                <Calendar className="h-4 w-4" />
                Filter Tanggal
              </Button>
              <Button variant="outline" className="gap-2">
                <Filter className="h-4 w-4" />
                Filter
              </Button>
            </div>
          </div>

          {/* Stats Cards */}
          <StaggerContainer className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <StaggerItem>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-emerald-500/10 flex items-center justify-center">
                      <ArrowDownLeft className="h-6 w-6 text-emerald-500" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Total Masuk</p>
                       <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton className="h-7 w-24" /> : formatAmount(totalIn, 'IDR')}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </StaggerItem>
            <StaggerItem>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-red-500/10 flex items-center justify-center">
                      <ArrowUpRight className="h-6 w-6 text-red-500" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Total Keluar</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton className="h-7 w-24" /> : formatAmount(totalOut, 'IDR')}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </StaggerItem>
            <StaggerItem>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-blue-500/10 flex items-center justify-center">
                      <Clock className="h-6 w-6 text-blue-500" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Menunggu</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton className="h-7 w-24" /> : String(pendingCount)}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </StaggerItem>
            <StaggerItem>
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-purple-500/10 flex items-center justify-center">
                      <CheckCircle2 className="h-6 w-6 text-purple-500" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Selesai</p>
                      <p className="text-xl font-bold text-foreground">
                        {isLoading ? <Skeleton className="h-7 w-24" /> : String(completedCount)}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </StaggerItem>
          </StaggerContainer>

          {/* Transactions Table */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg font-bold tracking-widest uppercase">Daftar Transaksi</CardTitle>
              <Badge variant="outline" className="font-mono">
                Halaman {page + 1}
              </Badge>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="space-y-4">
                  {[1, 2, 3, 4, 5].map((i) => (
                    <Skeleton key={i} className="h-16 w-full" />
                  ))}
                </div>
              ) : (
                <>
                  <div className="hidden md:block">
                    <Table>
                      <TableHeader>
                        <TableRow className="border-b border-border/50 hover:bg-transparent">
                          <TableHead className="h-14 text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase">
                            Tanggal
                          </TableHead>
                          <TableHead className="h-14 text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase">
                            Tipe
                          </TableHead>
                          <TableHead className="h-14 text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase">
                            Deskripsi
                          </TableHead>
                          <TableHead className="h-14 text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase text-center">
                            Status
                          </TableHead>
                          <TableHead className="h-14 text-xs font-bold text-muted-foreground tracking-[0.2em] uppercase text-right">
                            Jumlah
                          </TableHead>
                          <TableHead className="h-14 w-[50px]"></TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {transactions?.map((transaction: Transaction) => {
                          const status = statusConfig[transaction.status];
                          const type = typeConfig[transaction.type] || typeConfig.TRANSFER;
                          const StatusIcon = status.icon;
                          const TypeIcon = type.icon;

                          return (
                            <TableRow
                              key={transaction.id}
                              className="group border-b border-border/30 hover:bg-muted/30 transition-colors"
                            >
                              <TableCell className="py-6 whitespace-nowrap">
                                <div className="text-sm font-bold text-foreground">
                                  {formatDate(transaction.createdAt)}
                                </div>
                                <div className="text-xs text-muted-foreground font-mono mt-1">
                                  {transaction.referenceNumber}
                                </div>
                              </TableCell>
                              <TableCell className="py-6">
                                <div className="flex items-center gap-3">
                                  <div className={clsx(
                                    "h-10 w-10 rounded-xl flex items-center justify-center",
                                    isCreditType(transaction.type) ? "bg-emerald-500/10" : "bg-primary/10"
                                  )}>
                                    <TypeIcon className={clsx(
                                      "h-5 w-5",
                                      isCreditType(transaction.type) ? "text-emerald-500" : "text-primary"
                                    )} />
                                  </div>
                                  <span className="text-sm font-bold text-foreground">{type.label}</span>
                                </div>
                              </TableCell>
                              <TableCell className="py-6">
                                <p className="text-sm font-medium text-foreground max-w-[200px] truncate">
                                  {transaction.description}
                                </p>
                              </TableCell>
                              <TableCell className="py-6 text-center">
                                <Badge variant="outline" className={clsx("font-bold text-xs", status.color)}>
                                  <StatusIcon className="h-3 w-3 mr-1" />
                                  {status.label}
                                </Badge>
                              </TableCell>
                              <TableCell className={clsx(
                                "py-6 text-right font-bold tabular-nums",
                                isCreditType(transaction.type) ? "text-emerald-600" : "text-foreground"
                              )}>
                                {isCreditType(transaction.type) ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
                              </TableCell>
                              <TableCell className="py-6 text-right">
                                <DropdownMenu>
                                  <DropdownMenuTrigger asChild>
                                    <Button variant="ghost" size="icon" className="h-9 w-9 rounded-xl">
                                      <MoreHorizontal className="h-4 w-4" />
                                    </Button>
                                  </DropdownMenuTrigger>
                                  <DropdownMenuContent align="end" className="w-48">
                                    <DropdownMenuItem className="cursor-pointer">
                                      Lihat Detail
                                    </DropdownMenuItem>
                                    {canCancel(transaction.status) && (
                                      <DropdownMenuItem
                                        className="cursor-pointer text-red-600 focus:text-red-600"
                                        onClick={() => handleCancelClick(transaction)}
                                      >
                                        Batalkan Transaksi
                                      </DropdownMenuItem>
                                    )}
                                  </DropdownMenuContent>
                                </DropdownMenu>
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Mobile Layout */}
                  <div className="md:hidden space-y-4">
                    {transactions?.map((transaction: Transaction) => {
                      const status = statusConfig[transaction.status];
                      const type = typeConfig[transaction.type] || typeConfig.TRANSFER;
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
                                "h-10 w-10 rounded-xl flex items-center justify-center",
                                isCreditType(transaction.type) ? "bg-emerald-500/10" : "bg-primary/10"
                              )}>
                                <TypeIcon className={clsx(
                                  "h-5 w-5",
                                    isCreditType(transaction.type) ? "text-emerald-500" : "text-primary"
                                )} />
                              </div>
                              <div>
                                <p className="text-sm font-bold text-foreground">{type.label}</p>
                                <p className="text-xs text-muted-foreground">{formatDate(transaction.createdAt)}</p>
                              </div>
                            </div>
                            <Badge variant="outline" className={clsx("font-bold text-xs", status.color)}>
                              <StatusIcon className="h-3 w-3 mr-1" />
                              {status.label}
                            </Badge>
                          </div>
                          <div className="flex items-center justify-between pt-3 border-t border-border/50">
                            <p className="text-xs text-muted-foreground truncate max-w-[150px]">
                              {transaction.description}
                            </p>
                            <p className={clsx(
                              "text-sm font-bold tabular-nums",
                              isCreditType(transaction.type) ? "text-emerald-600" : "text-foreground"
                            )}>
                              {isCreditType(transaction.type) ? '+' : '-'}{formatAmount(transaction.amount, transaction.currency)}
                            </p>
                          </div>
                          {canCancel(transaction.status) && (
                            <div className="mt-3 pt-3 border-t border-border/50">
                              <Button
                                variant="ghost"
                                size="sm"
                                className="w-full text-red-600 hover:text-red-600 hover:bg-red-50"
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
                    <div className="text-center py-16">
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
                        variant="outline"
                        size="sm"
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                      >
                        Sebelumnya
                      </Button>
                      <span className="text-sm text-muted-foreground">
                        Halaman {page + 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPage((p) => p + 1)}
                        disabled={!transactions || transactions.length < 20}
                      >
                        Selanjutnya
                      </Button>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </PageTransition>

      {/* Cancel Confirmation Dialog */}
      <Dialog open={isCancelDialogOpen} onOpenChange={setIsCancelDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <AlertCircle className="h-5 w-5 text-red-500" />
              Batalkan Transaksi?
            </DialogTitle>
            <DialogDescription>
              Apakah Anda yakin ingin membatalkan transaksi ini? Tindakan ini tidak dapat dibatalkan.
            </DialogDescription>
          </DialogHeader>
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
          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={() => setIsCancelDialogOpen(false)}>
              Batal
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirmCancel}
              disabled={cancelTransaction.isPending}
            >
              {cancelTransaction.isPending ? 'Membatalkan...' : 'Ya, Batalkan'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}
