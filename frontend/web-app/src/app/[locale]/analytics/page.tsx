'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { BarChart3, TrendingUp, TrendingDown, Calendar, ArrowUpRight, ArrowDownRight, PieChart, Activity, Wifi, WifiOff } from 'lucide-react';
import clsx from 'clsx';
import { useAnalyticsWebSocket } from '@/hooks';
import { useAuthStore } from '@/stores';
import { PageTransition } from '@/components/ui/Motion';

export default function AnalyticsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  const { analytics, isConnected } = useAnalyticsWebSocket(accountId);

  const analyticsData = analytics || {
    totalIncome: 42500000,
    totalExpenses: 12800000,
    monthlySavings: 29700000,
    investmentRoi: 5200000,
    incomeChange: 12.5,
    expenseChange: -4.2,
    savingsChange: 18.1,
    roiChange: 2.4,
    spendingBreakdown: [
      { label: 'Belanja', amount: 4200000, percentage: 60, color: 'bg-bank-green' },
      { label: 'Utilitas', amount: 2800000, percentage: 30, color: 'bg-bank-emerald' },
      { label: 'Lain-lain', amount: 1500000, percentage: 10, color: 'bg-gray-400' }
    ]
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <div className="flex justify-between items-end">
            <div>
              <h2 className="text-3xl font-black text-foreground">Intelijen Keuangan</h2>
              <p className="text-sm text-gray-500 font-medium">Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan kekayaan Anda.</p>
            </div>
            <div className="flex items-center gap-4">
              <div className={clsx("flex items-center gap-2 px-4 py-2 rounded-xl border transition-all", isConnected ? "bg-success-light text-primary border-primary/10" : "bg-muted text-muted-foreground border-border")}>
                {isConnected ? <Wifi className="h-4 w-4 animate-pulse" /> : <WifiOff className="h-4 w-4" />}
                <span className="text-[10px] font-bold tracking-widest uppercase">
                  {isConnected ? 'Live Update' : 'Offline'}
                </span>
              </div>
              <button className="bg-gray-50 dark:bg-gray-900 border border-border px-6 py-3 rounded-xl font-black text-[10px] tracking-widest flex items-center gap-2 hover:bg-gray-100 transition-all shadow-sm">
                <Calendar className="h-4 w-4" /> Januari 2026
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              { label: 'Total Pemasukan', amount: analyticsData.totalIncome, change: analyticsData.incomeChange, isPos: true, icon: TrendingUp },
              { label: 'Total Pengeluaran', amount: analyticsData.totalExpenses, change: analyticsData.expenseChange, isPos: false, icon: TrendingDown },
              { label: 'Tabungan Bulanan', amount: analyticsData.monthlySavings, change: analyticsData.savingsChange, isPos: true, icon: Activity },
              { label: 'ROI Investasi', amount: analyticsData.investmentRoi, change: analyticsData.roiChange, isPos: true, icon: ArrowUpRight },
            ].map((stat, i) => (
              <div key={i} className="bg-card p-8 rounded-xl border border-border shadow-sm group hover:shadow-xl hover:shadow-bank-green/5 transition-all duration-500">
                <div className="flex justify-between items-start mb-6">
                  <div className="h-12 w-12 bg-gray-50 dark:bg-gray-900 rounded-xl flex items-center justify-center border border-border group-hover:border-bank-green/20 transition-all">
                    <stat.icon className={clsx("h-6 w-6", stat.isPos ? "text-bank-green" : "text-red-500")} />
                  </div>
                  <span className={clsx(
                    "text-[10px] font-black px-3 py-1 rounded-full leading-none tracking-widest",
                    stat.isPos ? "bg-bank-green/10 text-bank-green" : "bg-red-50 text-red-500"
                  )}>
                    {stat.change > 0 ? '+' : ''}{stat.change}%
                  </span>
                </div>
                <p className="text-[10px] font-black text-gray-400 tracking-[0.2em] mb-2">{stat.label}</p>
                <h3 className="text-2xl font-black text-foreground">Rp {stat.amount.toLocaleString('id-ID')}</h3>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-8">
              <div className="bg-card rounded-xl p-10 border border-border shadow-sm h-full relative overflow-hidden group">
                <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />
                <div className="flex justify-between items-center mb-10 relative z-10">
                  <div>
                    <h3 className="text-xl font-black text-foreground">Trajektori Pengeluaran</h3>
                    <p className="text-[10px] text-gray-400 font-black tracking-widest mt-1">Analisis arus kas harian periode ini</p>
                  </div>
                  <div className="flex gap-4">
                    <div className="flex items-center gap-2 px-4 py-2 bg-bank-green/10 rounded-xl border border-bank-green/10">
                      <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                      <span className="text-[10px] font-black text-bank-green tracking-widest">Masuk</span>
                    </div>
                    <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 dark:bg-gray-900 rounded-xl border border-border">
                      <div className="h-2 w-2 bg-gray-400 rounded-full" />
                      <span className="text-[10px] font-black text-gray-400 tracking-widest">Keluar</span>
                    </div>
                  </div>
                </div>

                <div className="h-[300px] flex items-end justify-between gap-3 px-4 relative z-10">
                  {[65, 45, 80, 55, 90, 75, 40, 85, 30, 95, 20, 100].map((h, i) => (
                    <div key={i} className="flex-1 flex flex-col gap-3 items-center group/bar">
                      <div className="w-full flex flex-col gap-1.5 items-center h-full justify-end">
                        <div
                          className="w-full bg-bank-green/20 rounded-t-xl group-hover/bar:bg-bank-green/40 transition-all duration-500 cursor-pointer relative"
                          style={{ height: `${h}%` }}
                        >
                          <div className="absolute top-0 left-0 w-full h-1 bg-bank-green/60 rounded-full" />
                        </div>
                        <div
                          className="w-full bg-gray-100 dark:bg-gray-800 rounded-t-lg group-hover/bar:bg-gray-200 transition-all duration-500 cursor-pointer"
                          style={{ height: `${h * 0.4}%` }}
                        />
                      </div>
                      <span className="text-[8px] font-black text-gray-300">Hari {i + 1}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="lg:col-span-4">
              <div className="bg-card rounded-xl p-10 border border-border shadow-sm h-full flex flex-col group">
                <h3 className="text-xl font-black text-foreground mb-10">Rincian Pengeluaran</h3>

                <div className="relative aspect-square mb-12 flex items-center justify-center">
                  <div className="absolute inset-0 flex flex-col items-center justify-center z-10 transition-transform group-hover:scale-110 duration-700">
                    <p className="text-[10px] font-black text-gray-400 tracking-widest">Total Keluar</p>
                    <p className="text-2xl font-black text-foreground">Rp {analyticsData.totalExpenses.toLocaleString('id-ID', { notation: 'compact', compactDisplay: 'short' })}</p>
                  </div>
                  <div className="w-full h-full rounded-full border-[20px] border-gray-50 dark:border-gray-900 relative">
                    {analyticsData.spendingBreakdown.map((cat, i) => {
                      const rotation = i === 0 ? -45 : i === 1 ? 12 : 90;
                      return (
                        <div
                          key={i}
                          className={`absolute -inset-[20px] rounded-full border-[20px] ${cat.color} ${i === 0 ? 'border-r-transparent border-b-transparent' : i === 1 ? 'border-l-transparent border-t-transparent' : 'border-l-transparent border-b-transparent'} group-hover:rotate-0 transition-transform duration-1000`}
                          style={{ transform: `rotate(${rotation}deg)` }}
                        />
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-6 flex-1">
                  {analyticsData.spendingBreakdown.map((cat, i) => (
                    <div key={i} className="flex items-center justify-between group/cat cursor-pointer">
                      <div className="flex items-center gap-4">
                        <div className={clsx("h-3 w-3 rounded-full transition-transform group-hover/cat:scale-150 duration-300", cat.color)} />
                        <span className="text-xs font-black text-foreground tracking-widest">{cat.label}</span>
                      </div>
                      <div className="text-right">
                        <span className="text-[10px] font-black text-gray-400 tracking-[0.1em]">Rp {cat.amount.toLocaleString('id-ID', { notation: 'compact', compactDisplay: 'short' })}</span>
                        <span className="text-[8px] font-black text-muted-foreground ml-1">({cat.percentage}%)</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div className="bg-foreground text-background rounded-xl p-12 relative overflow-hidden group shadow-2xl">
            <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
              <div className="space-y-4 max-w-xl text-center md:text-left">
                <h3 className="text-3xl font-black text-white">Siap untuk menabung otomatis?</h3>
                <p className="text-sm font-medium text-gray-400 leading-relaxed tracking-wide">
                  Sistem AI kami mendeteksi Anda dapat menabung tambahan <span className="text-bank-green font-black">Rp 2.500.000</span> setiap bulan dengan mengoptimalkan tagihan utilitas dan langganan berulang Anda.
                </p>
              </div>
              <button className="whitespace-nowrap bg-bank-green text-white px-10 py-6 rounded-xl font-black text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20">
                Terapkan Optimasi
              </button>
            </div>
            <Activity className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
          </div>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
