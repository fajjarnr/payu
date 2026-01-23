'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Gift, Coins, DollarSign, Share2, TrendingUp, Copy, ArrowRight, Star, Trophy, CheckCircle, Award, Calendar, Zap, History, Clock } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function RewardsPage() {
  const [activeTab, setActiveTab] = useState<'points' | 'cashback' | 'referral'>('points');

  const loyaltyStats = {
    totalEarned: 12500,
    totalRedeemed: 3200,
    currentBalance: 9300,
    pointsExpiring: 800,
    expiryDate: '31 Jan 2026'
  };

  const recentPoints = [
    { id: 1, type: 'EARNED', points: 500, description: 'Transaksi pembelian di Shopee', date: '20 Jan 2026' },
    { id: 2, type: 'REDEEMED', points: -200, description: 'Tukar diskon belanja', date: '18 Jan 2026' },
    { id: 3, type: 'EARNED', points: 300, description: 'Login harian', date: '17 Jan 2026' },
    { id: 4, type: 'EARNED', points: 1000, description: 'Referral teman berhasil', date: '15 Jan 2026' },
    { id: 5, type: 'EARNED', points: 200, description: 'Transaksi QRIS', date: '12 Jan 2026' }
  ];

  const cashbackHistory = [
    { id: 1, merchant: 'TokoBapak', amount: 25000, status: 'credited', date: '20 Jan 2026', description: '10% cashback' },
    { id: 2, merchant: 'Traveloka', amount: 150000, status: 'pending', date: '18 Jan 2026', description: '15% cashback' },
    { id: 3, merchant: 'Shopee', amount: 10000, status: 'credited', date: '15 Jan 2026', description: '5% cashback' },
    { id: 4, merchant: 'Indomaret', amount: 5000, status: 'credited', date: '10 Jan 2026', description: '2% cashback' }
  ];

  const referralStats = {
    code: 'PAYU2024',
    totalReferrals: 8,
    completedReferrals: 5,
    pendingReferrals: 3,
    rewardPerReferral: 1000,
    totalEarnings: 5000
  };

  const activePromotions = [
    { id: 1, name: 'Weekend Warrior', description: 'Dapatkan 2x poin untuk semua transaksi QRIS', type: 'LOYALTY_POINTS', value: '2X', endDate: '26 Jan 2026', icon: Zap },
    { id: 2, name: 'Cashback Hari Raya', description: 'Cashback 15% untuk transaksi di mitra pilihan', type: 'CASHBACK', value: '15%', endDate: '31 Jan 2026', icon: DollarSign },
    { id: 3, name: 'Bulanan Penuh', description: 'Bebas biaya transfer untuk 10 transaksi pertama', type: 'VOUCHER', value: 'GRATIS', endDate: '30 Jan 2026', icon: Star }
  ];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(amount);
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-black text-foreground">Rewards & Gamifikasi</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Kumpulkan poin, dapatkan cashback, dan raih lebih banyak keuntungan.</p>
                </div>
              </div>

              <div className="flex gap-2 mb-8">
                <button
                  onClick={() => setActiveTab('points')}
                  className={clsx(
                    'px-6 py-3 rounded-xl font-bold text-xs tracking-widest uppercase transition-all flex items-center gap-2',
                    activeTab === 'points' ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20' : 'bg-muted/50 text-muted-foreground border border-border'
                  )}
                >
                  <Coins className="h-4 w-4" /> Poin Loyalty
                </button>
                <button
                  onClick={() => setActiveTab('cashback')}
                  className={clsx(
                    'px-6 py-3 rounded-xl font-bold text-xs tracking-widest uppercase transition-all flex items-center gap-2',
                    activeTab === 'cashback' ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20' : 'bg-muted/50 text-muted-foreground border border-border'
                  )}
                >
                  <DollarSign className="h-4 w-4" /> Cashback
                </button>
                <button
                  onClick={() => setActiveTab('referral')}
                  className={clsx(
                    'px-6 py-3 rounded-xl font-bold text-xs tracking-widest uppercase transition-all flex items-center gap-2',
                    activeTab === 'referral' ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20' : 'bg-muted/50 text-muted-foreground border border-border'
                  )}
                >
                  <Share2 className="h-4 w-4" /> Referral
                </button>
              </div>
            </StaggerItem>

            {activeTab === 'points' ? (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-10 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10">
                        <div className="flex items-start justify-between mb-8">
                          <div>
                            <div className="flex items-center gap-3 mb-3">
                              <div className="h-12 w-12 bg-white/20 rounded-xl flex items-center justify-center border border-white/10">
                                <Coins className="h-6 w-6" />
                              </div>
                              <div>
                                <p className="text-[10px] font-bold text-white/80 tracking-widest uppercase">Saldo Poin</p>
                                <h3 className="text-3xl font-black">{loyaltyStats.currentBalance.toLocaleString()}</h3>
                              </div>
                            </div>
                            <p className="text-sm text-white/80">Tukarkan poin Anda untuk berbagai hadiah menarik</p>
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-6 mb-8">
                          <div>
                            <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase mb-1">Total Diperoleh</p>
                            <p className="text-2xl font-black">{loyaltyStats.totalEarned.toLocaleString()}</p>
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase mb-1">Total Ditukar</p>
                            <p className="text-2xl font-black">{loyaltyStats.totalRedeemed.toLocaleString()}</p>
                          </div>
                        </div>

                        {loyaltyStats.pointsExpiring > 0 && (
                          <div className="bg-white/10 rounded-xl p-4 border border-white/10">
                            <div className="flex items-center gap-3">
                              <Calendar className="h-5 w-5 text-warning" />
                              <div>
                                <p className="text-[10px] font-bold text-white/80 tracking-widest uppercase">Poin Akan Kadaluarsa</p>
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
                    <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full">
                      <h3 className="text-lg font-black text-foreground mb-6">Cara Mendapatkan Poin</h3>
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
                  <h3 className="text-xl font-black text-foreground">Riwayat Poin</h3>
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
                                <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase">{point.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className={clsx(
                                'text-lg font-black',
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
              </>
            ) : activeTab === 'cashback' ? (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <StaggerItem className="lg:col-span-1">
                    <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-10 text-white relative overflow-hidden shadow-2xl h-full">
                      <div className="relative z-10">
                        <div className="flex items-center gap-3 mb-8">
                          <div className="h-14 w-14 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                            <DollarSign className="h-7 w-7" />
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-gray-400 tracking-widest uppercase">Total Cashback</p>
                            <h3 className="text-4xl font-black">{formatCurrency(190000)}</h3>
                          </div>
                        </div>
                        <div className="space-y-4">
                          <div className="flex justify-between items-center py-3 border-b border-white/10">
                            <span className="text-sm text-gray-400">Dikreditkan</span>
                            <span className="font-bold text-success-light">{formatCurrency(40000)}</span>
                          </div>
                          <div className="flex justify-between items-center py-3 border-b border-white/10">
                            <span className="text-sm text-gray-400">Menunggu</span>
                            <span className="font-bold text-warning">{formatCurrency(150000)}</span>
                          </div>
                          <div className="flex justify-between items-center pt-3">
                            <span className="text-sm text-gray-400">Kadaluarsa</span>
                            <span className="font-bold text-red-400">{formatCurrency(0)}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </StaggerItem>

                  <StaggerItem className="lg:col-span-2">
                    <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full">
                      <h3 className="text-lg font-black text-foreground mb-6">Promosi Aktif</h3>
                      <div className="space-y-4">
                        {activePromotions.map((promo, i) => (
                          <div key={i} className="flex items-center justify-between p-4 bg-muted/30 rounded-xl border border-border hover:border-primary/20 transition-all group">
                            <div className="flex items-center gap-4">
                              <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10 group-hover:scale-110 transition-transform">
                                <promo.icon className="h-6 w-6 text-primary" />
                              </div>
                              <div>
                                <h4 className="font-bold text-foreground">{promo.name}</h4>
                                <p className="text-sm text-muted-foreground">{promo.description}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-black text-primary">{promo.value}</p>
                              <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase">Berakhir: {promo.endDate}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </StaggerItem>
                </div>

                <div className="space-y-8 mt-12">
                  <h3 className="text-xl font-black text-foreground">Riwayat Cashback</h3>
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
                                    'text-[10px] font-bold tracking-widest uppercase',
                                    cb.status === 'credited' ? 'text-success-light' : 'text-warning'
                                  )}>
                                    - {cb.status === 'credited' ? 'Dikreditkan' : 'Menunggu'}
                                  </span>
                                </div>
                                <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase">{cb.date}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-lg font-black text-foreground">{formatCurrency(cb.amount)}</p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  <StaggerItem>
                    <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-8 sm:p-10 text-white relative overflow-hidden shadow-2xl">
                      <div className="relative z-10">
                        <div className="flex items-center justify-between mb-8">
                          <h3 className="text-xl font-black">Kode Referral Anda</h3>
                          <div className="h-12 w-12 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                            <Gift className="h-6 w-6" />
                          </div>
                        </div>

                        <div className="bg-white/10 rounded-xl p-6 mb-8 border border-white/10">
                          <div className="flex items-center justify-between">
                            <span className="text-4xl font-black tracking-widest">{referralStats.code}</span>
                            <ButtonMotion>
                              <button className="h-12 w-12 bg-white/20 rounded-lg flex items-center justify-center border border-white/10 hover:bg-white/30 transition-all">
                                <Copy className="h-6 w-6" />
                              </button>
                            </ButtonMotion>
                          </div>
                          <p className="text-sm text-white/80 mt-4">Bagikan kode ini kepada teman dan dapatkan {referralStats.rewardPerReferral} poin untuk setiap teman yang berhasil bergabung</p>
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                          <div>
                            <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase mb-1">Total Teman</p>
                            <p className="text-2xl font-black">{referralStats.totalReferrals}</p>
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-white/60 tracking-widest uppercase mb-1">Berhasil Bergabung</p>
                            <p className="text-2xl font-black text-success-light">{referralStats.completedReferrals}</p>
                          </div>
                        </div>
                      </div>
                      <Share2 className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12" />
                    </div>
                  </StaggerItem>

                  <StaggerItem>
                    <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card h-full">
                      <h3 className="text-lg font-black text-foreground mb-6">Ringkasan Referral</h3>
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
                        <button className="w-full py-4 bg-primary text-primary-foreground rounded-xl font-bold text-xs tracking-widest uppercase shadow-xl shadow-primary/20 hover:bg-bank-emerald transition-all flex items-center justify-center gap-2">
                          Bagikan Link Referral <ArrowRight className="h-4 w-4" />
                        </button>
                      </ButtonMotion>
                    </div>
                  </StaggerItem>
                </div>
              </>
            )}
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
