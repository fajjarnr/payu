'use client';

import React from 'react';
import { useLocale } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight, User, Landmark, Smartphone, ReceiptText, MoreHorizontal as MoreIcon, X } from '@/components/icons';
import { cn } from '@/lib/utils';
import { Badge, Button, Card, Dropdown, Skeleton, Table } from 'antd';
import type { TableColumnsType } from 'antd';



import { useTransactions, useCancelTransaction } from '@/hooks';
import { useAuthStore } from '@/stores';
import { notify as toast } from '@/lib/notify';
import { formatCurrency } from '@/lib/currency';
import type { Transaction } from '@/services/TransactionService';

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: 'Menunggu', color: 'bg-warning/10 text-warning border-warning/20' },
  VALIDATING: { label: 'Validasi', color: 'bg-accent/10 text-accent border-accent/20' },
  PROCESSING: { label: 'Diproses', color: 'bg-primary/10 text-secondary border-primary/20' },
  COMPLETED: { label: 'Selesai', color: 'bg-primary/10 text-primary-dark border-primary/20' },
  FAILED: { label: 'Gagal', color: 'bg-error/10 text-error border-error/20' },
  CANCELLED: { label: 'Batal', color: 'bg-text-secondary/10 text-text-secondary border-text-secondary/20' },
};

// Helper to check if transaction type is a credit (income)
const isCreditType = (type: string): boolean => type === 'TOP_UP';

interface TransferActivityProps {
  className?: string;
}

export default function TransferActivity({ className = '' }: TransferActivityProps) {
  // BUG-FE-008 FIX: Use dynamic locale instead of hardcoded 'id-ID'
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';
  const accountId = useAuthStore((state) => state.accountId);
  const { data: transactions, isLoading } = useTransactions(accountId || undefined, 0, 5);
  const cancelTransaction = useCancelTransaction();

  const handleCancel = async (transactionId: string) => {
    try {
      await cancelTransaction.mutateAsync(transactionId);
      toast.success('Transaksi berhasil dibatalkan');
    } catch {
      toast.error('Gagal membatalkan transaksi');
    }
  };

  const canCancel = (status: string) => status === 'PENDING' || status === 'PROCESSING';

  // Wire data can carry DECIMAL as a JSON number; formatCurrency already
  // normalizes number input. Keep the string path byte-identical (direction
  // sign is rendered by the caller from transaction type).
  const formatAmount = (amount: string | number) => {
    const unsigned = typeof amount === 'string' ? amount.replace(/^-/, '') : amount;
    return formatCurrency(unsigned, { locale: bcp47Locale });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(bcp47Locale, {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };
  const displayTransactions = transactions || [];
  const columns: TableColumnsType<Transaction> = [
    {
      key: 'date',
      title: 'Tanggal',
      render: (_, item) => (
        <div>
          <div className="text-xs sm:text-xs text-muted-foreground font-bold tabular-nums uppercase tracking-tighter opacity-70">
            {formatDate(item.createdAt)}
          </div>
          <div className="text-xs font-mono text-muted-foreground/50 mt-1">
            {item.referenceNumber}
          </div>
        </div>
      ),
    },
    {
      key: 'recipient',
      title: 'Penerima',
      render: (_, item) => (
        <div className="flex items-center gap-4">
          <div className="h-12 w-12 rounded-xl bg-accent flex items-center justify-center border border-border group-hover:scale-110 transition-transform shadow-sm">
            <User className="h-6 w-6 text-primary" />
          </div>
          <div>
            <p className="text-xs sm:text-xs text-muted-foreground font-bold tracking-widest leading-none mb-2 uppercase opacity-60">{item.type}</p>
            <p className="text-sm font-bold text-foreground uppercase tracking-tight truncate max-w-[150px]">{item.description}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      align: 'center',
      render: (_, item) => (
        <Badge count={statusConfig[item.status]?.label || item.status} showZero color="green" className="inline-flex items-center" classNames={{ indicator: cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold shadow-none transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 bg-transparent text-foreground font-bold text-xs", statusConfig[item.status]?.color || statusConfig.PENDING.color) }} />
      ),
    },
    {
      key: 'amount',
      title: 'Jumlah',
      align: 'right',
      render: (_, item) => (
        <p className={cn(
          "text-sm sm:text-base font-bold tabular-nums tracking-tight",
          isCreditType(item.type) ? "text-primary-dark" : "text-foreground"
        )}>
          {isCreditType(item.type) ? '+' : '-'}{formatAmount(item.amount)}
        </p>
      ),
    },
    {
      key: 'actions',
      title: '',
      align: 'right',
      render: (_, item) => (
        <Dropdown
          menu={{ items: [{ key: 'detail', label: (<span className="relative flex cursor-default select-none items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground cursor-pointer">Lihat Detail</span>) }, ...(canCancel(item.status) ? [{ key: 'cancel', danger: true, label: (<span className="relative flex cursor-default select-none items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground cursor-pointer text-error focus:text-error"><X className="h-4 w-4 mr-2" />Batalkan</span>), onClick: () => handleCancel(item.id) }] : [])] }}
          trigger={["click"]}
          placement="bottomRight"
        >
          <Button type="default" aria-label="Opsi transaksi" className="h-11 w-11 text-muted-foreground hover:text-foreground hover:bg-muted rounded-xl border-transparent bg-transparent hover:border-transparent active:bg-transparent active:border-transparent" icon={<MoreHorizontal className="h-4 w-4" />} />
        </Dropdown>
      ),
    },
  ];

  return (
    <div data-testid="transfer-activity-section" className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 lg:gap-8", className)}>
      {/* Quick Transfer Section - Now on the Left */}
      <Card data-testid="quick-transfer-card" className="lg:col-span-4 relative overflow-hidden group min-h-[320px]" styles={{ body: { display: 'contents' } }}>
        {/* Decorative background */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl transition-transform group-hover:scale-125 pointer-events-none" />

        <div className="flex flex-col space-y-1.5 p-6 flex flex-row items-center justify-between pb-6">
          <h3 className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Kirim Cepat
          </h3>
          <div className="p-3 bg-muted/50 rounded-xl hover:bg-muted transition-colors cursor-pointer border border-transparent hover:border-border shadow-sm">
            <Search className="h-5 w-5 text-muted-foreground" />
          </div>
        </div>

        <div className="p-6 pt-0 space-y-8 relative z-10 flex flex-col justify-between h-full">
          <div>
            <p className="text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.25em] mb-6 text-center uppercase opacity-60">Kategori Favorit</p>
            <div className="grid grid-cols-4 gap-6 px-1">
              {[
                { icon: Landmark, label: 'Bank' },
                { icon: Smartphone, label: 'E-Wallet' },
                { icon: ReceiptText, label: 'Tagihan' },
                { icon: MoreIcon, label: 'Lain' },
              ].map((item, i) => (
                <Button key={i} data-testid={`quick-transfer-category-${item.label.toLowerCase()}`} type="default" className="flex flex-col items-center gap-4 group/btn h-auto p-0 hover:bg-transparent border-transparent bg-transparent hover:bg-muted hover:text-foreground hover:border-transparent active:bg-transparent active:border-transparent">
                  <div className="h-14 w-14 sm:h-16 sm:w-16 rounded-2xl bg-primary/[0.03] flex items-center justify-center group-hover/btn:bg-primary/10 group-hover/btn:scale-105 transition-all border border-primary/10 group-hover/btn:border-primary/30 shadow-sm overflow-hidden">
                    <item.icon className="h-6 w-6 sm:h-7 sm:w-7 text-primary" />
                  </div>
                  <span className="text-xs sm:text-xs font-bold text-muted-foreground group-hover/btn:text-primary uppercase tracking-tighter">{item.label}</span>
                </Button>
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs sm:text-xs font-bold text-muted-foreground tracking-[0.25em] mb-6 text-center uppercase opacity-60">Kontak Terbaru</p>
            <div className="flex justify-between items-center px-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-12 w-12 sm:h-14 sm:w-14 rounded-2xl bg-accent border-2 border-background shadow-md flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 active:scale-95 transition-all hover:ring-2 ring-primary/30 group/avatar">
                  <User className="h-6 w-6 sm:h-7 sm:w-7 text-primary/60 group-hover/avatar:text-primary transition-colors" />
                </div>
              ))}
            </div>
          </div>

          <Button data-testid="quick-transfer-send-button" type="primary" size="large" className="w-full uppercase tracking-[0.2em] font-bold text-xs h-14 sm:h-16 rounded-2xl shadow-xl shadow-primary/10 hover:shadow-primary/20 transition-all mt-4">
            Kirim Sekarang
          </Button>
        </div>
      </Card>

      {/* Recent Transfer Activity List - Now on the Right */}
      <Card data-testid="recent-activity-card" className="lg:col-span-8 overflow-hidden" styles={{ body: { display: 'contents' } }}>
        <div className="flex flex-col space-y-1.5 p-6 flex flex-row items-center justify-between pb-6">
          <h3 className="text-2xl font-bold leading-none tracking-tight text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            Aktivitas Terakhir
          </h3>
          <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground bg-muted/50 px-5 py-2.5 rounded-xl cursor-pointer hover:bg-muted transition-colors border border-transparent hover:border-border tracking-widest uppercase shadow-sm">
            Januari <ChevronDown className="h-4 w-4" />
          </div>
        </div>

        <div className="p-6 pt-0 overflow-x-auto">
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} title={false} paragraph={false} className="block animate-pulse rounded-xl bg-muted/50 h-16 w-full" />
              ))}
            </div>
          ) : (
            <>
              <div className="hidden md:block">
                <Table<Transaction>
                  columns={columns}
                  dataSource={displayTransactions}
                  rowKey="id"
                  pagination={false}
                  loading={isLoading}
                  locale={{ emptyText: 'Belum ada transaksi' }}
                  onRow={(record) => ({ 'data-testid': `transfer-row-${record.id}` }) as React.HTMLAttributes<HTMLElement>}
                />
              </div>

              {/* Mobile Card Layout */}
              <div className="md:hidden space-y-4">
                {displayTransactions.map((item: Transaction) => (
                  <div key={item.id} data-testid={`transfer-card-mobile-${item.id}`} className="bg-muted/30 p-4 rounded-xl border border-transparent hover:border-primary/20 transition-all">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-xl bg-card flex items-center justify-center border border-border shadow-sm">
                          <User className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <p className="text-xs font-bold text-foreground leading-tight truncate max-w-[120px]">{item.description}</p>
                          <p className="text-xs font-semibold text-muted-foreground tracking-widest leading-none uppercase">{item.type}</p>
                        </div>
                      </div>
                      <p className={cn(
                        "text-sm font-bold tabular-nums",
                        isCreditType(item.type) ? "text-primary-dark" : "text-foreground"
                      )}>
                        {isCreditType(item.type) ? '+' : '-'}{formatAmount(item.amount)}
                      </p>
                    </div>
                    <div className="flex items-center justify-between pt-3 border-t border-border/50">
                      <p className="text-xs font-medium text-muted-foreground">{formatDate(item.createdAt)}</p>
                      <div className="flex items-center gap-2">
                        <Badge count={statusConfig[item.status]?.label || item.status} showZero color="green" className="inline-flex items-center" classNames={{ indicator: cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold shadow-none transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 bg-transparent text-foreground font-bold text-xs", statusConfig[item.status]?.color || statusConfig.PENDING.color) }} />
                        {canCancel(item.status) && (
                          <Button
                            type="default"
                            aria-label="Batalkan transaksi"
                            className="h-11 w-11 text-error hover:text-white hover:bg-error border-transparent bg-transparent hover:border-transparent active:bg-transparent active:border-transparent"
                            icon={<X className="h-4 w-4" />}
                            onClick={() => handleCancel(item.id)}
                          />
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {!displayTransactions.length && (
                <div className="text-center py-12">
                  <div className="h-16 w-16 bg-muted/50 rounded-full flex items-center justify-center mx-auto mb-4">
                    <ReceiptText className="h-8 w-8 text-muted-foreground" />
                  </div>
                  <h3 className="text-lg font-bold text-foreground mb-2">Belum Ada Transaksi</h3>
                  <p className="text-sm text-muted-foreground">
                    Transaksi Anda akan muncul di sini
                  </p>
                </div>
              )}

              <div className="mt-6 pt-6 border-t border-border flex flex-col sm:flex-row justify-between items-center gap-6">
                <Button type="default" size="small" data-testid="repeat-last-transfer-button" className="flex items-center gap-3 text-xs font-bold text-primary hover:text-primary/80 tracking-widest transition-colors uppercase min-h-[44px] h-auto p-0 hover:bg-transparent border-transparent bg-transparent hover:border-transparent active:bg-transparent active:border-transparent" icon={<RotateCcw className="h-4 w-4" />}>Ulangi Transfer Terakhir
                </Button>
                <Link href="/transactions">
                  <Button type="default" size="small" data-testid="view-full-history-button" className="flex items-center gap-3 text-xs font-bold text-primary hover:underline tracking-widest transition-all uppercase min-h-[44px] h-auto p-0 hover:bg-transparent border-transparent bg-transparent hover:border-transparent active:bg-transparent active:border-transparent" icon={<ArrowRight className="h-4 w-4" />}>Riwayat Lengkap
                  </Button>
                </Link>
              </div>
            </>
          )}
        </div>
      </Card>
    </div>
  );
}
