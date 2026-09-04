'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, TrendingDown, Calendar, ArrowUpRight, Activity, Wifi, WifiOff } from 'lucide-react';
import clsx from 'clsx';
import { useAnalyticsWebSocket } from '@/hooks';
import { useAuthStore } from '@/stores';
import { PageTransition } from '@/components/ui/Motion';
import { 
  Bar, 
  BarChart, 
  CartesianGrid, 
  Cell,
  Pie, 
  PieChart, 
  XAxis, 
  YAxis,
} from "recharts"
import {
  ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"

export default function AnalyticsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  const { analytics, isConnected } = useAnalyticsWebSocket(accountId || undefined);

  // BUG-FE-062: Replace hardcoded fallback data with zeros/empty state
  const analyticsData = analytics || {
    totalIncome: 0,
    totalExpenses: 0,
    monthlySavings: 0,
    investmentRoi: 0,
    incomeChange: 0,
    expenseChange: 0,
    savingsChange: 0,
    roiChange: 0,
    spendingBreakdown: [] as { label: string; amount: number; percentage: number; color: string }[]
  };

  // ponytail: Money string HALF_EVEN 4 preferred, number legacy — chart coerces via Number()
  const trajectoryData: { day: string; masuk: number | string; keluar: number | string }[] = (analytics?.trajectoryData ?? []) as { day: string; masuk: number | string; keluar: number | string }[]

  const breakdownData = analyticsData.spendingBreakdown.map(cat => ({
    name: cat.label,
    value: cat.amount,
    fill: cat.color.includes('green') ? 'hsl(var(--primary))' : 
          cat.color.includes('emerald') ? 'hsl(var(--primary)/0.6)' : 
          'hsl(var(--muted-foreground)/0.4)'
  }))

  const trajectoryConfig = {
    masuk: {
      label: "Masuk",
      color: "hsl(var(--primary))",
    },
    keluar: {
      label: "Keluar",
      color: "hsl(var(--muted-foreground)/0.3)",
    },
  } satisfies ChartConfig

  const breakdownConfig = {
    amount: {
      label: "Jumlah",
    },
  } satisfies ChartConfig

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-6 lg:space-y-8">
          <div className="flex justify-between items-end">
            <div>
              <h2 className="text-3xl font-bold text-foreground">Intelijen Keuangan</h2>
              <p className="text-sm text-gray-500 font-medium">Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan kekayaan Anda.</p>
            </div>
            <div className="flex items-center gap-4">
              <div className={clsx("flex items-center gap-2 px-4 py-2 rounded-xl border transition-all", isConnected ? "bg-success-light text-primary border-primary/10" : "bg-muted text-muted-foreground border-border")}>
                {isConnected ? <Wifi className="h-4 w-4 animate-pulse" /> : <WifiOff className="h-4 w-4" />}
                <span className="text-xs font-bold tracking-widest uppercase">
                  {isConnected ? 'Live Update' : 'Offline'}
                </span>
              </div>
              <button className="bg-gray-50 dark:bg-gray-900 border border-border px-6 py-3 rounded-xl font-bold text-xs tracking-widest flex items-center gap-2 hover:bg-gray-100 transition-all shadow-sm">
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
              <div key={i} className="bg-card p-5 sm:p-6 lg:p-8 rounded-xl border border-border shadow-sm group hover:shadow-xl hover:shadow-bank-green/5 transition-all duration-500">
                <div className="flex justify-between items-start mb-6">
                  <div className="h-12 w-12 bg-gray-50 dark:bg-gray-900 rounded-xl flex items-center justify-center border border-border group-hover:border-bank-green/20 transition-all">
                    <stat.icon className={clsx("h-6 w-6", stat.isPos ? "text-bank-green" : "text-red-500")} />
                  </div>
                  <span className={clsx(
                    "text-xs font-bold px-3 py-1 rounded-full leading-none tracking-widest",
                    stat.isPos ? "bg-bank-green/10 text-bank-green" : "bg-red-50 text-red-500"
                  )}>
                    {stat.change > 0 ? '+' : ''}{stat.change}%
                  </span>
                </div>
                <p className="text-xs font-bold text-gray-400 tracking-[0.2em] mb-2">{stat.label}</p>
                <h3 className="text-2xl font-bold text-foreground">Rp {Number(stat.amount).toLocaleString('id-ID')}</h3>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6">
            <div className="md:col-span-6 lg:col-span-8">
              <Card className="rounded-xl border border-border shadow-sm h-full relative overflow-hidden group">
                <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />
                <CardHeader className="flex flex-row items-center justify-between pb-6 relative z-10 p-6 sm:p-6 lg:p-8">
                  <div>
                    <CardTitle className="text-xl font-bold text-foreground">Trajektori Pengeluaran</CardTitle>
                    <CardDescription className="text-xs text-gray-400 font-bold tracking-widest mt-1 lowercase">Analisis arus kas harian periode ini</CardDescription>
                  </div>
                  <div className="flex gap-4">
                    <div className="flex items-center gap-2 px-4 py-2 bg-bank-green/10 rounded-xl border border-bank-green/10">
                      <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                      <span className="text-xs font-bold text-bank-green tracking-widest uppercase">Masuk</span>
                    </div>
                    <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 dark:bg-gray-900 rounded-xl border border-border">
                      <div className="h-2 w-2 bg-gray-400 rounded-full" />
                      <span className="text-xs font-bold text-gray-400 tracking-widest uppercase">Keluar</span>
                    </div>
                  </div>
                </CardHeader>

                <CardContent className="h-[400px] relative z-10 px-6 pb-10">
                  <ChartContainer config={trajectoryConfig} className="h-full w-full">
                    <BarChart data={trajectoryData}>
                      <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="hsl(var(--muted)/0.3)" />
                      <XAxis
                        dataKey="day"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 10, fontWeight: 900 }}
                        dy={10}
                      />
                      <YAxis hide />
                      <ChartTooltip
                        cursor={false}
                        content={<ChartTooltipContent indicator="dashed" />}
                      />
                      <Bar
                        dataKey="masuk"
                        fill="var(--color-masuk)"
                        radius={[4, 4, 0, 0]}
                        barSize={32}
                      />
                      <Bar
                        dataKey="keluar"
                        fill="var(--color-keluar)"
                        radius={[4, 4, 0, 0]}
                        barSize={32}
                      />
                    </BarChart>
                  </ChartContainer>
                </CardContent>
              </Card>
            </div>

            <div className="md:col-span-6 lg:col-span-4">
              <Card className="rounded-xl border border-border shadow-sm h-full flex flex-col group p-8">
                <CardHeader className="p-0 mb-10">
                  <CardTitle className="text-xl font-bold text-foreground">Rincian Pengeluaran</CardTitle>
                </CardHeader>

                <CardContent className="p-0 flex flex-col h-full">
                  <div className="relative aspect-square mb-12 flex items-center justify-center">
                    <ChartContainer config={breakdownConfig} className="w-full h-full">
                      <PieChart>
                        <ChartTooltip
                          cursor={false}
                          content={<ChartTooltipContent hideLabel />}
                        />
                        <Pie
                          data={breakdownData}
                          dataKey="value"
                          nameKey="name"
                          innerRadius={90}
                          outerRadius={120}
                          strokeWidth={10}
                          stroke="transparent"
                        >
                          {breakdownData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.fill} />
                          ))}
                        </Pie>
                      </PieChart>
                    </ChartContainer>
                    <div className="absolute inset-0 flex flex-col items-center justify-center z-10 pointer-events-none">
                      <p className="text-xs font-bold text-gray-400 tracking-widest uppercase mb-1">Total Keluar</p>
                      <p className="text-2xl font-bold text-foreground">Rp {Number(analyticsData.totalExpenses).toLocaleString('id-ID', { notation: 'compact', compactDisplay: 'short' })}</p>
                    </div>
                  </div>

                  <div className="space-y-6 flex-1 mt-auto">
                    {analyticsData.spendingBreakdown.map((cat, i) => (
                      <div key={i} className="flex items-center justify-between group/cat cursor-pointer">
                        <div className="flex items-center gap-4">
                          <div className={clsx("h-3 w-3 rounded-full transition-transform group-hover/cat:scale-150 duration-300", cat.color)} />
                          <span className="text-xs font-bold text-foreground tracking-widest uppercase">{cat.label}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-xs font-bold text-gray-400 tracking-[0.1em]">Rp {Number(cat.amount).toLocaleString('id-ID', { notation: 'compact', compactDisplay: 'short' })}</span>
                          <span className="text-xs font-bold text-muted-foreground ml-1">({cat.percentage}%)</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>

          <div className="bg-foreground text-background rounded-xl p-5 sm:p-6 lg:p-8 relative overflow-hidden group shadow-2xl">
            <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
              <div className="space-y-4 max-w-xl text-center md:text-left">
                <h3 className="text-3xl font-bold text-white">Siap untuk menabung otomatis?</h3>
                <p className="text-sm font-medium text-gray-400 leading-relaxed tracking-wide">
                  Sistem AI kami mendeteksi Anda dapat menabung tambahan <span className="text-bank-green font-bold">Rp 2.500.000</span> setiap bulan dengan mengoptimalkan tagihan utilitas dan langganan berulang Anda.
                </p>
              </div>
              <button className="whitespace-nowrap bg-bank-green text-white px-8 py-4 rounded-xl font-bold text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20">
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
