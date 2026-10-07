'use client';

import { Search, ChevronRight, PlusCircle, LifeBuoy, ArrowRight, Clock, Calendar as CalendarIcon, Zap, Truck } from '@/components/icons';
import { Button, DatePicker, Form, Input } from 'antd';
import dayjs from 'dayjs';
import { transferSchema, type TransferRequest, type TransferType, type TransferScheduleType } from '@/types';
import { zodFieldRule } from '@/lib/zodForm';
import { compareCurrency, parseCurrencyExact } from '@/lib/currency';
import { useState, useCallback, useMemo, useEffect } from 'react';
import { useInitiateTransfer } from '@/hooks';
import { useAuthStore } from '@/stores';
import { useBeneficiaries } from '@/hooks/useBeneficiaries';
import { useUIStore } from '@/stores';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { format } from "date-fns";
import { id } from "date-fns/locale";
import { formatCurrencyWithoutSymbol } from '@/lib/currency';

const TRANSFER_TYPES: { type: TransferType; label: string; description: string; icon: React.ComponentType<{ className?: string }>; fee: string; maxLimit: string; processingTime: string }[] = [
  {
    type: 'INTERNAL_TRANSFER',
    label: 'Transfer Instan',
    description: 'Transfer antar rekening PayU seketika',
    icon: Zap,
    fee: 'Gratis',
    maxLimit: 'Rp 100.000.000',
    processingTime: 'Seketika'
  },
  {
    type: 'BIFAST_TRANSFER',
    label: 'BI-FAST',
    description: 'Transfer real-time antar bank nasional',
    icon: Zap,
    fee: 'Gratis',
    maxLimit: 'Rp 250.000.000',
    processingTime: 'Seketika'
  },
  {
    type: 'SKN_TRANSFER',
    label: 'SKN',
    description: 'Transfer kliring nasional',
    icon: Truck,
    fee: 'Gratis',
    maxLimit: 'Rp 100.000.000',
    processingTime: 'Hari kerja'
  },
  {
    type: 'RTGS_TRANSFER',
    label: 'RTGS',
    description: 'Transfer real-time gross settlement',
    icon: Clock,
    fee: 'Gratis',
    maxLimit: 'Tidak terbatas',
    processingTime: 'Seketika'
  }
];

const SCHEDULE_TYPES: { type: TransferScheduleType; label: string; description: string }[] = [
  {
    type: 'NOW',
    label: 'Sekarang',
    description: 'Proses transfer segera'
  },
  {
    type: 'SCHEDULED',
    label: 'Terjadwal',
    description: 'Tentukan tanggal pengiriman'
  },
  {
    type: 'RECURRING',
    label: 'Berulang',
    description: 'Pengiriman rutin bulanan'
  }
];
export interface ReviewContact {
  name: string;
  initial: string;
  color: string;
  accountId: string;
}

/**
 * Resolve the review-screen recipient. Favorites/beneficiaries win; a
 * manually typed account id falls back to itself so the review never
 * renders a nameless recipient.
 */
export function resolveReviewContact(
  contacts: ReviewContact[],
  selected: string | null,
  typedAccountId: string | null | undefined,
): ReviewContact | undefined {
  const accountId = selected ?? typedAccountId ?? '';
  return (
    contacts.find((c) => c.accountId === accountId) ??
    (accountId
      ? { name: accountId, initial: accountId.charAt(0).toUpperCase(), color: 'bg-muted text-muted-foreground', accountId }
      : undefined)
  );
}

export default function TransferPage() {
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [showReview, setShowReview] = useState(false);
  const accountId = useAuthStore((state) => state.accountId);
  const addToast = useUIStore((state) => state.addToast);
  const transferMutation = useInitiateTransfer();

  const { data: beneficiaries } = useBeneficiaries(accountId || undefined);
  const recentContacts: Array<{ name: string; initial: string; color: string; accountId: string }> = (beneficiaries ?? []).map((b) => ({
        name: b.nickname || b.accountName || b.accountNumber.slice(-4),
        initial: (b.nickname || b.accountName || 'B').charAt(0).toUpperCase(),
        color: 'bg-primary-light text-primary-dark',
        accountId: b.accountNumber,
      }));

  const [form] = Form.useForm<TransferRequest>();
  const rule = (field: string) => zodFieldRule(form, transferSchema, field);
  const onValid = (values: TransferRequest) => onSubmit(transferSchema.parse(values) as TransferRequest);

  const amount = Form.useWatch('amount', form) ?? '0';
  const transferType = Form.useWatch('transferType', form) ?? 'INTERNAL_TRANSFER';
  const scheduleType = Form.useWatch('scheduleType', form) ?? 'NOW';
  const toAccountId = Form.useWatch('toAccountId', form);
  const description = Form.useWatch('description', form);
  const scheduledAt = Form.useWatch('scheduledAt', form);
  const recurringDay = Form.useWatch('recurringDay', form);
  const recurringMonth = Form.useWatch('recurringMonth', form);

  // TRF-SUBMIT-001: sender must default from the session account so typing a
  // recipient manually (no favorite contact) still passes schema validation.
  useEffect(() => {
    if (accountId) form.setFieldsValue({ fromAccountId: accountId });
  }, [accountId, form]);

  const handleContactSelect = (contact: { name: string; accountId: string }) => {
    setSelectedContact(contact.accountId);
    form.setFieldsValue({ toAccountId: contact.accountId, fromAccountId: accountId || '' });
  };

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value.replace(/\D/g, '');
    form.setFieldsValue({ amount: rawValue || '0' });
  };

  const formattedAmount = amount === '0' ? '' : formatCurrencyWithoutSymbol(amount);

  const onSubmit = (data: TransferRequest) => {
    let scheduledAt = undefined;
    let recurringDay = undefined;
    let recurringMonth = undefined;

    if (data.scheduleType === 'SCHEDULED' && data.scheduledAt) {
      scheduledAt = data.scheduledAt;
    } else if (data.scheduleType === 'RECURRING') {
      recurringDay = data.recurringDay;
      recurringMonth = data.recurringMonth;
    }

    transferMutation.mutate(
      {
        senderAccountId: data.fromAccountId || accountId || '',
        recipientAccountNumber: data.toAccountId,
        amount: parseCurrencyExact(data.amount),
        description: data.description || '',
        type: data.transferType || 'INTERNAL_TRANSFER',
        scheduledAt,
        recurringDay,
        recurringMonth
      },
      {
        onSuccess: () => {
          const message = data.scheduleType === 'NOW'
            ? 'Transfer berhasil!'
            : data.scheduleType === 'SCHEDULED'
            ? 'Transfer terjadwal berhasil diset!'
            : 'Transfer berulang berhasil diset!';
          addToast(message, 'success');
          setShowReview(false);
          setSelectedContact(null);
          form.setFieldsValue({ amount: '0', description: '' });
        },
        onError: () => {
          addToast('Transfer gagal. Silakan coba lagi.', 'error');
        }
      }
    );
  };

  // Memoize form values for review to prevent React Compiler warnings
  const formValues = useMemo(() => ({
    amount,
    toAccountId,
    description,
    scheduleType,
    scheduledAt,
    recurringDay,
    recurringMonth,
    fromAccountId: accountId || ''
  }), [amount, toAccountId, description, scheduleType, scheduledAt, recurringDay, recurringMonth, accountId]);

  const handleReview = useCallback(() => {
    if (!formValues.toAccountId || compareCurrency((formValues.amount ?? '0') as string, '0' as string) <= 0) {
      addToast('Silakan pilih penerima dan masukkan jumlah transfer', 'warning');
      return;
    }

    if (formValues.scheduleType === 'SCHEDULED' && !formValues.scheduledAt) {
      addToast('Silakan tentukan tanggal transfer', 'warning');
      return;
    }

    if (formValues.scheduleType === 'RECURRING' && (!formValues.recurringDay || !formValues.recurringMonth)) {
      addToast('Silakan tentukan tanggal dan bulan transfer', 'warning');
      return;
    }

    setShowReview(true);
  }, [formValues, addToast]);

  const selectedTransferType = TRANSFER_TYPES.find(t => t.type === transferType);

  if (showReview) {
    const reviewAccountId = selectedContact ?? formValues.toAccountId ?? '';
    const selectedContactData = resolveReviewContact(recentContacts, selectedContact, formValues.toAccountId);
    const selectedScheduleType = SCHEDULE_TYPES.find(s => s.type === scheduleType);
    const TransferTypeIcon = selectedTransferType?.icon || Zap;

    return (
      <DashboardLayout>
          <div className="space-y-6 lg:space-y-8">
                <div className="flex items-center gap-6">
                  <Button
                    type="default"
                    data-testid="back-from-review-button"
                    onClick={() => setShowReview(false)}
                    className="w-14 h-14 bg-card rounded-xl border border-border shadow-sm"
                    aria-label="Kembali"
                  >
                    <ChevronRight className="h-6 w-6 rotate-180" />
                  </Button>
                </div>

                <div className="bg-card rounded-xl p-5 sm:p-6 shadow-card border border-border relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />

                  <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6 mb-8 pb-8 border-b border-border">
                    <div className="flex items-center gap-6">
                      <div className={clsx("w-20 h-20 rounded-2xl flex items-center justify-center font-bold text-3xl shadow-lg transition-transform group-hover:rotate-3", selectedContactData?.color)}>
                        {selectedContactData?.initial}
                      </div>
                      <div>
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">Kepada Penerima</p>
                        <h3 className="text-2xl font-bold text-foreground">{selectedContactData?.name}</h3>
                        <p className="text-xs font-bold text-primary tracking-tight">ID Akun: {reviewAccountId}</p>
                      </div>
                    </div>
                    <div className="text-left md:text-right">
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">Jumlah Transfer</p>
                      <p className="text-4xl sm:text-4xl lg:text-5xl font-bold text-foreground">Rp {formatCurrencyWithoutSymbol(amount)}</p>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mt-2">Mata Uang IDR</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6 relative z-10">
                    <div className="bg-muted p-6 rounded-xl border border-border">
                      <div className="flex items-center gap-2 mb-2">
                        <TransferTypeIcon className="h-4 w-4 text-primary" />
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Tipe Transfer</p>
                      </div>
                      <p className="font-bold text-foreground text-sm">{selectedTransferType?.label}</p>
                      <p className="text-xs text-muted-foreground mt-1">{selectedTransferType?.processingTime}</p>
                    </div>
                    <div className="bg-muted p-6 rounded-xl border border-border">
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">Biaya Transfer</p>
                      <p className="font-bold text-foreground text-sm">{selectedTransferType?.fee}</p>
                    </div>
                    <div className="bg-muted p-6 rounded-xl border border-border">
                      <div className="flex items-center gap-2 mb-2">
                        {scheduleType !== 'NOW' && <CalendarIcon className="h-4 w-4 text-primary" />}
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Jadwal</p>
                      </div>
                      <p className="font-bold text-foreground text-sm">{selectedScheduleType?.label}</p>
                      {scheduleType === 'SCHEDULED' && scheduledAt && (
                        <p className="text-xs text-muted-foreground mt-1">
                          {format(new Date(scheduledAt), 'PPP', { locale: id })}
                        </p>
                      )}
                      {scheduleType === 'RECURRING' && (
                        <p className="text-xs text-muted-foreground mt-1">Tanggal {recurringDay || '-'}-{recurringMonth || 'setiap bulan'}</p>
                      )}
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10">
                    <div className="bg-muted p-5 sm:p-6 lg:p-8 rounded-xl border border-border">
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">Kantong Sumber</p>
                      <p className="font-bold text-foreground text-lg">Kantong Utama Cair</p>
                      <p className="text-xs font-bold text-primary tracking-widest uppercase mt-2">Saldo: Rp 86.353.000</p>
                    </div>
                    {description && (
                      <div className="bg-muted p-5 sm:p-6 lg:p-8 rounded-xl border border-border">
                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2">Pesan Konfirmasi</p>
                        <p className="font-bold text-foreground text-lg">&quot;{description}&quot;</p>
                      </div>
                    )}
                  </div>
                </div>

                  <Button
                    type="primary"
                    onClick={() => form.submit()}
                    data-testid="confirm-transfer-button"
                    disabled={transferMutation.isPending}
                    className="w-full h-16 rounded-2xl shadow-2xl shadow-primary/20"
                  >
                    {transferMutation.isPending ? 'Memvalidasi Transaksi...' : 'Otorisasi Transfer Sekarang'}
                  </Button>
          </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
        <div className="space-y-6 lg:space-y-8">
              <div className="mb-6 sm:mb-8">
                <h2 className="text-2xl sm:text-3xl font-bold text-foreground">Transfer Instan</h2>
                <p className="text-sm text-muted-foreground font-medium mt-1">Kirim dana secara aman dalam hitungan detik.</p>
              </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-6 lg:gap-8">
              <div className="lg:col-span-8 space-y-6 sm:space-y-8">
              <Form form={form} onFinish={onValid} initialValues={{ amount: '0', transferType: 'INTERNAL_TRANSFER', scheduleType: 'NOW' }} layout="vertical">
                <Form.Item name="transferType" rules={[rule('transferType')]} noStyle>
                  <Input type="hidden" />
                </Form.Item>
                <Form.Item name="scheduleType" rules={[rule('scheduleType')]} noStyle>
                  <Input type="hidden" />
                </Form.Item>
                <Form.Item name="fromAccountId" rules={[rule('fromAccountId')]} noStyle>
                  <Input type="hidden" />
                </Form.Item>
                <div className="bg-card rounded-xl sm:rounded-2xl p-4 sm:p-6 lg:p-8 border border-border shadow-card">
                  <h3 className="text-xs sm:text-sm font-bold text-foreground mb-4 sm:mb-6 tracking-widest uppercase">Pilih Metode Transfer</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {TRANSFER_TYPES.map((t) => {
                      const Icon = t.icon;
                      const isSelected = transferType === t.type;
                      return (
                        <Button type="text" htmlType="button"
                          key={t.type}
                          onClick={() => form.setFieldsValue({ transferType: t.type })}
                          data-testid={`transfer-type-${t.type.toLowerCase()}`}
                          className={clsx(
                            "flex flex-col gap-4 p-6 rounded-xl border-2 transition-all group",
                            isSelected
                              ? "bg-primary/5 border-primary shadow-lg shadow-primary/10"
                              : "bg-muted border-transparent hover:border-border hover:bg-card"
                          )}
                        >
                          <div className="flex items-start justify-between">
                            <div className={clsx("h-12 w-12 rounded-xl flex items-center justify-center", isSelected ? "bg-primary/10 text-primary" : "bg-muted/50 text-muted-foreground")}>
                              <Icon className="h-6 w-6" />
                            </div>
                            {isSelected && <div className="h-2 w-2 bg-primary rounded-full animate-pulse" />}
                          </div>
                          <div className="text-left">
                            <h4 className="font-bold text-foreground text-sm mb-1">{t.label}</h4>
                            <p className="text-xs text-muted-foreground mb-2">{t.description}</p>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-muted-foreground uppercase tracking-widest bg-muted/50 px-2 py-1 rounded">{t.fee}</span>
                              <span className="text-xs font-bold text-muted-foreground uppercase tracking-widest bg-muted/50 px-2 py-1 rounded">{t.processingTime}</span>
                            </div>
                          </div>
                        </Button>
                      );
                    })}
                  </div>
                </div>

                <div className="bg-card rounded-xl sm:rounded-2xl p-4 sm:p-6 lg:p-8 border border-border shadow-card">
                  <h3 className="text-xs sm:text-sm font-bold text-foreground mb-4 sm:mb-6 tracking-widest uppercase">Jadwal Transfer</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {SCHEDULE_TYPES.map((s) => {
                      const isSelected = scheduleType === s.type;
                      return (
                        <Button type="text" htmlType="button"
                          key={s.type}
                          onClick={() => form.setFieldsValue({ scheduleType: s.type })}
                          data-testid={`schedule-type-${s.type.toLowerCase()}`}
                          className={clsx(
                            "flex flex-col gap-3 p-6 rounded-xl border-2 transition-all group",
                            isSelected
                              ? "bg-primary/5 border-primary shadow-lg shadow-primary/10"
                              : "bg-muted border-transparent hover:border-border hover:bg-card"
                          )}
                        >
                          <div className="flex items-center justify-between">
                            {s.type !== 'NOW' && (
                              <div className={clsx("h-10 w-10 rounded-lg flex items-center justify-center", isSelected ? "bg-primary/10 text-primary" : "bg-muted/50 text-muted-foreground")}>
                                {s.type === 'SCHEDULED' ? <CalendarIcon className="h-5 w-5" /> : <Clock className="h-5 w-5" />}
                              </div>
                            )}
                            {isSelected && <div className="ml-auto h-2 w-2 bg-primary rounded-full animate-pulse" />}
                          </div>
                          <div className="text-left">
                            <h4 className="font-bold text-foreground text-sm mb-1">{s.label}</h4>
                            <p className="text-xs text-muted-foreground">{s.description}</p>
                          </div>
                        </Button>
                      );
                    })}
                  </div>

                  {scheduleType === 'SCHEDULED' && (
                    <div className="mt-6 bg-muted/50 p-6 rounded-xl border border-border">
                      <Form.Item name="scheduledAt" rules={[rule('scheduledAt')]} className="mb-0" label={<label htmlFor="transfer-scheduled-at" className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3 block">Tanggal Transfer</label>}>
                        <DatePicker id="transfer-scheduled-at"
                          className="w-full h-16 rounded-xl font-bold"
                          placeholder="Pilih Tanggal Transfer"
                          format="DD MMM YYYY"
                          disabledDate={(current) => current && current < dayjs().startOf('day')}
                          onChange={(date) => form.setFieldsValue({ scheduledAt: date ? date.toDate().toISOString() : undefined })}
                        />
                      </Form.Item>
                    </div>
                  )}

                  {scheduleType === 'RECURRING' && (
                    <div className="mt-8 space-y-6 lg:space-y-8 animate-fade-in">
                      <div className="space-y-4">
                        <span id="recurring-day-label" className="text-xs font-bold text-muted-foreground tracking-[0.3em] uppercase ml-2">Pilih Tanggal Tagihan / Transfer</span>
                        <Form.Item name="recurringDay" rules={[rule('recurringDay')]} className="mb-0" noStyle>
                          <Input type="hidden" />
                        </Form.Item>
                            <div className="grid grid-cols-7 gap-2 bg-muted/30 p-4 rounded-xl border border-border" aria-labelledby="recurring-day-label">
                              {Array.from({ length: 31 }, (_, i) => i + 1).map((d) => (
                                <Button type="text" htmlType="button"
                                  key={d}
                                  onClick={() => form.setFieldsValue({ recurringDay: d })}
                                  className={clsx(
                                    "aspect-square rounded-xl flex items-center justify-center font-bold text-sm transition-all active:scale-90",
                                    recurringDay === d
                                      ? "bg-primary text-surface shadow-lg shadow-primary/30 scale-105"
                                      : "bg-card text-foreground/60 hover:bg-primary/10 hover:text-primary border border-transparent hover:border-primary/20"
                                  )}
                                >
                                  {d}
                                </Button>
                              ))}
                            </div>
                      </div>

                      <div className="space-y-4">
                        <div className="flex items-center justify-between ml-2">
                          <span id="recurring-month-label" className="text-xs font-bold text-muted-foreground tracking-[0.3em] uppercase">Pilih Bulan (Opsional)</span>
                          <Button type="link" htmlType="button"
                            onClick={() => form.setFieldsValue({ recurringMonth: undefined })}
                            className="text-xs font-bold text-primary-dark tracking-widest uppercase hover:underline"
                          >
                            Reset ke Setiap Bulan
                          </Button>
                        </div>
                        <Form.Item name="recurringMonth" rules={[rule('recurringMonth')]} className="mb-0" noStyle>
                          <Input type="hidden" />
                        </Form.Item>
                            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2 bg-muted/30 p-4 rounded-2xl border border-border" aria-labelledby="recurring-month-label">
                              {['JAN', 'FEB', 'MAR', 'APR', 'MEI', 'JUN', 'JUL', 'AGU', 'SEP', 'OKT', 'NOV', 'DES'].map((m, idx) => {
                                const val = idx + 1;
                                return (
                                  <Button type="text" htmlType="button"
                                    key={m}
                                    onClick={() => form.setFieldsValue({ recurringMonth: val })}
                                    className={clsx(
                                      "py-4 rounded-xl flex items-center justify-center font-bold text-xs tracking-widest transition-all active:scale-95",
                                      recurringMonth === val
                                        ? "bg-primary text-surface shadow-lg shadow-primary/30"
                                        : "bg-card text-foreground/60 hover:bg-primary/10 hover:text-primary border border-transparent hover:border-primary/20"
                                    )}
                                  >
                                    {m}
                                  </Button>
                                );
                              })}
                            </div>
                      </div>
                    </div>
                  )}
                </div>

                <div className="relative group">
                  <Form.Item name="toAccountId" rules={[rule('toAccountId')]} className="mb-0" noStyle>
                    <Input data-testid="recipient-account-input" type="text" placeholder="Masukkan ID Akun atau Nomor Rekening" className="pl-16 h-16 text-lg" aria-label="Nomor Rekening Penerima" />
                  </Form.Item>
                  <Search className="absolute left-6 top-1/2 -translate-y-1/2 h-6 w-6 text-muted-foreground group-focus-within:text-primary transition-colors z-10 pointer-events-none" />
                </div>

                <div className="bg-card rounded-xl sm:rounded-2xl p-4 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden">
                  <div className="absolute top-0 right-0 w-48 h-48 bg-primary/5 rounded-full blur-3xl -z-0" />

                  <div className="flex justify-between items-center mb-6 relative z-10 gap-2">
                    <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Nominal Transfer</span>
                    <div className="flex items-center gap-2 sm:gap-3 bg-success-light px-3 sm:px-4 py-1.5 rounded-full border border-primary/10 shadow-sm shrink-0">
                      <div className="h-1.5 w-1.5 bg-primary rounded-full animate-pulse" />
                      <span className="text-xs font-bold text-primary tracking-widest uppercase">Secured IDR</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 sm:gap-6 mb-6 relative z-10">
                      <Form.Item name="amount" rules={[rule('amount')]} className="mb-0" noStyle>
                        <Input type="hidden" data-testid="amount-input" />
                      </Form.Item>
                      <Input value={formattedAmount} onChange={handleAmountChange} placeholder="0" aria-label="Nominal Transfer" className="w-full bg-transparent border-0 p-0 focus:ring-0 placeholder:text-muted-foreground/10 text-3xl sm:text-4xl lg:text-5xl xl:text-7xl font-bold outline-none text-foreground truncate" variant="borderless" />
                  </div>



                  <div className="bg-muted/50 p-4 sm:p-6 lg:p-8 rounded-xl border border-border relative z-10">
                    <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-3">Memo Transaksi</p>
                    <Form.Item name="description" rules={[rule('description')]} className="mb-0" noStyle>
                      <Input data-testid="description-input" type="text" placeholder="Apa tujuan transfer ini?" className="w-full text-base font-bold bg-transparent border-0 p-0 focus:ring-0 placeholder:text-muted-foreground/40 outline-none" aria-label="Memo Transaksi" />
                    </Form.Item>
                  </div>
                </div>

                  <Button
                    type="primary"
                    htmlType="button"
                    onClick={handleReview}
                    data-testid="review-transfer-button"
                    className="w-full h-16 rounded-2xl shadow-xl shadow-primary/20 group"
                  >
                    Tinjau Ringkasan Transfer
                    <ArrowRight className="h-5 w-5 ml-2 group-hover:translate-x-2 transition-transform" />
                  </Button>
              </Form>
              </div>

              <div className="lg:col-span-4 space-y-6 sm:space-y-8">
                <div className="bg-card rounded-xl sm:rounded-2xl p-4 sm:p-6 lg:p-8 border border-border shadow-card h-full flex flex-col">
                  <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xs font-bold text-foreground tracking-widest uppercase">Penerima Favorit</h3>
                    <div className="h-1 w-8 bg-primary rounded-full" />
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-2 gap-6">
                    {recentContacts.map((c) => (
                      <Button key={c.accountId} type="text" htmlType="button"
                        onClick={() => handleContactSelect(c)}
                        data-testid={`favorite-contact-${c.name.toLowerCase()}`}
                        className={clsx(
                          "flex flex-col items-center gap-4 p-6 rounded-xl border transition-all group",
                          selectedContact === c.accountId
                            ? "bg-primary/5 border-primary shadow-lg shadow-primary/10"
                            : "bg-muted border-transparent hover:border-border hover:bg-card"
                        )}
                      >
                        <div className={`w-14 h-14 rounded-2xl ${c.color} flex items-center justify-center font-bold text-2xl shadow-sm group-hover:scale-110 transition-transform`}>
                          {c.initial}
                        </div>
                        <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{c.name}</span>
                      </Button>
                    ))}
                    <Button type="text" htmlType="button" className="flex flex-col items-center gap-4 p-6 rounded-xl border border-dashed border-border hover:border-primary hover:bg-primary/5 transition-all group">
                      <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground group-hover:text-primary transition-colors">
                        <PlusCircle className="h-6 w-6" />
                      </div>
                      <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Tambah</span>
                    </Button>
                  </div>

                  <div className="mt-auto pt-10">
                    <div className="bg-gradient-to-br from-text-primary to-text-primary rounded-xl p-5 sm:p-6 lg:p-8 text-surface relative overflow-hidden shadow-2xl group">
                      <div className="relative z-10">
                        <h4 className="font-bold text-xl mb-2">Bantuan?</h4>
                        <p className="text-xs text-text-disabled font-bold tracking-widest uppercase mb-8 leading-relaxed">Proteksi & panduan transaksi aman.</p>
                        <Button type="default" htmlType="button" className="text-xs font-bold tracking-widest uppercase bg-surface/10 px-6 py-3 rounded-xl border border-surface/10 hover:bg-surface/20 transition-all">Hubungi Kami</Button>
                      </div>
                      <LifeBuoy className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-surface/5 rotate-12" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
        </div>
    </DashboardLayout>
  );
}
