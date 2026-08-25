'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { CreditCard, Calendar, ShieldCheck, Wallet, ArrowRight, Percent, CheckCircle, Clock, Plus, FileText, TrendingUp } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useCreditScore, usePayLater, usePayLaterTransactions, useActivePreApprovals, useActivatePayLater, useApplyLoan, usePayLaterPayment } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { asMoney, formatCurrency } from '@/lib/currency';
import { toast } from 'sonner';

export default function LendingPage() {
  const { user } = useAuthStore();
  const userId = user?.id ?? '';
  const { data: creditScoreData, isLoading: isLoadingScore } = useCreditScore(userId);
  const { data: payLaterData, isLoading: isLoadingPayLater } = usePayLater(userId);
  const { data: payLaterTxns } = usePayLaterTransactions(userId);
  const { data: preApprovals, isLoading: isLoadingPreApprovals } = useActivePreApprovals(userId);
  const activatePayLater = useActivatePayLater();
  const applyLoan = useApplyLoan();
  const payLaterPayment = usePayLaterPayment();

  // ponytail: minimal wiring for L1-L7 — real mutations instead of toast-only stubs
  const handleActivatePayLater = async () => {
    try {
      await activatePayLater.mutateAsync({
        userId,
        request: { monthlyIncome: asMoney('5000000'), employmentType: 'FULL_TIME', employmentDurationMonths: 12 },
      });
      toast.success('PayLater berhasil diaktifkan');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal aktivasi PayLater');
    }
  };

  const handleApplyLoan = async (productName: string) => {
    try {
      // eslint-disable-next-line react-hooks/purity -- externalId generated per user action, not render
      const externalId = `ext-${Date.now()}`;
      await applyLoan.mutateAsync({
        externalId,
        loanType: 'PERSONAL',
        principalAmount: asMoney('10000000'),
        tenureMonths: 12,
        purpose: productName,
      });
      toast.success(`Pengajuan ${productName} telah diterima`);
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal mengajukan pinjaman');
    }
  };

  const handlePayBill = async () => {
    try {
      const amount = payLaterData?.minimumPayment ?? '100000';
      await payLaterPayment.mutateAsync({ userId, amount });
      toast.success('Pembayaran tagihan berhasil');
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : 'Gagal membayar tagihan');
    }
  };

  const loanProducts = [
    {
      name: 'Pinjaman Personal',
      description: 'Pembiayaan fleksibel untuk kebutuhan pribadi dengan bunga kompetitif',
      minAmount: '2000000',
      maxAmount: '50000000',
      interestRate: '12.5% p.a',
      tenure: '6 - 36 bulan',
      processingTime: '1-2 hari kerja',
      icon: FileText,
      color: 'text-blue-500',
      bg: 'bg-blue-500/10'
    },
    {
      name: 'Pinjaman Multiguna',
      description: 'Gunakan aset Anda sebagai jaminan untuk limit pinjaman lebih tinggi',
      minAmount: '10000000',
      maxAmount: '200000000',
      interestRate: '10% p.a',
      tenure: '12 - 60 bulan',
      processingTime: '3-5 hari kerja',
      icon: TrendingUp,
      color: 'text-primary',
      bg: 'bg-success-light'
    }
  ];

  const payLaterStats = {
    creditLimit: payLaterData?.creditLimit ?? '0',
    usedLimit: payLaterData?.usedLimit ?? '0',
    availableLimit: payLaterData?.availableLimit ?? '0',
    minimumPayment: payLaterData?.minimumPayment ?? '0',
    dueDate: payLaterData?.dueDate ?? '--',
    transactions: (payLaterTxns ?? []).map(t => ({
      id: String(t.id),
      merchant: t.merchantName ?? 'Unknown',
      amount: t.amount,
      date: t.createdAt ?? '--',
      status: t.type === 'PAYMENT' ? 'paid' : 'pending',
    }))
  };

  const creditScore = {
    score: creditScoreData?.score ?? 0,
    grade: creditScoreData?.grade ?? '--',
    maxScore: 850,
    lastUpdated: creditScoreData?.lastUpdated ?? '--',
    factors: creditScoreData?.factors ?? []
  };

  const creditUtilization = Number(payLaterStats.creditLimit) > 0
    ? (Number(payLaterStats.usedLimit) / Number(payLaterStats.creditLimit)) * 100
    : 0;

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <Tabs defaultValue="loans" data-testid="lending-tabs" className="w-full">
              <StaggerItem>
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                  <div>
                    <h2 className="text-3xl font-bold text-foreground">Pinjaman & Kredit</h2>
                    <p className="text-sm text-muted-foreground font-medium mt-1">Solusi pembiayaan fleksibel sesuai kebutuhan Anda.</p>
                  </div>
                  <TabsContent value="paylater" className="mt-0">
                    <ButtonMotion>
                      <Button 
                        onClick={handleActivatePayLater}
                        disabled={activatePayLater.isPending}
                        data-testid="activate-paylater-button" 
                        className="h-14 px-8 shadow-xl shadow-primary/20 flex items-center gap-2 disabled:opacity-50">
                        <Plus className="h-4 w-4" /> {activatePayLater.isPending ? 'Memproses...' : 'Aktifkan PayLater'}
                      </Button>
                    </ButtonMotion>
                  </TabsContent>
                </div>

                <TabsList className="mb-8">
                  <TabsTrigger value="loans" data-testid="loans-tab" className="px-8">Pinjaman</TabsTrigger>
                  <TabsTrigger value="paylater" data-testid="paylater-tab" className="px-8">PayLater</TabsTrigger>
                </TabsList>
              </StaggerItem>

              <TabsContent value="loans" className="mt-0 space-y-12">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10 flex items-start justify-between mb-8">
                        <div>
                          <p className="text-xs font-bold text-gray-400 tracking-widest uppercase mb-2">Skor Kredit Anda</p>
                          <div className="flex items-baseline gap-3">
                            <h3 className="text-5xl font-bold">
                              {isLoadingScore ? <Skeleton className="h-12 w-20 bg-white/20" /> : creditScore.score}
                            </h3>
                            <div className="flex items-center gap-2 bg-success-light/20 px-3 py-1 rounded-full border border-success-light/20">
                              <span className="text-lg font-bold text-success-light">
                                Grade {isLoadingScore ? "..." : creditScore.grade}
                              </span>
                            </div>
                          </div>
                          <p className="text-xs text-gray-400 font-bold tracking-widest uppercase mt-3">
                            Terakhir diperbarui: {isLoadingScore ? "..." : creditScore.lastUpdated}
                          </p>
                        </div>
                        <div className="h-16 w-16 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                          <ShieldCheck className="h-8 w-8 text-bank-green" />
                        </div>
                      </div>

                      <div className="space-y-3 mb-8">
                        {isLoadingScore 
                          ? [1,2].map(i => <Skeleton key={i} className="h-5 w-48 bg-white/10" />)
                          : creditScore.factors.map((factor, i) => (
                            <div key={i} className="flex items-center gap-3">
                                <div className="h-6 w-6 rounded-full bg-success-light/20 flex items-center justify-center">
                                <CheckCircle className="h-4 w-4 text-success-light" />
                                </div>
                                <span className="text-sm font-medium">{factor}</span>
                            </div>
                          ))}
                      </div>

                      <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                        <div className="bg-gradient-to-r from-success-light to-primary h-full rounded-full" style={{ width: `${(creditScore.score / creditScore.maxScore) * 100}%` }} />
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full">
                      <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center mb-6 border border-primary/10">
                        <Wallet className="h-6 w-6 text-primary" />
                      </div>
                      <h3 className="text-lg font-bold text-foreground mb-3">Total Limit Pinjaman</h3>
                       <p className="text-3xl font-bold text-primary mb-2">
                        {isLoadingPreApprovals ? <Skeleton className="h-9 w-32" /> : formatCurrency(preApprovals?.[0]?.maxAmount ?? '0')}
                       </p>
                      <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Tersedia berdasarkan skor kredit</p>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-bold text-foreground">Produk Pinjaman</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {loanProducts.map((product, i) => (
                      <StaggerItem key={i} data-testid={`loan-product-${i}`} className="bg-card p-8 rounded-xl border border-border shadow-sm hover:shadow-card hover:-translate-y-1 transition-all group cursor-pointer active:scale-[0.98]">
                        <div className="flex justify-between items-start mb-8">
                          <div className={clsx("h-16 w-16 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110", product.bg, product.color)}>
                            <product.icon className="h-8 w-8" />
                          </div>
                          <div className="h-10 w-10 bg-muted/30 rounded-lg flex items-center justify-center text-muted-foreground">
                            <Percent className="h-5 w-5" />
                          </div>
                        </div>
                        <div>
                          <h4 className="text-xl font-bold text-foreground mb-2 leading-tight">{product.name}</h4>
                          <p className="text-sm text-muted-foreground mb-6 leading-relaxed">{product.description}</p>
                          <div className="space-y-3 mb-6">
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Limit Pinjaman</span>
                              <span className="font-bold text-foreground">{formatCurrency(product.minAmount)} - {formatCurrency(product.maxAmount)}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Bunga</span>
                              <span className="font-bold text-primary">{product.interestRate}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Tenor</span>
                              <span className="font-bold text-foreground">{product.tenure}</span>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                              <span className="text-muted-foreground font-medium">Proses</span>
                              <span className="font-bold text-foreground">{product.processingTime}</span>
                            </div>
                          </div>
                          <ButtonMotion className="w-full">
                            <Button 
                              onClick={() => handleApplyLoan(product.name)}
                              disabled={applyLoan.isPending}
                              data-testid={`apply-loan-${i}`} 
                              className="w-full h-14 shadow-xl shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-50">
                              {applyLoan.isPending ? 'Memproses...' : <>Ajukan Sekarang <ArrowRight className="h-4 w-4" /></>}
                            </Button>
                          </ButtonMotion>
                        </div>
                      </StaggerItem>
                    ))}
                  </div>
                </div>
              </TabsContent>

              <TabsContent value="paylater" className="mt-0 space-y-12">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 mb-8">
                        <div>
                          <div className="flex items-center gap-3 mb-3">
                            <div className="h-12 w-12 bg-white/20 rounded-xl flex items-center justify-center border border-white/10">
                              <CreditCard className="h-6 w-6" />
                            </div>
                            <div>
                              <p className="text-xs font-bold text-white/80 tracking-widest uppercase">PayLater Limit</p>
                              <h3 className="text-3xl font-bold tracking-tight mt-1">{formatCurrency(payLaterStats.creditLimit)}</h3>
                            </div>
                          </div>
                          <div className="flex gap-4 text-xs font-bold text-white/80">
                            <span>Terpakai: {formatCurrency(payLaterStats.usedLimit)}</span>
                            <span>•</span>
                            <span>Tersedia: {formatCurrency(payLaterStats.availableLimit)}</span>
                          </div>
                        </div>

                        <div className="text-left md:text-right bg-white/10 p-4 rounded-xl backdrop-blur-sm border border-white/10">
                          <p className="text-xs font-bold text-white/60 tracking-widest uppercase mb-1">Jatuh Tempo</p>
                          <p className="font-bold">{payLaterStats.dueDate}</p>
                        </div>
                      </div>

                      <div className="relative z-10 space-y-2">
                        <div className="flex justify-between text-xs font-bold tracking-widest uppercase text-white/80">
                          <span>Penggunaan Limit</span>
                          <span>{creditUtilization.toFixed(0)}%</span>
                        </div>
                        <div className="w-full bg-black/20 h-3 rounded-full overflow-hidden p-0.5 border border-white/10">
                          <div className="bg-white h-full rounded-full transition-all duration-500" style={{ width: `${creditUtilization}%` }} />
                        </div>
                      </div>

                      <div className="relative z-10 mt-8 pt-6 border-t border-white/10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                        <div>
                          <p className="text-xs font-bold text-white/60 tracking-widest uppercase">Pembayaran Minimum</p>
                          <p className="text-xl font-bold">{formatCurrency(payLaterStats.minimumPayment)}</p>
                        </div>
                        <ButtonMotion>
                          <Button 
                            onClick={handlePayBill}
                            disabled={payLaterPayment.isPending}
                            data-testid="pay-bill-button" 
                            variant="secondary" 
                            className="px-8 h-12 rounded-xl bg-white text-primary hover:bg-white/90 shadow-lg disabled:opacity-50">
                            {payLaterPayment.isPending ? 'Memproses...' : 'Bayar Tagihan'}
                          </Button>
                        </ButtonMotion>
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full">
                      <div className="flex justify-between items-start mb-6">
                        <h3 className="text-lg font-bold text-foreground">Ringkasan Transaksi</h3>
                        <div className="h-8 w-8 bg-primary/10 rounded-lg flex items-center justify-center border border-primary/10">
                          <TrendingUp className="h-4 w-4 text-primary" />
                        </div>
                      </div>
                      <div className="space-y-4">
                        <div className="flex justify-between items-center py-3 border-b border-border">
                          <span className="text-sm text-muted-foreground font-medium">Total Transaksi</span>
                          <span className="text-lg font-bold text-foreground">{payLaterStats.transactions.length}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-border">
                          <span className="text-sm text-muted-foreground font-medium">Pembayaran Berhasil</span>
                          <span className="text-lg font-bold text-success-light">{payLaterStats.transactions.filter(t => t.status === 'paid').length}</span>
                        </div>
                        <div className="flex justify-between items-center py-3">
                          <span className="text-sm text-muted-foreground font-medium">Menunggu Pembayaran</span>
                          <span className="text-lg font-bold text-warning">{payLaterStats.transactions.filter(t => t.status !== 'paid').length}</span>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-bold text-foreground">Riwayat Transaksi PayLater</h3>
                  <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                    <div className="divide-y divide-border">
                      {payLaterStats.transactions.map((txn) => (
                        <div key={txn.id} data-testid={`transaction-${txn.id}`} className="p-6 hover:bg-muted/30 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-4">
                              <div className={clsx(
                                'h-12 w-12 rounded-xl flex items-center justify-center',
                                txn.status === 'paid' ? 'bg-success-light/10 text-success-light' : 'bg-warning/10 text-warning'
                              )}>
                                {txn.status === 'paid' ? <CheckCircle className="h-6 w-6" /> : <Clock className="h-6 w-6" />}
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{txn.merchant}</h4>
                                <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{txn.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-bold text-foreground">{formatCurrency(txn.amount)}</p>
                              <p className={clsx(
                                'text-xs font-bold tracking-widest uppercase flex items-center justify-end gap-1',
                                txn.status === 'paid' ? 'text-success-light' : 'text-warning'
                              )}>
                                {txn.status === 'paid' ? 'Dibayar' : 'Menunggu Pembayaran'}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </TabsContent>
            </Tabs>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
