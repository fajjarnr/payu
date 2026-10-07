'use client';

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import dynamic from 'next/dynamic';
import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, TrendingDown, Calendar, ArrowUpRight, Activity, Wifi, WifiOff } from '@/components/icons';
import clsx from 'clsx';
import { useAnalyticsWebSocket, useCashFlow, useSpendingTrends } from '@/hooks';
import { useAuthStore } from '@/stores';
const Column = dynamic(() => import('@ant-design/plots').then(m => m.Column), { ssr: false });
const Pie = dynamic(() => import('@ant-design/plots').then(m => m.Pie), { ssr: false });
import { cn } from '@/lib/utils';

export default function AnalyticsPage() {
  const accountId = useAuthStore((state) => state.accountId);
  // FE-AUDIT-006: analytics events are keyed by account_id (backend BUG-AUTH-013),
  // so queries must use accountId — Keycloak sub returns zero rows.
  const { analytics, isConnected } = useAnalyticsWebSocket(accountId || undefined);
  const { data: cashFlow } = useCashFlow(accountId || undefined);
  const { data: trends } = useSpendingTrends(accountId || undefined);

  // REST baseline: the WS feed is enhancement-only (it may never connect —
  // no WS proxy exists in this environment), so seed the page from REST.
  // Live WS data takes precedence when present.
  const CATEGORY_COLORS = [
    'bg-primary', 'bg-primary', 'bg-warning',
    'bg-accent', 'bg-error', 'bg-text-secondary',
  ];
  const restData =
    cashFlow || trends
      ? {
          totalIncome: cashFlow?.income ?? 0,
          totalExpenses: cashFlow?.expenses ?? trends?.totalSpending ?? 0,
          monthlySavings: cashFlow?.netCashFlow ?? 0,
          investmentRoi: 0,
          incomeChange: 0,
          expenseChange: trends?.monthOverMonthChange ?? 0,
          savingsChange: 0,
          roiChange: 0,
          spendingBreakdown: (trends?.categories ?? cashFlow?.expensesByCategory ?? []).map((c, i) => ({
            label: c.category,
            amount: c.amount,
            percentage: c.percentage,
            color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
          })),
        }
      : null;

  // BUG-FE-062: Replace hardcoded fallback data with zeros/empty state
  const analyticsData = analytics ?? restData ?? {
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

  const PIE_PALETTE = ['#00D09C', '#34d399', '#f59e0b', '#8b5cf6', '#f43f5e', '#64748b'];
  const breakdownData = analyticsData.spendingBreakdown.map((cat, i) => ({
    name: cat.label,
    value: cat.amount,
    fill: PIE_PALETTE[i % PIE_PALETTE.length],
  }))

  // Canvas-rendered charts cannot resolve CSS var() colors, so use literals.
  // Primary matches the DESIGN.md primary token (#00D09C).
  const PRIMARY = '#00D09C';
  const TRACK = '#9ca3af';

  return (
    <DashboardLayout>
      <div className="space-y-6 lg:space-y-8">
          <div className="flex justify-between items-end">
            <div>
              <h2 className="text-3xl font-bold text-foreground tracking-tight">Intelijen Keuangan</h2>
              <p className="text-sm text-text-secondary font-medium">Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan kekayaan Anda.</p>
            </div>
            <div className="flex items-center gap-4">
              <div className={clsx("flex items-center gap-2 px-4 py-2 rounded-xl border transition-all", isConnected ? "bg-success-light text-primary border-primary/10" : "bg-muted text-muted-foreground border-border")}>
                {isConnected ? <Wifi className="h-4 w-4 animate-pulse" /> : <WifiOff className="h-4 w-4" />}
                <span className="text-xs font-bold tracking-widest uppercase">
                  {isConnected ? 'Live Update' : 'Offline'}
                </span>
              </div>
              <Button type="default" className="bg-surface-dim dark:bg-text-primary border border-border px-6 py-3 rounded-xl font-bold text-xs tracking-widest flex items-center gap-2 hover:bg-surface-dim transition-all shadow-sm">
                <Calendar className="h-4 w-4" /> Januari 2026
              </Button>
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
                  <div className="h-12 w-12 bg-surface-dim dark:bg-text-primary rounded-xl flex items-center justify-center border border-border group-hover:border-bank-green/20 transition-all">
                    <stat.icon className={clsx("h-6 w-6", stat.isPos ? "text-bank-green" : "text-error")} />
                  </div>
                  <span className={clsx(
                    "text-xs font-bold px-3 py-1 rounded-full leading-none tracking-widest",
                    stat.isPos ? "bg-bank-green/10 text-bank-green" : "bg-error text-white"
                  )}>
                    {stat.change > 0 ? '+' : ''}{stat.change}%
                  </span>
                </div>
                <p className="text-xs font-bold text-text-disabled tracking-[0.2em] mb-2">{stat.label}</p>
                <h3 className="text-2xl font-bold text-foreground">Rp {Number(stat.amount).toLocaleString('id-ID')}</h3>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6">
            <div className="md:col-span-6 lg:col-span-8">
              <Card className={cn("rounded-2xl border border-border bg-card text-card-foreground shadow-card", "rounded-xl border border-border shadow-sm h-full relative overflow-hidden group")} styles={{ body: { display: "contents" } }}>
                <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />
                <div className={cn("flex flex-col space-y-1.5 p-6", "flex flex-row items-center justify-between pb-6 relative z-10 p-6 sm:p-6 lg:p-8")}>
                  <div>
                    <h3 className={cn("text-2xl font-bold leading-none tracking-tight", "text-xl font-bold text-foreground")}>Trajektori Pengeluaran</h3>
                    <p className={cn("text-xs text-muted-foreground font-medium uppercase tracking-[0.1em]", "text-xs text-text-disabled font-bold tracking-widest mt-1 lowercase")}>Analisis arus kas harian periode ini</p>
                  </div>
                  <div className="flex gap-4">
                    <div className="flex items-center gap-2 px-4 py-2 bg-bank-green/10 rounded-xl border border-bank-green/10">
                      <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                      <span className="text-xs font-bold text-bank-green tracking-widest uppercase">Masuk</span>
                    </div>
                    <div className="flex items-center gap-2 px-4 py-2 bg-surface-dim dark:bg-text-primary rounded-xl border border-border">
                      <div className="h-2 w-2 bg-text-disabled rounded-full" />
                      <span className="text-xs font-bold text-text-disabled tracking-widest uppercase">Keluar</span>
                    </div>
                  </div>
                </div>

                <div className={cn("p-6 pt-0", "h-[400px] relative z-10 px-6 pb-10")}>
                  <div className="h-full w-full">
                    <Column
                      data={trajectoryData.flatMap((d) => [
                        { day: d.day, type: 'Masuk', amount: Number(d.masuk) },
                        { day: d.day, type: 'Keluar', amount: Number(d.keluar) },
                      ])}
                      xField="day"
                      yField="amount"
                      colorField="type"
                      group={true}
                      scale={{ color: { range: [PRIMARY, TRACK] } }}
                      style={{ radiusTopLeft: 4, radiusTopRight: 4, maxWidth: 32 }}
                      axis={{
                        x: { title: false, labelFill: '#6b7280', labelFontSize: 10, labelFontWeight: 700 },
                        y: false,
                      }}
                      label={false}
                    />
                  </div>
                </div>
              </Card>
            </div>

            <div className="md:col-span-6 lg:col-span-4">
              <Card className={cn("rounded-2xl border border-border bg-card text-card-foreground shadow-card", "rounded-xl border border-border shadow-sm h-full flex flex-col group p-5 sm:p-6")} styles={{ body: { display: "contents" } }}>
                <div className={cn("flex flex-col space-y-1.5 p-6", "p-0 mb-6")}>
                  <h3 className={cn("text-2xl font-bold leading-none tracking-tight", "text-xl font-bold text-foreground")}>Rincian Pengeluaran</h3>
                </div>

                <div className={cn("p-6 pt-0", "p-0 flex flex-col h-full")}>
                  <div className="relative aspect-square mb-8 flex items-center justify-center">
                    <div className="w-full h-full">
                      <Pie
                        data={breakdownData.map((d) => ({ name: d.name, value: Number(d.value) }))}
                        angleField="value"
                        colorField="name"
                        scale={{ color: { range: breakdownData.map((d) => d.fill) } }}
                        innerRadius={0.65}
                        style={{ lineWidth: 4, stroke: '#fff' }}
                        legend={false}
                        label={false}
                      />
                    </div>
                    <div className="absolute inset-0 flex flex-col items-center justify-center z-10 pointer-events-none">
                      <p className="text-xs font-bold text-text-disabled tracking-widest uppercase mb-1">Total Keluar</p>
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
                          <span className="text-xs font-bold text-text-disabled tracking-[0.1em]">Rp {Number(cat.amount).toLocaleString('id-ID', { notation: 'compact', compactDisplay: 'short' })}</span>
                          <span className="text-xs font-bold text-muted-foreground ml-1">({cat.percentage}%)</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </Card>
            </div>
          </div>

          <div className="bg-foreground text-background rounded-xl p-5 sm:p-6 lg:p-8 relative overflow-hidden group shadow-2xl">
            <div className="absolute top-0 right-0 w-80 h-80 bg-surface/5 rounded-full blur-3xl -z-0" />
            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
              <div className="space-y-4 max-w-xl text-center md:text-left">
                <h3 className="text-3xl font-bold text-surface">Siap untuk menabung otomatis?</h3>
                <p className="text-sm font-medium text-text-disabled leading-relaxed tracking-wide">
                  Sistem AI kami mendeteksi Anda dapat menabung tambahan <span className="text-bank-green font-bold">Rp 2.500.000</span> setiap bulan dengan mengoptimalkan tagihan utilitas dan langganan berulang Anda.
                </p>
              </div>
              <Button type="primary" className="whitespace-nowrap bg-bank-green text-surface px-8 py-4 rounded-xl font-bold text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20">
                Terapkan Optimasi
              </Button>
            </div>
          </div>
      </div>
    </DashboardLayout>
  );
}
