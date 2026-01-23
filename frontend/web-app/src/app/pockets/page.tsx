'use client';

import React, { useState } from 'react';
import { Plus, Target, Lock, TrendingUp, ChevronRight, Wallet, History, ArrowUpRight, ShieldCheck, Activity } from "lucide-react";
import { useQuery } from '@tanstack/react-query';
import { BalanceResponse, WalletTransaction } from '@/types';
import api from '@/lib/api';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { SkeletonBalance, SkeletonTransaction } from '@/components/ui/Skeleton';

export default function PocketsPage() {
  const [accountId] = useState(() => localStorage.getItem('accountId') || '');

  const { data: balance, isLoading: balanceLoading } = useQuery({
    queryKey: ['wallet-balance', accountId],
    queryFn: async () => {
      const response = await api.get<BalanceResponse>(`/wallets/${accountId}/balance`);
      return response.data;
    },
    enabled: !!accountId
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ['wallet-transactions', accountId],
    queryFn: async () => {
      const response = await api.get<WalletTransaction[]>(`/wallets/${accountId}/transactions`);
      return response.data;
    },
    enabled: !!accountId
  });

  const savingGoals = [
    {
      id: 1,
      name: 'Liburan Akhir Tahun',
      target: 10000000,
      current: 2500000,
      color: 'bg-orange-100 text-orange-600',
      icon: Target
    },
    {
      id: 2,
      name: 'Dana Darurat',
      target: 50000000,
      current: 50000000,
      color: 'bg-purple-100 text-purple-600',
      icon: Lock,
      interestRate: '4.5% p.a',
      locked: true
    }
  ];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="max-w-6xl mx-auto space-y-8 sm:space-y-10 pb-10">
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                <div>
                  <h2 className="text-2xl sm:text-3xl font-black text-foreground uppercase tracking-tight">Manajemen Kantong</h2>
                  <p className="text-sm text-gray-500 font-medium">Atur dana Anda ke dalam kantong-kantong terpisah untuk kejelasan finansial yang lebih baik.</p>
                </div>
                <ButtonMotion>
                  <button className="bg-bank-green text-white px-6 sm:px-8 py-3 sm:py-4 rounded-[1.25rem] sm:rounded-[1.5rem] font-black text-xs uppercase tracking-widest shadow-xl shadow-bank-green/20 flex items-center gap-2">
                    <Plus className="h-4 w-4" /> Buat Kantong Baru
                  </button>
                </ButtonMotion>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
              <StaggerItem className="lg:col-span-8">
                <div className="bg-card rounded-[2rem] sm:rounded-[3rem] p-6 sm:p-10 border border-border shadow-sm flex flex-col justify-between h-full relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-72 h-72 sm:w-96 sm:h-96 bg-bank-green/5 rounded-full blur-3xl -z-0" />

                  <div className="relative z-10">
                    <div className="flex justify-between items-start mb-8 sm:mb-12">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2 mb-2">
                          <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                          <p className="text-[10px] font-black text-bank-green uppercase tracking-[0.2em]">Dompet Aktif</p>
                        </div>
                        <h3 className="text-2xl sm:text-3xl font-black text-foreground uppercase italic tracking-tighter">Kantong Utama Cair.</h3>
                      </div>
                      <div className="h-10 w-10 sm:h-12 sm:w-12 bg-gray-50 dark:bg-gray-900 rounded-2xl flex items-center justify-center border border-border">
                        <Wallet className="h-5 w-5 sm:h-6 sm:w-6 text-gray-400" />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Total Dana Tersedia</p>
                      <h4 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-black text-foreground tracking-tighter italic">
                        {balanceLoading ? (
                          <SkeletonBalance />
                        ) : (
                          `Rp ${balance?.balance.toLocaleString('id-ID')}`
                        )}
                      </h4>
                    </div>
                  </div>

                  <div className="relative z-10 flex flex-col md:flex-row gap-4 sm:gap-6 mt-8 sm:mt-12 pt-8 sm:pt-10 border-t border-border">
                    <div className="flex-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Protokol Cadangan</p>
                      <p className="text-base sm:text-lg font-black text-foreground">Rp {balance?.reservedBalance?.toLocaleString('id-ID') || '0'}</p>
                    </div>
                    <div className="flex-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Batas Harian (Cap)</p>
                      <p className="text-base sm:text-lg font-black text-foreground">Rp 50.000.000</p>
                    </div>
                    <div className="flex-1 flex justify-end">
                      <button className="p-3 sm:p-4 bg-foreground text-background rounded-2xl group-hover:bg-bank-green group-hover:text-white transition-all shadow-xl">
                        <ArrowUpRight className="h-5 w-5 sm:h-6 sm:w-6" />
                      </button>
                    </div>
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem className="lg:col-span-4 space-y-6 sm:space-y-8">
                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[2rem] sm:rounded-[3rem] p-6 sm:p-10 text-white h-full flex flex-col justify-between relative overflow-hidden group shadow-2xl">
                  <div className="relative z-10">
                    <h3 className="text-lg sm:text-xl font-black uppercase tracking-tight mb-4">Keamanan Kantong</h3>
                    <div className="space-y-4 sm:space-y-6">
                      <div className="flex gap-3 sm:gap-4">
                        <div className="h-9 w-9 sm:h-10 sm:w-10 bg-white/10 rounded-xl flex items-center justify-center border border-white/10 shrink-0">
                          <ShieldCheck className="h-4 w-4 sm:h-5 sm:w-5 text-bank-green" />
                        </div>
                        <div>
                          <p className="text-xs font-black uppercase tracking-tight">Dijamin LPS</p>
                          <p className="text-[10px] text-gray-400 font-medium leading-relaxed">Dana aman dan dijamin oleh LPS hingga Rp 2 Miliar per pengguna.</p>
                        </div>
                      </div>
                      <div className="flex gap-3 sm:gap-4">
                        <div className="h-9 w-9 sm:h-10 sm:w-10 bg-white/10 rounded-xl flex items-center justify-center border border-white/10 shrink-0">
                          <Activity className="h-4 w-4 sm:h-5 sm:w-5 text-bank-green" />
                        </div>
                        <div>
                          <p className="text-xs font-black uppercase tracking-tight">Buku Besar Real-time</p>
                          <p className="text-[10px] text-gray-400 font-medium leading-relaxed">Log audit terdistribusi untuk setiap pergerakan saldo saldo Anda.</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="mt-6 sm:mt-12">
                    <p className="text-[10px] font-black text-gray-500 uppercase tracking-widest">Tipe Protokol: RESP-V3 (ENCRYPTED)</p>
                  </div>
                  <Wallet className="absolute bottom-[-20px] right-[-20px] sm:bottom-[-30px] sm:right-[-30px] h-36 w-36 sm:h-48 sm:w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
              </StaggerItem>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-10">
              <StaggerItem className="lg:col-span-7 space-y-6 sm:space-y-8">
                <div className="flex justify-between items-center">
                  <h3 className="text-lg sm:text-xl font-black text-foreground uppercase tracking-tight italic">Tujuan Khusus</h3>
                  <button className="text-[10px] font-black text-bank-green uppercase tracking-widest hover:underline">Kelola Semua</button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                  {savingGoals.map((goal) => {
                    const percentage = Math.round((goal.current / goal.target) * 100);
                    const Icon = goal.icon;
                    return (
                      <div key={goal.id} className="bg-card rounded-[1.75rem] sm:rounded-[2.5rem] p-5 sm:p-8 border border-border shadow-sm group hover:shadow-xl transition-all">
                        <div className="flex items-center gap-3 sm:gap-4 mb-4 sm:mb-6">
                          <div className={clsx("h-12 w-12 sm:h-14 sm:w-14 rounded-2xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110", goal.color)}>
                            <Icon className="h-5 w-5 sm:h-7 sm:w-7" />
                          </div>
                          <div>
                            <h4 className="font-black text-foreground text-sm sm:text-base uppercase tracking-tight">{goal.name}</h4>
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Target: Rp {goal.target.toLocaleString('id-ID')}</p>
                          </div>
                        </div>
                        {goal.locked ? (
                          <div className="space-y-3 sm:space-y-4">
                            <div className="flex justify-between items-end">
                              <p className="text-xl sm:text-2xl font-black text-foreground tracking-tighter italic">Rp {goal.current.toLocaleString('id-ID')}</p>
                              <div className="bg-yellow-100 text-yellow-700 px-2 sm:px-3 py-1 rounded-full text-[10px] font-black uppercase">{goal.interestRate}</div>
                            </div>
                            <div className="flex items-center gap-2 text-[10px] font-black text-bank-green uppercase tracking-widest">
                              <Lock className="h-3 w-3" /> Terpenuhi & Terkunci
                            </div>
                          </div>
                        ) : (
                          <div className="space-y-3 sm:space-y-4">
                            <div className="flex justify-between items-end mb-1">
                              <p className="text-xl sm:text-2xl font-black text-foreground tracking-tighter italic">Rp {goal.current.toLocaleString('id-ID')}</p>
                              <p className="text-[10px] font-black text-bank-green">{percentage}%</p>
                            </div>
                            <div className="h-2 w-full bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                              <div className="h-full bg-bank-green rounded-full" style={{ width: `${percentage}%` }} />
                            </div>
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest text-right">Sisa: Rp {(goal.target - goal.current).toLocaleString('id-ID')}</p>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </StaggerItem>

              <StaggerItem className="lg:col-span-5 space-y-6 sm:space-y-8">
                <div className="flex justify-between items-center">
                  <h3 className="text-lg sm:text-xl font-black text-foreground uppercase tracking-tight italic">Buku Besar Terakhir</h3>
                  <div className="h-8 w-8 bg-gray-50 dark:bg-gray-900 rounded-xl flex items-center justify-center border border-border">
                    <History className="h-4 w-4 text-gray-400" />
                  </div>
                </div>

                <div className="bg-card rounded-[1.75rem] sm:rounded-[2.5rem] p-5 sm:p-8 border border-border shadow-sm min-h-[300px] sm:min-h-[400px]">
                  {transactionsLoading ? (
                    <div className="space-y-4 sm:space-y-6">
                      {[1, 2, 3, 4].map(i => <SkeletonTransaction key={i} />)}
                    </div>
                  ) : (
                    <div className="space-y-6 sm:space-y-8">
                      {transactions?.map((tx) => (
                        <div key={tx.id} className="flex items-center justify-between group">
                          <div className="flex gap-4 sm:gap-5">
                            <div className={clsx(
                              "h-10 w-10 sm:h-12 sm:w-12 rounded-2xl flex items-center justify-center border transition-all group-hover:scale-110",
                              tx.type === 'CREDIT' ? "bg-bank-green/10 border-bank-green/20" : "bg-red-50 border-red-100"
                            )}>
                              {tx.type === 'CREDIT' ? (
                                <TrendingUp className="h-4 w-4 sm:h-5 sm:w-5 text-bank-green" />
                              ) : (
                                <ChevronRight className="h-4 w-4 sm:h-5 sm:w-5 text-red-500 rotate-90" />
                              )}
                            </div>
                            <div>
                              <p className="text-xs sm:text-sm font-black text-foreground uppercase tracking-tight">{tx.description}</p>
                              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{new Date(tx.createdAt).toLocaleDateString('id-ID')} • {tx.type}</p>
                            </div>
                          </div>
                          <p className={clsx(
                            "text-xs sm:text-sm font-black tracking-tight",
                            tx.type === 'CREDIT' ? "text-bank-green" : "text-foreground"
                          )}>
                            {tx.type === 'CREDIT' ? '+' : '-'} Rp {tx.amount.toLocaleString('id-ID')}
                          </p>
                        </div>
                      ))}
                      {(!transactions || transactions.length === 0) && (
                        <div className="h-full flex flex-col items-center justify-center text-center py-16 sm:py-20">
                          <History className="h-10 w-10 sm:h-12 sm:w-12 text-gray-100 dark:text-gray-800 mb-4" />
                          <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Tidak ada aktivitas terdeteksi.</p>
                        </div>
                      )}
                    </div>
                  )}
                  <button className="w-full mt-8 sm:mt-10 py-3 sm:py-4 bg-gray-50 dark:bg-gray-900 rounded-[1.5rem] sm:rounded-2xl font-black text-[10px] uppercase tracking-widest border border-border hover:bg-gray-100 transition-all">Lihat Rekening Koran</button>
                </div>
              </StaggerItem>
            </div>

            <StaggerItem>
              <div className="bg-foreground text-background rounded-[1.75rem] sm:rounded-[2.5rem] p-8 sm:p-12 relative overflow-hidden group shadow-2xl">
                <div className="absolute top-0 right-0 w-64 h-64 sm:w-80 sm:h-80 bg-white/5 rounded-full blur-3xl -z-0" />
                <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6 sm:gap-10">
                  <div className="space-y-3 sm:space-y-4 max-w-xl">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 bg-bank-green rounded-2xl flex items-center justify-center shadow-lg shadow-bank-green/20">
                        <TrendingUp className="h-5 w-5 text-white" />
                      </div>
                      <h3 className="text-2xl sm:text-3xl font-black italic tracking-tighter">Akselerasi Kekayaan Anda.</h3>
                    </div>
                    <p className="text-sm text-gray-400 font-medium leading-relaxed">
                      Pindahkan dana mengendap dari kantong ke reksa dana yield tinggi atau emas digital. AI kami menyarankan Anda bisa berhemat hingga <span className="text-bank-green font-black">Rp 12,5 Juta</span> lebih per tahun.
                    </p>
                  </div>
                  <ButtonMotion>
                    <button className="whitespace-nowrap px-8 sm:px-10 py-4 sm:py-5 bg-bank-green text-white rounded-[1.5rem] sm:rounded-3xl font-black uppercase text-xs tracking-widest shadow-2xl shadow-bank-green/20">
                      Jelajahi Marketplace
                    </button>
                  </ButtonMotion>
                </div>
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
