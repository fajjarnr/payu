'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Gift, Coins, DollarSign, Share2, TrendingUp, Copy, ArrowRight, Trophy, CheckCircle, Award, Calendar, Zap, History, Clock } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useLoyaltyBalance, useCashbacks, useReferralSummary, useActivePromotions } from '@/hooks';
import { addCurrency, formatExactDecimal, type Money } from '@/lib/currency';
import { useAuthStore } from '@/stores/authStore';
import type { LoyaltyBalanceResponse, Cashback, ReferralSummaryResponse, Promotion } from '@/services/PromotionService';

export default function RewardsPage() {
  const { accountId } = useAuthStore();
  const acctId = accountId ?? '';
  const { data: loyaltyData } = useLoyaltyBalance(acctId);
  const { data: cashbackData } = useCashbacks(acctId);
  const { data: referralData } = useReferralSummary(acctId);
  const { data: promotionsData } = useActivePromotions();

  // BUG-FE-023: Use real data with skeleton/empty state instead of hardcoded fake data
  const loyaltyBalance = loyaltyData as LoyaltyBalanceResponse | undefined;
  const loyaltyStats = {
    totalEarned: loyaltyBalance?.totalEarned ?? 0,
    totalRedeemed: loyaltyBalance?.totalRedeemed ?? 0,
    currentBalance: loyaltyBalance?.currentBalance ?? 0,
    pointsExpiring: loyaltyBalance?.pointsExpiring ?? 0,
    expiryDate: loyaltyBalance?.expiryDate ?? '-'
  };

  const recentPoints: Array<{ id: number; type: string; points: number; description: string; date: string }> = [];

  const cashbackList = (cashbackData ?? []) as Cashback[];
  const cashbackHistory = cashbackList.map(cb => ({
    id: cb.id,
    merchant: cb.merchantName ?? '',
    amount: cb.amount,
    status: cb.status.toLowerCase(),
    date: cb.createdAt,
    description: cb.referenceId,
  }));

  // Compute cashback summary from actual data
  const cashbackCredited = cashbackHistory.filter(cb => cb.status === 'credited').reduce((sum, cb) => addCurrency(sum, cb.amount), '0');
  const cashbackPending = cashbackHistory.filter(cb => cb.status === 'pending').reduce((sum, cb) => addCurrency(sum, cb.amount), '0');
  const cashbackTotal = addCurrency(cashbackCredited, cashbackPending);

  const referralSummary = referralData as ReferralSummaryResponse | undefined;
  const referralStats = {
    code: referralSummary?.referralCode ?? '-',
    totalReferrals: referralSummary?.totalReferrals ?? 0,
    completedReferrals: referralSummary?.completedReferrals ?? 0,
    pendingReferrals: referralSummary?.pendingReferrals ?? 0,
    rewardPerReferral: 0,
    totalEarnings: referralSummary?.totalEarnings ?? 0
  };

  const promotionsList = (promotionsData ?? []) as Promotion[];
  const activePromotions = promotionsList.map(p => ({
    id: p.id,
    name: p.name,
    description: p.description,
    type: p.type,
    value: String(p.value),
    endDate: p.endDate,
  }));

  const formatCurrency = (amount: Money) => {
    return formatExactDecimal(amount, 0, 'id-ID');
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <Tabs defaultValue="points" className="w-full">
              <StaggerItem>
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                  <div>
                    <h2 className="text-3xl font-bold text-foreground">Rewards & Gamifikasi</h2>
                    <p className="text-sm text-muted-foreground font-medium mt-1">Kumpulkan poin, dapatkan cashback, dan raih lebih banyak keuntungan.</p>
                  </div>
                </div>

                <TabsList className="mb-8">
                  <TabsTrigger value="points" className="px-6 flex items-center gap-2">
                    <Coins className="h-4 w-4" /> Poin Loyalty
                  </TabsTrigger>
                  <TabsTrigger value="cashback" className="px-6 flex items-center gap-2">
                    <DollarSign className="h-4 w-4" /> Cashback
                  </TabsTrigger>
                  <TabsTrigger value="referral" className="px-6 flex items-center gap-2">
                    <Share2 className="h-4 w-4" /> Referral
                  </TabsTrigger>
                </TabsList>
              </StaggerItem>

              <TabsContent value="points" className="mt-0 space-y-12">
                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10">
                        <div className="flex items-start justify-between mb-8">
                          <div>
                            <div className="flex items-center gap-3 mb-3">
                              <div className="h-12 w-12 bg-white/20 rounded-xl flex items-center justify-center border border-white/10">
                                <Coins className="h-6 w-6" />
                              </div>
                              <div>
                                <p className="text-xs font-bold text-white/80 tracking-widest uppercase">Saldo Poin</p>
                                <h3 className="text-3xl font-bold">{loyaltyStats.currentBalance.toLocaleString()}</h3>
                              </div>
                            </div>
                            <p className="text-sm text-white/80">Tukarkan poin Anda untuk berbagai hadiah menarik</p>
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-6 mb-8">
                          <div>
                            <p className="text-xs font-bold text-white/60 tracking-widest uppercase mb-1">Total Diperoleh</p>
                            <p className="text-2xl font-bold">{loyaltyStats.totalEarned.toLocaleString()}</p>
                          </div>
                          <div>
                            <p className="text-xs font-bold text-white/60 tracking-widest uppercase mb-1">Total Ditukar</p>
                            <p className="text-2xl font-bold">{loyaltyStats.totalRedeemed.toLocaleString()}</p>
                          </div>
                        </div>

                        {loyaltyStats.pointsExpiring > 0 && (
                          <div className="bg-white/10 rounded-xl p-4 border border-white/10">
                            <div className="flex items-center gap-3">
                              <Calendar className="h-5 w-5 text-warning" />
                              <div>
                                <p className="text-xs font-bold text-white/80 tracking-widest uppercase">Poin Akan Kadaluarsa</p>
                                <p className="font-bold">{loyaltyStats.pointsExpiring.toLocaleString()} poin - {loyaltyStats.expiryDate}</p>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                      <Trophy className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12" />
                    </div>
                  </StaggerItem>

                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full">
                      <h3 className="text-lg font-bold text-foreground mb-6">Cara Mendapatkan Poin</h3>
                      <div className="space-y-6">
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center shrink-0 border border-primary/10">
                            <Zap className="h-5 w-5 text-primary" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Transaksi Rutin</h4>
                            <p className="text-sm text-muted-foreground">Dapatkan 1 poin untuk setiap Rp 10.000 transaksi</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center shrink-0 border border-primary/10">
                            <Calendar className="h-5 w-5 text-primary" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Login Harian</h4>
                            <p className="text-sm text-muted-foreground">Dapatkan 10-100 poin untuk login setiap hari</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center shrink-0 border border-primary/10">
                            <Share2 className="h-5 w-5 text-primary" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Referral Teman</h4>
                            <p className="text-sm text-muted-foreground">Dapatkan 1000 poin untuk setiap teman yang berhasil bergabung</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center shrink-0 border border-primary/10">
                            <Award className="h-5 w-5 text-primary" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Event Khusus</h4>
                            <p className="text-sm text-muted-foreground">Bonus poin untuk event dan promosi tertentu</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-bold text-foreground">Riwayat Poin</h3>
                  <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                    <div className="divide-y divide-border">
                      {recentPoints.map((point) => (
                        <div key={point.id} className="p-6 hover:bg-muted/30 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-4">
                              <div className={clsx(
                                'h-12 w-12 rounded-xl flex items-center justify-center',
                                point.type === 'EARNED' ? 'bg-success-light/10 text-success-light' : 'bg-warning/10 text-warning'
                              )}>
                                {point.type === 'EARNED' ? <TrendingUp className="h-6 w-6" /> : <History className="h-6 w-6" />}
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{point.description}</h4>
                                <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{point.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className={clsx(
                                'text-lg font-bold',
                                point.type === 'EARNED' ? 'text-success-light' : 'text-warning'
                              )}>
                                {point.type === 'EARNED' ? '+' : ''}{point.points}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </TabsContent>

              <TabsContent value="cashback" className="mt-0 space-y-12">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-1">
                    <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-2xl h-full">
                      <div className="relative z-10">
                        <div className="flex items-center gap-3 mb-8">
                          <div className="h-14 w-14 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                            <DollarSign className="h-7 w-7" />
                          </div>
                          <div>
                            <p className="text-xs font-bold text-gray-400 tracking-widest uppercase">Total Cashback</p>
                            <h3 className="text-4xl font-bold">{formatCurrency(cashbackTotal)}</h3>
                          </div>
                        </div>
                        <div className="space-y-4">
                          <div className="flex justify-between items-center py-3 border-b border-white/10">
                            <span className="text-sm text-gray-400">Dikreditkan</span>
                            <span className="font-bold text-success-light">{formatCurrency(cashbackCredited)}</span>
                          </div>
                          <div className="flex justify-between items-center py-3 border-b border-white/10">
                            <span className="text-sm text-gray-400">Menunggu</span>
                            <span className="font-bold text-warning">{formatCurrency(cashbackPending)}</span>
                          </div>
                          <div className="flex justify-between items-center pt-3">
                            <span className="text-sm text-gray-400">Kadaluarsa</span>
                            <span className="font-bold text-red-400">{formatCurrency('0')}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full">
                      <h3 className="text-lg font-bold text-foreground mb-6">Promosi Aktif</h3>
                      <div className="space-y-4">
                        {activePromotions.map((promo, i) => (
                          <div key={i} className="flex items-center justify-between p-4 bg-muted/30 rounded-xl border border-border hover:border-primary/20 transition-all group">
                            <div className="flex items-center gap-4">
                              <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10 group-hover:scale-110 transition-transform">
                                <Gift className="h-6 w-6 text-primary" />
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{promo.name}</h4>
                                <p className="text-sm text-muted-foreground">{promo.description}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-bold text-primary">{promo.value}</p>
                              <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">Berakhir: {promo.endDate}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-bold text-foreground">Riwayat Cashback</h3>
                  <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                    <div className="divide-y divide-border">
                      {cashbackHistory.map((cb) => (
                        <div key={cb.id} className="p-6 hover:bg-muted/30 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-4">
                              <div className={clsx(
                                'h-12 w-12 rounded-xl flex items-center justify-center',
                                cb.status === 'credited' ? 'bg-success-light/10 text-success-light' : 'bg-warning/10 text-warning'
                              )}>
                                {cb.status === 'credited' ? <CheckCircle className="h-6 w-6" /> : <Clock className="h-6 w-6" />}
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{cb.merchant}</h4>
                                <div className="flex items-center gap-2">
                                  <p className="text-sm text-muted-foreground">{cb.description}</p>
                                  <span className={clsx(
                                    'text-xs font-bold tracking-widest uppercase',
                                    cb.status === 'credited' ? 'text-success-light' : 'text-warning'
                                  )}>
                                    - {cb.status === 'credited' ? 'Dikreditkan' : 'Menunggu'}
                                  </span>
                                </div>
                                <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase">{cb.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-bold text-foreground">{formatCurrency(cb.amount)}</p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </TabsContent>

              <TabsContent value="referral" className="mt-0 space-y-12">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  <StaggerItem>
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10">
                        <div className="flex items-center justify-between mb-8">
                          <h3 className="text-xl font-bold">Kode Referral Anda</h3>
                          <div className="h-12 w-12 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                            <Gift className="h-6 w-6" />
                          </div>
                        </div>

                        <div className="bg-white/10 rounded-xl p-6 mb-8 border border-white/10">
                          <div className="flex items-center justify-between">
                            <span className="text-4xl font-bold tracking-widest">{referralStats.code}</span>
                            <ButtonMotion>
                              <Button size="icon" variant="ghost" className="h-12 w-12 bg-white/10 rounded-lg border border-white/10 hover:bg-white/30 transition-all text-white">
                                <Copy className="h-6 w-6" />
                              </Button>
                            </ButtonMotion>
                          </div>
                          <p className="text-sm text-white/80 mt-4">Bagikan kode ini kepada teman dan dapatkan {referralStats.rewardPerReferral} poin untuk setiap teman yang berhasil bergabung</p>
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                          <div>
                            <p className="text-xs font-bold text-white/60 tracking-widest uppercase mb-1">Total Teman</p>
                            <p className="text-2xl font-bold">{referralStats.totalReferrals}</p>
                          </div>
                          <div>
                            <p className="text-xs font-bold text-white/60 tracking-widest uppercase mb-1">Berhasil Bergabung</p>
                            <p className="text-2xl font-bold text-success-light">{referralStats.completedReferrals}</p>
                          </div>
                        </div>
                      </div>
                      <Share2 className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12" />
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card h-full">
                      <h3 className="text-lg font-bold text-foreground mb-6">Ringkasan Referral</h3>
                      <div className="space-y-6">
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center shrink-0 border border-primary/10">
                            <CheckCircle className="h-5 w-5 text-primary" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Berhasil Bergabung</h4>
                            <p className="text-sm text-muted-foreground">{referralStats.completedReferrals} teman telah berhasil bergabung</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-warning/10 rounded-lg flex items-center justify-center shrink-0 border border-warning/10">
                            <Clock className="h-5 w-5 text-warning" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Menunggu Konfirmasi</h4>
                            <p className="text-sm text-muted-foreground">{referralStats.pendingReferrals} teman dalam proses verifikasi</p>
                          </div>
                        </div>
                        <div className="flex items-start gap-4">
                          <div className="h-10 w-10 bg-success-light/10 rounded-lg flex items-center justify-center shrink-0 border border-success-light/10">
                            <Trophy className="h-5 w-5 text-success-light" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground">Total Penghasilan</h4>
                            <p className="text-sm text-muted-foreground">{referralStats.totalEarnings.toLocaleString()} poin dari referral</p>
                          </div>
                        </div>
                      </div>

                      <ButtonMotion className="mt-8 w-full">
                        <Button className="w-full h-14 shadow-xl shadow-primary/20 flex items-center justify-center gap-2">
                          Bagikan Link Referral <ArrowRight className="h-4 w-4" />
                        </Button>
                      </ButtonMotion>
                    </div>
                  </StaggerItem>
                </div>
              </TabsContent>
            </Tabs>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
